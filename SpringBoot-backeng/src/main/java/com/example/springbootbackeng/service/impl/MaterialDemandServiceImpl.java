package com.example.springbootbackeng.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springbootbackeng.entity.Material;
import com.example.springbootbackeng.entity.MaterialDemand;
import com.example.springbootbackeng.entity.MaterialDemandVO;
import com.example.springbootbackeng.entity.SimpleOption;
import com.example.springbootbackeng.entity.User;
import com.example.springbootbackeng.mapper.MaterialDemandMapper;
import com.example.springbootbackeng.mapper.MaterialMapper;
import com.example.springbootbackeng.mapper.ReviewLogMapper;
import com.example.springbootbackeng.mapper.UserMapper;
import com.example.springbootbackeng.mq.MqEventPublisher;
import com.example.springbootbackeng.mq.OperationLogMessage;
import com.example.springbootbackeng.mq.OrderCreatedMessage;
import com.example.springbootbackeng.service.MaterialDemandService;

@Service
public class MaterialDemandServiceImpl implements MaterialDemandService {

    private final MaterialDemandMapper materialDemandMapper;
    private final UserMapper userMapper;
    private final MaterialMapper materialMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final MqEventPublisher mqEventPublisher;

    public MaterialDemandServiceImpl(MaterialDemandMapper materialDemandMapper,
                                     UserMapper userMapper,
                                     MaterialMapper materialMapper,
                                     ReviewLogMapper reviewLogMapper,
                                     MqEventPublisher mqEventPublisher) {
        this.materialDemandMapper = materialDemandMapper;
        this.userMapper = userMapper;
        this.materialMapper = materialMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.mqEventPublisher = mqEventPublisher;
    }

    @Override
    public List<MaterialDemandVO> listAll(Integer demandStatus, String keyword) {
        return materialDemandMapper.selectAllWithDetails(demandStatus, keyword);
    }

    @Override
    public String create(MaterialDemand demand) {
        if (demand == null) {
            return "请求参数不能为空";
        }
        if (demand.getDemandUserId() == null || demand.getDemandUserId().trim().isEmpty()) {
            return "申请人ID不能为空";
        }
        if (demand.getMaterialId() == null || demand.getMaterialId().trim().isEmpty()) {
            return "物料ID不能为空";
        }
        if (demand.getDemandQuantity() == null || demand.getDemandQuantity() <= 0) {
            return "申请数量必须大于0";
        }
        if (demand.getUseTime() == null) {
            return "期望使用时间不能为空";
        }

        if (demand.getDemandId() == null || demand.getDemandId().trim().isEmpty()) {
            demand.setDemandId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (demand.getDemandStatus() == null) {
            demand.setDemandStatus(0);
        }

        boolean success = materialDemandMapper.insert(demand) > 0;
        if (!success) {
            return "新增需求失败";
        }

        OrderCreatedMessage orderMsg = new OrderCreatedMessage();
        orderMsg.setDemandId(demand.getDemandId());
        orderMsg.setDemandUserId(demand.getDemandUserId());
        orderMsg.setMaterialId(demand.getMaterialId());
        orderMsg.setDemandQuantity(demand.getDemandQuantity());
        orderMsg.setUseTime(demand.getUseTime());
        orderMsg.setCreatedAt(LocalDateTime.now());
        mqEventPublisher.publishOrderCreated(orderMsg);

        OperationLogMessage opMsg = new OperationLogMessage();
        opMsg.setUserId(demand.getDemandUserId());
        opMsg.setOperationModule("物料需求");
        opMsg.setOperationType("ADD");
        opMsg.setOperationDesc("提交需求单: " + demand.getDemandId());
        opMsg.setRequestIp(null);
        mqEventPublisher.publishOperationLog(opMsg);

        return null;
    }

    @Override
    public String audit(String demandId, Integer demandStatus, String rejectReason, String auditorId) {
        if (demandId == null || demandId.trim().isEmpty()) {
            return "需求单号不能为空";
        }
        if (demandStatus == null || (demandStatus != 1 && demandStatus != 2)) {
            return "审批状态不合法";
        }
        if (auditorId == null || auditorId.trim().isEmpty()) {
            return "审批人不能为空";
        }

        MaterialDemand db = materialDemandMapper.selectById(demandId);
        if (db == null) {
            return "需求不存在";
        }

        if (db.getDemandStatus() != null && db.getDemandStatus() != 0) {
            return "仅待审核需求可审批";
        }

        if (demandStatus == 2) {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                return "驳回时必须填写驳回理由";
            }
            db.setRejectReason(rejectReason.trim());
        } else {
            db.setRejectReason(null);
        }

        db.setDemandStatus(demandStatus);
        String auditorIdTrim = auditorId.trim();
        db.setAuditorId(auditorIdTrim);
        db.setAuditTime(LocalDateTime.now());

        int demandUpdated = materialDemandMapper.updateById(db);
        if (demandUpdated <= 0) {
            return "审批操作失败";
        }

        User auditor = userMapper.selectById(auditorIdTrim);
        String approveName = auditor == null
                ? auditorIdTrim
                : (auditor.getRealName() == null || auditor.getRealName().trim().isEmpty()
                    ? auditor.getUsername()
                    : auditor.getRealName());

        String reviewState = demandStatus == 1 ? "已通过" : "已驳回";
        String reviewResult = demandStatus == 1 ? "通过" : "驳回";
        String reviewReason = demandStatus == 1 ? "审批通过" : rejectReason.trim();
        String reviewOther = demandStatus == 1 ? "需求审批通过" : "需求审批驳回";

        int affected = reviewLogMapper.updateAuditByDemandId(
                demandId,
                auditorIdTrim,
                approveName,
                reviewState,
                reviewResult,
                reviewReason,
                reviewOther
        );

        if (affected <= 0) {
            int inserted = reviewLogMapper.insertAuditRecordByDemandId(
                    demandId,
                    auditorIdTrim,
                    approveName,
                    reviewState,
                    reviewResult,
                    reviewReason,
                    reviewOther
            );
            if (inserted <= 0) {
                return "审批已完成，但审批记录写入失败";
            }
        }

        OperationLogMessage opMsg = new OperationLogMessage();
        opMsg.setUserId(auditorIdTrim);
        opMsg.setOperationModule("需求审批");
        opMsg.setOperationType("AUDIT");
        opMsg.setOperationDesc("审批需求单 " + demandId + "，结果: " + reviewResult);
        opMsg.setRequestIp(null);
        mqEventPublisher.publishOperationLog(opMsg);

        return null;
    }

        @Override
        public List<SimpleOption> listUserOptions() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1)
            .orderByAsc(User::getRealName)
            .orderByAsc(User::getUsername);

        return userMapper.selectList(wrapper).stream()
            .map(u -> new SimpleOption(
                u.getUserId(),
                (u.getRealName() == null || u.getRealName().trim().isEmpty())
                    ? u.getUsername()
                    : (u.getRealName() + "(" + u.getUsername() + ")")
            ))
            .collect(Collectors.toList());
        }

        @Override
        public List<SimpleOption> listMaterialOptions() {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Material::getStatus, 1)
            .orderByAsc(Material::getMaterialName);

        return materialMapper.selectList(wrapper).stream()
            .map(m -> new SimpleOption(m.getMaterialId(), m.getMaterialName()))
            .collect(Collectors.toList());
        }
}
