package com.example.springbootbackeng.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.example.springbootbackeng.entity.AiRecognitionLog;
import com.example.springbootbackeng.entity.AiRecognitionLogVO;
import com.example.springbootbackeng.mapper.AiRecognitionLogMapper;
import com.example.springbootbackeng.service.AiRecognitionLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiRecognitionLogServiceImpl implements AiRecognitionLogService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final String LOCAL_IMAGE_SCHEME = "local://";
    private static final String LEGACY_UPLOAD_SCHEME = "upload://";
    private static final Path UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"), "uploads", "ai-recognition");
    private static final String DEFAULT_BAIDU_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v2/advanced_general";
    private static final String BAIDU_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final long TOKEN_REFRESH_BUFFER_MS = 60_000L;
    private static final Path SETTINGS_FILE = Paths.get(System.getProperty("user.dir"), "config", "ai-recognition-settings.json");
    private static final int DEFAULT_BAIKE_NUM = 3;
    private static final BigDecimal DEFAULT_MIN_CONFIDENCE = BigDecimal.valueOf(55);

    private final AiRecognitionLogMapper aiRecognitionLogMapper;
    private final Environment environment;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    private final Object tokenLock = new Object();
    private final Object settingsLock = new Object();
    private volatile String cachedAccessToken;
    private volatile long accessTokenExpireAtMs;
    private volatile RecognitionSettings cachedSettings;

    public AiRecognitionLogServiceImpl(AiRecognitionLogMapper aiRecognitionLogMapper,
                                       Environment environment) {
        this.aiRecognitionLogMapper = aiRecognitionLogMapper;
        this.environment = environment;
    }

    @Override
    public List<AiRecognitionLogVO> listVisibleLogs(String keyword, String currentUserId, boolean isManager) {
        if (!isManager && isBlank(currentUserId)) {
            return Collections.emptyList();
        }
        String filterUserId = isManager ? null : currentUserId;
        return aiRecognitionLogMapper.selectVisibleLogs(filterUserId, keyword);
    }

    @Override
    public Map<String, Object> runRecognition(String userId, String originalImageUrl) {
        String uid = trim(userId);
        String imageRef = trim(originalImageUrl);
        if (uid == null) {
            throw new IllegalArgumentException("未获取到登录用户");
        }
        if (imageRef == null) {
            throw new IllegalArgumentException("图片地址不能为空");
        }

        Map<String, Object> recognition;
        String source;
        try {
            recognition = invokeBaiduRecognition(imageRef);
            source = "baidu";
        } catch (Exception ex) {
            recognition = fallbackRecognition(imageRef);
            recognition.put("fallbackReason", ex.getMessage());
            source = "fallback";
        }

        String materialName = trim((String) recognition.get("recognizeMaterialName"));
        if (materialName == null) {
            materialName = "未知物料";
        }
        Integer quantity = toPositiveInt(recognition.get("recognizeQuantity"));
        BigDecimal confidence = toPercent(recognition.get("confidenceScore"));

        Map<String, Object> aiResultJson = new LinkedHashMap<>();
        aiResultJson.put("source", source);
        aiResultJson.put("originalImageRef", imageRef);
        aiResultJson.put("recognizeMaterialName", materialName);
        aiResultJson.put("recognizeQuantity", quantity);
        aiResultJson.put("confidenceScore", confidence);
        aiResultJson.put("raw", recognition.get("raw"));
        aiResultJson.put("summary", recognition.get("summary"));
        aiResultJson.put("matchedKeyword", recognition.get("matchedKeyword"));
        aiResultJson.put("matchedRoot", recognition.get("matchedRoot"));
        aiResultJson.put("baikeInfo", recognition.get("baikeInfo"));

        AiRecognitionLog row = new AiRecognitionLog();
        row.setRecId(UUID.randomUUID().toString().replace("-", ""));
        row.setUserId(uid);
        row.setOriginalImageUrl(imageRef);
        row.setAiResultJson(toJson(aiResultJson));
        row.setRecognizeMaterialName(materialName);
        row.setRecognizeQuantity(quantity);
        row.setConfidenceScore(confidence);
        row.setIsVerified(0);
        row.setVerifyUserId(null);
        row.setCreateTime(LocalDateTime.now());

        int inserted = aiRecognitionLogMapper.insert(row);
        if (inserted <= 0) {
            throw new IllegalStateException("AI识别记录写入失败");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recId", row.getRecId());
        result.put("recognizeMaterialName", row.getRecognizeMaterialName());
        result.put("recognizeQuantity", row.getRecognizeQuantity());
        result.put("confidenceScore", row.getConfidenceScore());
        result.put("source", source);
        return result;
    }

    @Override
    public String verifyRecognition(String recId, String verifyUserId, String recognizeMaterialName, Integer recognizeQuantity) {
        String id = trim(recId);
        String verifier = trim(verifyUserId);
        if (id == null) return "记录ID不能为空";
        if (verifier == null) return "核实人不能为空";

        AiRecognitionLog old = aiRecognitionLogMapper.selectById(id);
        if (old == null) return "识别记录不存在";

        AiRecognitionLog patch = new AiRecognitionLog();
        patch.setRecId(id);
        patch.setIsVerified(1);
        patch.setVerifyUserId(verifier);
        if (trim(recognizeMaterialName) != null) {
            patch.setRecognizeMaterialName(trim(recognizeMaterialName));
        }
        if (recognizeQuantity != null && recognizeQuantity >= 0) {
            patch.setRecognizeQuantity(recognizeQuantity);
        }

        int updated = aiRecognitionLogMapper.updateById(patch);
        return updated > 0 ? null : "核实更新失败";
    }

    @Override
    public Map<String, Object> getAiConfigCheck() {
        String apiKey = resolveConfig(
                "BAIDU_AI_METERIAL_SYSTEM_API_KEY",
                "BAIDU_AI_MATERIAL_SYSTEM_API_KEY",
                "baidu.material.api-key");
        String secretKey = resolveConfig(
                "BAIDU_AI_METERIAL_SYSTEM_SECRET_KEY",
                "BAIDU_AI_MATERIAL_SYSTEM_SECRET_KEY",
                "baidu.material.secret-key");
        String materialUrl = resolveConfig("BAIDU_METERIAL_URL", "BAIDU_MATERIAL_URL", "baidu.material.url");

        RecognitionSettings settings = loadRecognitionSettings();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("baiduApiKeyConfigured", apiKey != null);
        data.put("baiduSecretConfigured", secretKey != null);
        data.put("baiduMaterialUrlConfigured", materialUrl != null);
        data.put("baiduApiKeyMasked", maskSecret(apiKey));
        data.put("baiduSecretMasked", maskSecret(secretKey));
        data.put("baiduMaterialUrlMasked", maskBaseUrl(materialUrl == null ? DEFAULT_BAIDU_URL : materialUrl));
        data.put("baikeNum", settings.baikeNum);
        data.put("minConfidenceThreshold", settings.minConfidenceThreshold);
        return data;
    }

    @Override
    public Map<String, Object> getRecognitionSettings() {
        RecognitionSettings settings = loadRecognitionSettings();
        return settings.toMap();
    }

    @Override
    public String updateRecognitionSettings(String operatorUserId,
                                            Integer baikeNum,
                                            BigDecimal minConfidenceThreshold,
                                            Map<String, List<String>> keywordDict) {
        if (isBlank(operatorUserId)) {
            return "未获取到登录用户";
        }
        if (baikeNum == null) {
            return "baike_num不能为空";
        }
        if (baikeNum < 0 || baikeNum > 5) {
            return "baike_num范围必须在0-5";
        }
        if (minConfidenceThreshold == null) {
            return "最低置信度阈值不能为空";
        }
        if (minConfidenceThreshold.compareTo(BigDecimal.ZERO) < 0 || minConfidenceThreshold.compareTo(BigDecimal.valueOf(100)) > 0) {
            return "最低置信度阈值范围必须在0-100";
        }

        RecognitionSettings current = loadRecognitionSettings();
        RecognitionSettings next = new RecognitionSettings();
        next.baikeNum = baikeNum;
        next.minConfidenceThreshold = minConfidenceThreshold.setScale(2, RoundingMode.HALF_UP);
        next.keywordDict = normalizeKeywordDict(keywordDict, current.keywordDict);

        try {
            saveRecognitionSettings(next);
            return null;
        } catch (Exception e) {
            return "保存AI识别参数失败: " + e.getMessage();
        }
    }

    private Map<String, Object> invokeBaiduRecognition(String imageRef) throws Exception {
        RecognitionSettings settings = loadRecognitionSettings();

        byte[] imageBytes = loadImageBytes(imageRef);
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalStateException("图片数据为空");
        }

        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        if (imageBase64.length() > 8 * 1024 * 1024) {
            throw new IllegalStateException("图片base64编码后超过8MB，请压缩后重试");
        }

        int baikeNum = settings.baikeNum;

        String endpoint = resolveConfig("BAIDU_METERIAL_URL", "BAIDU_MATERIAL_URL", "baidu.material.url");
        if (isBlank(endpoint)) {
            endpoint = DEFAULT_BAIDU_URL;
        }

        String form = "image=" + URLEncoder.encode(imageBase64, StandardCharsets.UTF_8);
        if (baikeNum > 0) {
            form += "&baike_num=" + baikeNum;
        }

        String accessToken = getBaiduAccessToken(false);
        String body = doBaiduClassifyRequest(endpoint, accessToken, form);
        JsonNode root = objectMapper.readTree(body);

        if (root.has("error_code")) {
            int code = root.path("error_code").asInt(-1);
            if (isBaiduTokenExpired(code)) {
                accessToken = getBaiduAccessToken(true);
                body = doBaiduClassifyRequest(endpoint, accessToken, form);
                root = objectMapper.readTree(body);
            }
            if (root.has("error_code")) {
                throw new IllegalStateException("百度识别失败: " + root.path("error_msg").asText("未知错误"));
            }
        }

        return parseBaiduResult(root, imageRef, settings);
    }

    private String doBaiduClassifyRequest(String endpoint, String accessToken, String formBody) throws Exception {
        String sep = endpoint.contains("?") ? "&" : "?";
        String url = endpoint + sep + "access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("百度识别HTTP失败: " + response.statusCode());
        }
        return response.body();
    }

    private Map<String, Object> parseBaiduResult(JsonNode root, String imageRef, RecognitionSettings settings) {
        JsonNode results = root.path("result");
        if (!results.isArray() || results.isEmpty()) {
            return Map.of(
                    "recognizeMaterialName", "无法识别",
                    "recognizeQuantity", 0,
                    "confidenceScore", 0,
                    "summary", "百度返回结果为空",
                    "raw", objectToMap(root),
                    "detectedItems", Collections.emptyList(),
                    "materialCandidates", Collections.emptyList()
            );
        }

        List<DetectedItem> allItems = new ArrayList<>();
        for (JsonNode item : results) {
            String keyword = trim(item.path("keyword").asText(""));
            String rootTag = trim(item.path("root").asText(""));
            double score = clamp01(item.path("score").asDouble(0));
            List<String> materialMatches = findMaterialMatches(keyword, settings.keywordDict);
            allItems.add(new DetectedItem(keyword, rootTag, score, item.path("baike_info"), materialMatches));
        }
        allItems.sort((a, b) -> Double.compare(b.score, a.score));

        List<DetectedItem> thresholdItems = new ArrayList<>();
        double threshold = settings.minConfidenceThreshold == null ? 0 : settings.minConfidenceThreshold.doubleValue();
        for (DetectedItem it : allItems) {
            if (it.score * 100 >= threshold) {
                thresholdItems.add(it);
            }
        }

        DetectedItem display = thresholdItems.isEmpty() ? allItems.get(0) : thresholdItems.get(0);
        BigDecimal confidencePercent = BigDecimal.valueOf(display.score * 100).setScale(2, RoundingMode.HALF_UP);

        List<String> topKeywords = new ArrayList<>();
        for (int i = 0; i < allItems.size() && i < 5; i++) {
            DetectedItem it = allItems.get(i);
            String name = isBlank(it.keyword) ? "未知对象" : it.keyword;
            topKeywords.add(name + "(" + String.format("%.2f", it.score) + ")");
        }

        List<String> materialCandidates = new ArrayList<>();
        for (DetectedItem it : allItems) {
            for (String m : it.materialMatches) {
                if (!materialCandidates.contains(m)) {
                    materialCandidates.add(m);
                }
            }
        }

        List<Map<String, Object>> detectedItemMaps = new ArrayList<>();
        for (DetectedItem it : allItems) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("keyword", isBlank(it.keyword) ? "未知对象" : it.keyword);
            m.put("root", it.rootTag);
            m.put("score", BigDecimal.valueOf(it.score).setScale(6, RoundingMode.HALF_UP));
            m.put("scorePercent", BigDecimal.valueOf(it.score * 100).setScale(2, RoundingMode.HALF_UP));
            m.put("materialMatches", it.materialMatches);
            m.put("baikeInfo", nodeToMap(it.baikeInfo));
            detectedItemMaps.add(m);
        }

        String displayName = isBlank(display.keyword) ? "未知对象" : display.keyword;
        String summary = "共识别到 " + allItems.size() + " 类对象；TOP关键词: " + String.join("、", topKeywords)
                + "；当前展示对象: " + displayName
                + "；置信度阈值: " + settings.minConfidenceThreshold + "%"
                + (materialCandidates.isEmpty() ? "；未命中预设物料词典" : "；命中预设物料: " + String.join("、", materialCandidates));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("recognizeMaterialName", displayName);
        map.put("recognizeQuantity", allItems.size());
        map.put("confidenceScore", confidencePercent);
        map.put("matchedKeyword", display.keyword);
        map.put("matchedRoot", display.rootTag);
        map.put("baikeInfo", nodeToMap(display.baikeInfo));
        map.put("materialCandidates", materialCandidates);
        map.put("detectedItems", detectedItemMaps);
        map.put("summary", summary);
        map.put("raw", objectToMap(root));
        return map;
    }

    private byte[] loadImageBytes(String imageRef) throws Exception {
        if (isBlank(imageRef)) {
            throw new IllegalArgumentException("图片地址不能为空");
        }

        if (imageRef.startsWith(LOCAL_IMAGE_SCHEME) || imageRef.startsWith(LEGACY_UPLOAD_SCHEME)) {
            String fileName = imageRef.startsWith(LOCAL_IMAGE_SCHEME)
                    ? imageRef.substring(LOCAL_IMAGE_SCHEME.length())
                    : imageRef.substring(LEGACY_UPLOAD_SCHEME.length());
            String safeName = fileName.replaceAll("[\\r\\n\\t\\\\/]", "");
            Path p = UPLOAD_ROOT.resolve(safeName).normalize();
            if (!p.startsWith(UPLOAD_ROOT) || !Files.exists(p) || !Files.isRegularFile(p)) {
                throw new IllegalStateException("上传图片不存在: " + safeName);
            }
            return Files.readAllBytes(p);
        }

        if (imageRef.startsWith("http://") || imageRef.startsWith("https://")) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(imageRef)).GET().build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IllegalStateException("下载图片失败: HTTP " + resp.statusCode());
            }
            return resp.body();
        }

        if (imageRef.startsWith("data:image")) {
            int comma = imageRef.indexOf(',');
            if (comma < 0) {
                throw new IllegalStateException("dataURL格式不正确");
            }
            String b64 = imageRef.substring(comma + 1);
            return Base64.getDecoder().decode(b64);
        }

        Path p = Paths.get(imageRef);
        if (Files.exists(p) && Files.isRegularFile(p)) {
            return Files.readAllBytes(p);
        }
        throw new IllegalStateException("不支持的图片地址: " + imageRef);
    }

    private String getBaiduAccessToken(boolean forceRefresh) throws Exception {
        long now = System.currentTimeMillis();
        if (!forceRefresh && !isBlank(cachedAccessToken) && now < (accessTokenExpireAtMs - TOKEN_REFRESH_BUFFER_MS)) {
            return cachedAccessToken;
        }

        synchronized (tokenLock) {
            now = System.currentTimeMillis();
            if (!forceRefresh && !isBlank(cachedAccessToken) && now < (accessTokenExpireAtMs - TOKEN_REFRESH_BUFFER_MS)) {
                return cachedAccessToken;
            }

            String apiKey = resolveConfig(
                    "BAIDU_AI_METERIAL_SYSTEM_API_KEY",
                    "BAIDU_AI_MATERIAL_SYSTEM_API_KEY",
                    "baidu.material.api-key");
            String secretKey = resolveConfig(
                    "BAIDU_AI_METERIAL_SYSTEM_SECRET_KEY",
                    "BAIDU_AI_MATERIAL_SYSTEM_SECRET_KEY",
                    "baidu.material.secret-key");
            if (isBlank(apiKey) || isBlank(secretKey)) {
                throw new IllegalStateException("请配置 BAIDU_AI_METERIAL_SYSTEM_API_KEY 与 BAIDU_AI_METERIAL_SYSTEM_SECRET_KEY");
            }

            String url = BAIDU_TOKEN_URL
                    + "?grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IllegalStateException("获取百度access_token失败: HTTP " + resp.statusCode());
            }

            JsonNode tokenJson = objectMapper.readTree(resp.body());
            if (tokenJson.has("error")) {
                throw new IllegalStateException("获取百度access_token失败: " + tokenJson.path("error_description").asText("未知错误"));
            }
            String token = tokenJson.path("access_token").asText("");
            long expiresIn = tokenJson.path("expires_in").asLong(0);
            if (isBlank(token) || expiresIn <= 0) {
                throw new IllegalStateException("获取百度access_token失败: 返回内容无效");
            }

            cachedAccessToken = token;
            accessTokenExpireAtMs = System.currentTimeMillis() + expiresIn * 1000L;
            return cachedAccessToken;
        }
    }

    private boolean isBaiduTokenExpired(int code) {
        return code == 110 || code == 111 || code == 1101;
    }

    private int resolveIntConfig(int def, int min, int max, String... keys) {
        String raw = resolveConfig(keys);
        if (isBlank(raw)) {
            return def;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < min) return min;
            if (v > max) return max;
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    private int extractQuantityFromText(String text) {
        if (isBlank(text)) return 0;
        int qty = 0;
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                int n = Integer.parseInt(matcher.group(1));
                if (n > 0 && n < 100000) {
                    qty = n;
                }
            } catch (Exception ignored) {
            }
        }
        return qty;
    }

    private List<String> findMaterialMatches(String keyword, Map<String, List<String>> keywordDict) {
        if (keywordDict == null || keywordDict.isEmpty() || isBlank(keyword)) {
            return Collections.emptyList();
        }
        String k = keyword.toLowerCase();
        List<String> hits = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : keywordDict.entrySet()) {
            List<String> dict = entry.getValue();
            if (dict == null || dict.isEmpty()) continue;
            for (String kw : dict) {
                if (!isBlank(kw) && k.contains(kw.toLowerCase())) {
                    hits.add(entry.getKey());
                    break;
                }
            }
        }
        return hits;
    }

    private RecognitionSettings loadRecognitionSettings() {
        RecognitionSettings local = cachedSettings;
        if (local != null) {
            return local.copy();
        }
        synchronized (settingsLock) {
            if (cachedSettings != null) {
                return cachedSettings.copy();
            }

            RecognitionSettings settings = defaultRecognitionSettings();
            try {
                if (Files.exists(SETTINGS_FILE) && Files.isRegularFile(SETTINGS_FILE)) {
                    Map<String, Object> json = objectMapper.readValue(SETTINGS_FILE.toFile(), new TypeReference<Map<String, Object>>() {});
                    settings.baikeNum = resolveInt(json.get("baikeNum"), settings.baikeNum, 0, 5);
                    settings.minConfidenceThreshold = resolveBigDecimal(json.get("minConfidenceThreshold"), settings.minConfidenceThreshold, BigDecimal.ZERO, BigDecimal.valueOf(100));
                    settings.keywordDict = normalizeKeywordDict(castKeywordDict(json.get("keywordDict")), settings.keywordDict);
                }
            } catch (Exception ignored) {
            }

            cachedSettings = settings;
            return settings.copy();
        }
    }

    private void saveRecognitionSettings(RecognitionSettings settings) throws Exception {
        synchronized (settingsLock) {
            Files.createDirectories(SETTINGS_FILE.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(SETTINGS_FILE.toFile(), settings.toMap());
            cachedSettings = settings.copy();
        }
    }

    private RecognitionSettings defaultRecognitionSettings() {
        RecognitionSettings settings = new RecognitionSettings();
        settings.baikeNum = DEFAULT_BAIKE_NUM;
        settings.minConfidenceThreshold = DEFAULT_MIN_CONFIDENCE.setScale(2, RoundingMode.HALF_UP);
        settings.keywordDict = new LinkedHashMap<>();
        settings.keywordDict.put("螺纹钢", new ArrayList<>(List.of("钢筋", "螺纹钢", "钢材", "钢管", "钢板", "工字钢", "角钢", "steel", "rebar")));
        settings.keywordDict.put("普通硅酸盐水泥", new ArrayList<>(List.of("水泥", "砂浆", "混凝土", "cement")));
        settings.keywordDict.put("页岩砖", new ArrayList<>(List.of("砖", "红砖", "青砖", "空心砖", "砌块", "砖块", "brick", "加气块")));

        int envBaikeNum = resolveIntConfig(settings.baikeNum, 0, 5,
                "BAIDU_METERIAL_BAIKE_NUM",
                "BAIDU_MATERIAL_BAIKE_NUM",
                "baidu.material.baike-num");
        settings.baikeNum = envBaikeNum;

        String envThreshold = resolveConfig(
                "BAIDU_METERIAL_MIN_CONFIDENCE_THRESHOLD",
                "BAIDU_MATERIAL_MIN_CONFIDENCE_THRESHOLD",
                "baidu.material.min-confidence-threshold");
        if (!isBlank(envThreshold)) {
            try {
                BigDecimal t = new BigDecimal(envThreshold).setScale(2, RoundingMode.HALF_UP);
                if (t.compareTo(BigDecimal.ZERO) >= 0 && t.compareTo(BigDecimal.valueOf(100)) <= 0) {
                    settings.minConfidenceThreshold = t;
                }
            } catch (Exception ignored) {
            }
        }

        return settings;
    }

    private int resolveInt(Object source, int def, int min, int max) {
        if (source == null) return def;
        try {
            int v = Integer.parseInt(String.valueOf(source));
            if (v < min) return min;
            if (v > max) return max;
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    private BigDecimal resolveBigDecimal(Object source, BigDecimal def, BigDecimal min, BigDecimal max) {
        if (source == null) return def;
        try {
            BigDecimal v = new BigDecimal(String.valueOf(source)).setScale(2, RoundingMode.HALF_UP);
            if (v.compareTo(min) < 0) return min;
            if (v.compareTo(max) > 0) return max;
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> castKeywordDict(Object obj) {
        if (!(obj instanceof Map<?, ?> rawMap)) {
            return null;
        }
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String k = String.valueOf(entry.getKey());
            Object v = entry.getValue();
            List<String> list = new ArrayList<>();
            if (v instanceof List<?> arr) {
                for (Object item : arr) {
                    if (item != null) list.add(String.valueOf(item));
                }
            }
            out.put(k, list);
        }
        return out;
    }

    private Map<String, List<String>> normalizeKeywordDict(Map<String, List<String>> input, Map<String, List<String>> fallback) {
        Map<String, List<String>> base = new LinkedHashMap<>();
        if (fallback != null) {
            for (Map.Entry<String, List<String>> e : fallback.entrySet()) {
                base.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
        if (input == null || input.isEmpty()) {
            return base;
        }

        for (String material : List.of("螺纹钢", "普通硅酸盐水泥", "页岩砖")) {
            List<String> source = input.get(material);
            if (source == null) continue;
            List<String> cleaned = new ArrayList<>();
            for (String kw : source) {
                String v = trim(kw);
                if (v != null && !cleaned.contains(v)) {
                    cleaned.add(v);
                }
            }
            if (!cleaned.isEmpty()) {
                base.put(material, cleaned);
            }
        }
        return base;
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null) return false;
        for (String t : terms) {
            if (t != null && !t.isEmpty() && text.contains(t.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private Map<String, Object> nodeToMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> objectToMap(Object obj) {
        return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> fallbackRecognition(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        String materialName = "其他物料";
        if (lower.contains("steel") || lower.contains("rebar") || lower.contains("螺纹钢") || lower.contains("钢筋")) {
            materialName = "螺纹钢";
        } else if (lower.contains("cement") || lower.contains("水泥")) {
            materialName = "普通硅酸盐水泥";
        } else if (lower.contains("brick") || lower.contains("砖")) {
            materialName = "页岩砖";
        }

        int qty = 1;
        Matcher matcher = NUMBER_PATTERN.matcher(lower);
        while (matcher.find()) {
            try {
                int n = Integer.parseInt(matcher.group(1));
                if (n > 0 && n < 100000) {
                    qty = n;
                }
            } catch (Exception ignored) {
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recognizeMaterialName", materialName);
        result.put("recognizeQuantity", qty);
        result.put("confidenceScore", 62.5);
        result.put("summary", "AI不可用，已按图片URL关键词进行规则识别");
        result.put("raw", Collections.singletonMap("imageUrl", imageUrl));
        return result;
    }

    private Integer toPositiveInt(Object value) {
        if (value == null) return 0;
        try {
            String cleaned = String.valueOf(value).replaceAll("[^0-9-]", "");
            int n = Integer.parseInt(cleaned);
            return Math.max(0, n);
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal toPercent(Object value) {
        try {
            BigDecimal bd = new BigDecimal(String.valueOf(value));
            if (bd.compareTo(BigDecimal.ZERO) < 0) bd = BigDecimal.ZERO;
            if (bd.compareTo(BigDecimal.valueOf(100)) > 0) bd = BigDecimal.valueOf(100);
            return bd.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String stripCodeFence(String text) {
        String v = text == null ? "" : text.trim();
        if (!v.startsWith("```")) return v;
        int firstBreak = v.indexOf('\n');
        if (firstBreak < 0) return v;
        String body = v.substring(firstBreak + 1);
        int lastFence = body.lastIndexOf("```");
        if (lastFence >= 0) {
            body = body.substring(0, lastFence);
        }
        return body.trim();
    }

    private String resolveConfig(String... keys) {
        if (keys == null) return null;
        for (String key : keys) {
            if (isBlank(key)) continue;
            String byEnv = unquote(System.getenv(key));
            if (!isBlank(byEnv)) return byEnv;

            String byProperty = unquote(System.getProperty(key));
            if (!isBlank(byProperty)) return byProperty;

            String bySpringProperty = unquote(environment.getProperty(key));
            if (!isBlank(bySpringProperty)) return bySpringProperty;
        }
        return null;
    }

    private String unquote(String s) {
        String v = trim(s);
        if (v == null) return null;
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return trim(v.substring(1, v.length() - 1));
        }
        return v;
    }

    private String trim(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private boolean isBlank(String s) {
        return trim(s) == null;
    }

    private String maskBaseUrl(String baseUrl) {
        String v = trim(baseUrl);
        if (v == null) return "未读取到";
        if (v.length() <= 18) return v;
        return v.substring(0, 12) + "..." + v.substring(v.length() - 6);
    }

    private String maskSecret(String token) {
        String v = trim(token);
        if (v == null) return "未读取到";
        if (v.length() <= 10) return "***";
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

    private static class DetectedItem {
        private final String keyword;
        private final String rootTag;
        private final double score;
        private final JsonNode baikeInfo;
        private final List<String> materialMatches;

        private DetectedItem(String keyword, String rootTag, double score, JsonNode baikeInfo, List<String> materialMatches) {
            this.keyword = keyword;
            this.rootTag = rootTag;
            this.score = score;
            this.baikeInfo = baikeInfo;
            this.materialMatches = materialMatches == null ? Collections.emptyList() : materialMatches;
        }
    }

    private static class RecognitionSettings {
        private int baikeNum;
        private BigDecimal minConfidenceThreshold;
        private Map<String, List<String>> keywordDict;

        private RecognitionSettings copy() {
            RecognitionSettings c = new RecognitionSettings();
            c.baikeNum = this.baikeNum;
            c.minConfidenceThreshold = this.minConfidenceThreshold == null ? null : this.minConfidenceThreshold;
            c.keywordDict = new LinkedHashMap<>();
            if (this.keywordDict != null) {
                for (Map.Entry<String, List<String>> e : this.keywordDict.entrySet()) {
                    c.keywordDict.put(e.getKey(), new ArrayList<>(e.getValue()));
                }
            }
            return c;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("baikeNum", baikeNum);
            map.put("minConfidenceThreshold", minConfidenceThreshold);
            map.put("keywordDict", keywordDict);
            return map;
        }
    }
}
