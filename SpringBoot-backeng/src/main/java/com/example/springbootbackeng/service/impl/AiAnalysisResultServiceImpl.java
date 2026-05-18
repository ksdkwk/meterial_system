package com.example.springbootbackeng.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springbootbackeng.dto.AiAnalysisRunRequest;
import com.example.springbootbackeng.entity.AiAnalysisResult;
import com.example.springbootbackeng.entity.Inventory;
import com.example.springbootbackeng.entity.InventoryLog;
import com.example.springbootbackeng.entity.Material;
import com.example.springbootbackeng.entity.MaterialDemand;
import com.example.springbootbackeng.mapper.AiAnalysisResultMapper;
import com.example.springbootbackeng.mapper.InventoryLogMapper;
import com.example.springbootbackeng.mapper.InventoryMapper;
import com.example.springbootbackeng.mapper.MaterialDemandMapper;
import com.example.springbootbackeng.mapper.MaterialMapper;
import com.example.springbootbackeng.service.AiAnalysisResultService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiAnalysisResultServiceImpl implements AiAnalysisResultService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AiAnalysisResultMapper aiAnalysisResultMapper;
    private final MaterialMapper materialMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final MaterialDemandMapper materialDemandMapper;
    private final Environment environment;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnalysisResultServiceImpl(AiAnalysisResultMapper aiAnalysisResultMapper,
                                       MaterialMapper materialMapper,
                                       InventoryMapper inventoryMapper,
                                       InventoryLogMapper inventoryLogMapper,
                                       MaterialDemandMapper materialDemandMapper,
                                       Environment environment) {
        this.aiAnalysisResultMapper = aiAnalysisResultMapper;
        this.materialMapper = materialMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.materialDemandMapper = materialDemandMapper;
        this.environment = environment;
    }

    @Override
    public Map<String, Object> getAiConfigCheck() {
        String baseUrl = resolveConfig("BASE_URL", "BASE URL", "base.url", "ai.deepseek.base-url");
        String apiKey = resolveConfig("DEEPSEEK_API", "DEEPSEEK API", "DEEPSEEK_API_KEY", "deepseek.api", "ai.deepseek.api-key");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("baseUrlConfigured", baseUrl != null);
        data.put("deepseekApiConfigured", apiKey != null);
        data.put("baseUrlMasked", maskBaseUrl(baseUrl));
        data.put("deepseekApiMasked", maskSecret(apiKey));
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runInventoryAnalysis(AiAnalysisRunRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        String createUserId = trim(req.getCreateUserId());
        if (createUserId == null) {
            throw new IllegalArgumentException("createUserId 不能为空");
        }

        LocalDateTime endTime = parseOrDefault(req.getEndTime(), LocalDateTime.now());
        LocalDateTime startTime = parseOrDefault(req.getStartTime(), endTime.minusDays(30));
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime 不能晚于 endTime");
        }

        Integer timeGranularity = normalizeGranularity(req.getTimeGranularity());
        Integer stockoutCalcType = normalizeStockoutCalcType(req.getStockoutCalcType());
        String filterMaterialId = trim(req.getMaterialId());
        String filterMaterialType = trim(req.getMaterialType());
        String filterStorageLocation = trim(req.getStorageLocation());

        List<Material> allMaterials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getStatus, 1));
        Map<String, Material> materialMap = allMaterials.stream()
                .filter(m -> trim(m.getMaterialId()) != null)
                .collect(Collectors.toMap(Material::getMaterialId, m -> m, (a, b) -> a));

        List<Inventory> allInventories = inventoryMapper.selectList(null);
        List<InventoryLog> allLogs = inventoryLogMapper.selectList(new LambdaQueryWrapper<InventoryLog>()
                .eq(InventoryLog::getLogStatus, 1)
                .ge(InventoryLog::getOperateTime, startTime)
                .le(InventoryLog::getOperateTime, endTime));
        List<MaterialDemand> allDemands = materialDemandMapper.selectList(new LambdaQueryWrapper<MaterialDemand>()
                .ge(MaterialDemand::getSubmitTime, startTime)
                .le(MaterialDemand::getSubmitTime, endTime));

        List<Inventory> inventories = allInventories.stream()
                .filter(i -> filterInventory(i, materialMap.get(i.getMaterialId()), filterMaterialId, filterMaterialType, filterStorageLocation))
                .collect(Collectors.toList());

        Set<String> materialIdScope = new LinkedHashSet<>();
        for (Inventory i : inventories) {
            String id = trim(i.getMaterialId());
            if (id != null) {
                materialIdScope.add(id);
            }
        }
        for (InventoryLog l : allLogs) {
            String id = trim(l.getMaterialId());
            if (id != null) {
                materialIdScope.add(id);
            }
        }
        for (MaterialDemand d : allDemands) {
            String id = trim(d.getMaterialId());
            if (id != null) {
                materialIdScope.add(id);
            }
        }

        if (filterMaterialId != null) {
            materialIdScope = materialIdScope.stream()
                    .filter(filterMaterialId::equals)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (filterMaterialType != null) {
            final Set<String> scopeForTypeFilter = materialIdScope;
            materialIdScope = scopeForTypeFilter.stream()
                    .filter(id -> {
                        Material m = materialMap.get(id);
                        return m != null && filterMaterialType.equals(trim(m.getMaterialType()));
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        Map<String, Integer> stockByMaterial = new HashMap<>();
        Map<String, Integer> thresholdByMaterial = new HashMap<>();
        Map<String, String> materialNameById = new HashMap<>();

        for (String materialId : materialIdScope) {
            Material m = materialMap.get(materialId);
            materialNameById.put(materialId, m != null && trim(m.getMaterialName()) != null ? m.getMaterialName() : materialId);
            thresholdByMaterial.put(materialId, safeInt(m == null ? null : m.getMinThreshold()));
        }

        for (Inventory inv : inventories) {
            String materialId = trim(inv.getMaterialId());
            if (materialId == null || !materialIdScope.contains(materialId)) continue;
            stockByMaterial.merge(materialId, safeInt(inv.getStockQuantity()), Integer::sum);
        }

        Map<String, Integer> outboundByMaterial = new HashMap<>();
        Map<String, Integer> inboundByMaterial = new HashMap<>();
        Map<String, Integer> demandTotalByMaterial = new HashMap<>();
        Map<String, Integer> demandPendingByMaterial = new HashMap<>();

        for (InventoryLog log : allLogs) {
            String materialId = trim(log.getMaterialId());
            if (materialId == null || !materialIdScope.contains(materialId)) continue;
            int qty = Math.abs(safeInt(log.getChangeQuantity()));
            int changeType = safeInt(log.getChangeType());
            if (changeType == 2) {
                outboundByMaterial.merge(materialId, qty, Integer::sum);
            } else {
                inboundByMaterial.merge(materialId, qty, Integer::sum);
            }
        }

        for (MaterialDemand demand : allDemands) {
            String materialId = trim(demand.getMaterialId());
            if (materialId == null || !materialIdScope.contains(materialId)) continue;
            int qty = Math.max(0, safeInt(demand.getDemandQuantity()));
            demandTotalByMaterial.merge(materialId, qty, Integer::sum);
            if (safeInt(demand.getDemandStatus()) != 4) {
                demandPendingByMaterial.merge(materialId, qty, Integer::sum);
            }
        }

        List<Map<String, Object>> trendItems = new ArrayList<>();
        List<Map<String, Object>> turnoverItems = new ArrayList<>();
        List<Map<String, Object>> shortageItems = new ArrayList<>();

        BigDecimal totalOutbound = BigDecimal.ZERO;
        BigDecimal totalCurrentStock = BigDecimal.ZERO;
        BigDecimal sumShortageWarningRate = BigDecimal.ZERO;
        BigDecimal sumDemandShortageRate = BigDecimal.ZERO;

        for (String materialId : materialIdScope) {
            int currentStock = stockByMaterial.getOrDefault(materialId, 0);
            int inbound = inboundByMaterial.getOrDefault(materialId, 0);
            int outbound = outboundByMaterial.getOrDefault(materialId, 0);
            int minThreshold = thresholdByMaterial.getOrDefault(materialId, 0);
            int demandTotal = demandTotalByMaterial.getOrDefault(materialId, 0);
            int demandPending = demandPendingByMaterial.getOrDefault(materialId, 0);

            BigDecimal avgStock = BigDecimal.valueOf(Math.max(1, currentStock));
            BigDecimal turnoverRate = BigDecimal.valueOf(outbound)
                    .divide(avgStock, 6, RoundingMode.HALF_UP);

            BigDecimal warningShortageRate = minThreshold > 0 && currentStock < minThreshold
                    ? BigDecimal.valueOf(minThreshold - currentStock)
                        .divide(BigDecimal.valueOf(minThreshold), 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal demandShortageRate = demandTotal > 0
                    ? BigDecimal.valueOf(demandPending)
                        .divide(BigDecimal.valueOf(demandTotal), 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal overallShortageRate = warningShortageRate
                    .add(demandShortageRate)
                    .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);

            int net = inbound - outbound;
            String direction = net > 0 ? "UP" : (net < 0 ? "DOWN" : "FLAT");

            trendItems.add(new LinkedHashMap<>(Map.of(
                    "materialId", materialId,
                    "materialName", materialNameById.getOrDefault(materialId, materialId),
                    "currentStock", currentStock,
                    "inboundTotal", inbound,
                    "outboundTotal", outbound,
                    "netChange", net,
                    "trendDirection", direction
            )));

            turnoverItems.add(new LinkedHashMap<>(Map.of(
                    "materialId", materialId,
                    "materialName", materialNameById.getOrDefault(materialId, materialId),
                    "outboundTotal", outbound,
                    "averageStock", avgStock,
                    "turnoverRate", turnoverRate
            )));

            shortageItems.add(new LinkedHashMap<>(Map.of(
                    "materialId", materialId,
                    "materialName", materialNameById.getOrDefault(materialId, materialId),
                    "currentStock", currentStock,
                    "minThreshold", minThreshold,
                    "warningShortageRate", warningShortageRate,
                    "pendingDemand", demandPending,
                    "totalDemand", demandTotal,
                    "demandShortageRate", demandShortageRate,
                    "overallShortageRate", overallShortageRate
            )));

            totalOutbound = totalOutbound.add(BigDecimal.valueOf(outbound));
            totalCurrentStock = totalCurrentStock.add(BigDecimal.valueOf(currentStock));
            sumShortageWarningRate = sumShortageWarningRate.add(warningShortageRate);
            sumDemandShortageRate = sumDemandShortageRate.add(demandShortageRate);
        }

        int materialCount = Math.max(1, materialIdScope.size());
        BigDecimal overallTurnoverRate = totalOutbound.divide(
                totalCurrentStock.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : totalCurrentStock,
                6,
                RoundingMode.HALF_UP
        );
        BigDecimal inventoryWarningShortageRate = sumShortageWarningRate
                .divide(BigDecimal.valueOf(materialCount), 6, RoundingMode.HALF_UP);
        BigDecimal demandSatisfyShortageRate = sumDemandShortageRate
                .divide(BigDecimal.valueOf(materialCount), 6, RoundingMode.HALF_UP);
        BigDecimal overallShortageRate = inventoryWarningShortageRate
                .add(demandSatisfyShortageRate)
                .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);

        Map<String, Object> localPayload = new LinkedHashMap<>();
        localPayload.put("timeRange", Map.of(
                "startTime", DATE_TIME_FMT.format(startTime),
                "endTime", DATE_TIME_FMT.format(endTime),
                "timeGranularity", timeGranularity
        ));
        localPayload.put("scope", Map.of(
                "materialId", defaultIfNull(filterMaterialId, "ALL"),
                "materialType", defaultIfNull(filterMaterialType, "ALL"),
                "storageLocation", defaultIfNull(filterStorageLocation, "ALL"),
                "materialCount", materialIdScope.size()
        ));
        localPayload.put("trend", Map.of("items", trendItems));
        localPayload.put("turnover", Map.of(
                "overallTurnoverRate", overallTurnoverRate,
                "items", turnoverItems
        ));
        localPayload.put("shortage", Map.of(
                "inventoryWarningShortageRate", inventoryWarningShortageRate,
                "demandSatisfyShortageRate", demandSatisfyShortageRate,
                "overallShortageRate", overallShortageRate,
                "items", shortageItems,
                "stockoutCalcType", stockoutCalcType
        ));

        Map<String, Object> aiJson = invokeDeepSeekAndParse(localPayload);

        Map<String, Object> trendResult = new LinkedHashMap<>();
        trendResult.put("rawMetrics", localPayload.get("trend"));
        trendResult.put("ai", aiJson.getOrDefault("trend", Map.of()));

        Map<String, Object> turnoverResult = new LinkedHashMap<>();
        turnoverResult.put("rawMetrics", localPayload.get("turnover"));
        turnoverResult.put("ai", aiJson.getOrDefault("turnover", Map.of()));

        Map<String, Object> shortageResult = new LinkedHashMap<>();
        shortageResult.put("rawMetrics", localPayload.get("shortage"));
        shortageResult.put("ai", aiJson.getOrDefault("shortage", Map.of()));

        List<AiAnalysisResult> created = new ArrayList<>();
        created.add(insertResult(1, 0, timeGranularity, startTime, endTime, filterMaterialId, filterMaterialType,
                filterStorageLocation, trendResult, null, createUserId));
        created.add(insertResult(2, 0, timeGranularity, startTime, endTime, filterMaterialId, filterMaterialType,
                filterStorageLocation, turnoverResult, overallTurnoverRate, createUserId));
        created.add(insertResult(3, stockoutCalcType, timeGranularity, startTime, endTime, filterMaterialId, filterMaterialType,
                filterStorageLocation, shortageResult, overallShortageRate.multiply(BigDecimal.valueOf(100)), createUserId));

        List<Map<String, Object>> records = created.stream().map(this::toRecordMap).collect(Collectors.toList());

        return new LinkedHashMap<>(Map.of(
                "runTime", DATE_TIME_FMT.format(LocalDateTime.now()),
                "records", records
        ));
    }

    @Override
    public Map<String, Object> listResults(Integer analysisType, Integer timeGranularity, Integer pageNum, Integer pageSize) {
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);

        LambdaQueryWrapper<AiAnalysisResult> wrapper = new LambdaQueryWrapper<>();
        if (analysisType != null) wrapper.eq(AiAnalysisResult::getAnalysisType, analysisType);
        if (timeGranularity != null) wrapper.eq(AiAnalysisResult::getTimeGranularity, timeGranularity);
        wrapper.orderByDesc(AiAnalysisResult::getCreateTime);

        List<AiAnalysisResult> all = aiAnalysisResultMapper.selectList(wrapper);
        int total = all.size();
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);
        List<AiAnalysisResult> page = from >= total ? List.of() : all.subList(from, to);

        List<Map<String, Object>> records = page.stream().map(this::toRecordMap).collect(Collectors.toList());

        return new LinkedHashMap<>(Map.of(
                "records", records,
                "total", total,
                "pageNum", validPageNum,
                "pageSize", validPageSize
        ));
    }

    @Override
    public Map<String, Object> getRawResult(String analysisId) {
        AiAnalysisResult result = aiAnalysisResultMapper.selectById(analysisId);
        if (result == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("analysisId", result.getAnalysisId());
        payload.put("analysisType", result.getAnalysisType());
        payload.put("analysisStatus", result.getAnalysisStatus());
        payload.put("analysisResult", result.getAnalysisResult());
        payload.put("createTime", result.getCreateTime());
        payload.put("finishTime", result.getFinishTime());
        return payload;
    }

    private Map<String, Object> invokeDeepSeekAndParse(Map<String, Object> localPayload) {
        String baseUrl = resolveConfig("BASE_URL", "BASE URL", "base.url", "ai.deepseek.base-url");
        String apiKey = resolveConfig("DEEPSEEK_API", "DEEPSEEK API", "DEEPSEEK_API_KEY", "deepseek.api", "ai.deepseek.api-key");
        if (baseUrl == null || apiKey == null) {
            throw new IllegalArgumentException("请在环境变量中配置 BASE_URL 与 DEEPSEEK_API（如刚修改，请重启IDE/终端后再启动后端）");
        }

        String endpoint;
        if (baseUrl.endsWith("/chat/completions")) {
            endpoint = baseUrl;
        } else if (baseUrl.endsWith("/")) {
            endpoint = baseUrl + "chat/completions";
        } else {
            endpoint = baseUrl + "/chat/completions";
        }
        try {
            String promptPayload = objectMapper.writeValueAsString(localPayload);
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("temperature", 0.2);
            requestBody.put("response_format", Map.of("type", "json_object"));
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "你是建筑物料库存分析助手。仅输出JSON，不要markdown。"),
                    Map.of("role", "user", "content", "请根据输入数据输出JSON，字段必须包含 trend/turnover/shortage 三个对象。"
                            + "trend含summary/highlights；turnover含summary/highlights；shortage含summary/highlights。"
                            + "输入数据如下：" + promptPayload)
            ));

            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI调用失败: HTTP " + response.statusCode() + " - " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (trim(content) == null) {
                throw new IllegalStateException("AI响应内容为空");
            }

            String cleaned = stripCodeFence(content);
            return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("调用AI分析失败: " + e.getMessage(), e);
        }
    }

    private AiAnalysisResult insertResult(int analysisType,
                                          int stockoutCalcType,
                                          int timeGranularity,
                                          LocalDateTime startTime,
                                          LocalDateTime endTime,
                                          String materialId,
                                          String materialType,
                                          String storageLocation,
                                          Map<String, Object> analysisResult,
                                          BigDecimal coreIndicator,
                                          String createUserId) {
        try {
            AiAnalysisResult row = new AiAnalysisResult();
            row.setAnalysisId(UUID.randomUUID().toString().replace("-", ""));
            row.setAnalysisType(analysisType);
            row.setStockoutCalcType(stockoutCalcType);
            row.setTimeGranularity(timeGranularity);
            row.setStartTime(startTime);
            row.setEndTime(endTime);
            row.setMaterialId(materialId);
            row.setMaterialType(materialType);
            row.setStorageLocation(storageLocation);
            row.setAnalysisResult(objectMapper.writeValueAsString(analysisResult));
            row.setCoreIndicator(coreIndicator == null ? null : coreIndicator.setScale(4, RoundingMode.HALF_UP));
            row.setCreateUserId(createUserId);
            row.setFinishTime(LocalDateTime.now());
            row.setAnalysisStatus(1);
            row.setFailReason(null);
            aiAnalysisResultMapper.insert(row);
            return row;
        } catch (Exception e) {
            throw new IllegalStateException("写入AI分析结果失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> toRecordMap(AiAnalysisResult row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("analysisId", row.getAnalysisId());
        map.put("analysisType", row.getAnalysisType());
        map.put("stockoutCalcType", row.getStockoutCalcType());
        map.put("timeGranularity", row.getTimeGranularity());
        map.put("startTime", row.getStartTime());
        map.put("endTime", row.getEndTime());
        map.put("materialId", row.getMaterialId());
        map.put("materialType", row.getMaterialType());
        map.put("storageLocation", row.getStorageLocation());
        map.put("coreIndicator", row.getCoreIndicator());
        map.put("createUserId", row.getCreateUserId());
        map.put("createTime", row.getCreateTime());
        map.put("finishTime", row.getFinishTime());
        map.put("analysisStatus", row.getAnalysisStatus());
        map.put("failReason", row.getFailReason());

        Object parsed = null;
        try {
            if (trim(row.getAnalysisResult()) != null) {
                parsed = objectMapper.readValue(row.getAnalysisResult(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            parsed = Map.of("raw", row.getAnalysisResult());
        }
        map.put("analysisResult", parsed);
        return map;
    }

    private boolean filterInventory(Inventory i,
                                    Material m,
                                    String materialId,
                                    String materialType,
                                    String storageLocation) {
        if (materialId != null && !Objects.equals(materialId, trim(i.getMaterialId()))) return false;
        if (materialType != null) {
            String type = m == null ? null : trim(m.getMaterialType());
            if (!Objects.equals(materialType, type)) return false;
        }
        if (storageLocation != null && !Objects.equals(storageLocation, trim(i.getStorageLocation()))) return false;
        return true;
    }

    private Integer normalizeGranularity(Integer v) {
        int g = v == null ? 2 : v;
        if (g < 1 || g > 4) return 2;
        return g;
    }

    private Integer normalizeStockoutCalcType(Integer v) {
        int t = v == null ? 2 : v;
        if (t < 0 || t > 2) return 2;
        return t;
    }

    private LocalDateTime parseOrDefault(String text, LocalDateTime fallback) {
        String v = trim(text);
        if (v == null) return fallback;
        List<String> formats = List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm");
        for (String f : formats) {
            try {
                return LocalDateTime.parse(v, DateTimeFormatter.ofPattern(f, Locale.CHINA));
            } catch (DateTimeParseException ignored) {
            }
        }
        return fallback;
    }

    private String stripCodeFence(String text) {
        String v = text.trim();
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

    private String trim(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private String unquote(String s) {
        String v = trim(s);
        if (v == null) return null;
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            String inner = v.substring(1, v.length() - 1).trim();
            return inner.isEmpty() ? null : inner;
        }
        return v;
    }

    private String resolveConfig(String... keys) {
        if (keys == null) return null;
        for (String key : keys) {
            if (trim(key) == null) continue;
            String byEnv = unquote(System.getenv(key));
            if (byEnv != null) return byEnv;

            String byProperty = unquote(System.getProperty(key));
            if (byProperty != null) return byProperty;

            String bySpringProperty = unquote(environment.getProperty(key));
            if (bySpringProperty != null) return bySpringProperty;
        }
        return null;
    }

    private int safeInt(Integer n) {
        return n == null ? 0 : n;
    }

    private String defaultIfNull(String s, String fallback) {
        return s == null ? fallback : s;
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
}
