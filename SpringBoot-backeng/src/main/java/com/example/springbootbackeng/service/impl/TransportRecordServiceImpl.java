package com.example.springbootbackeng.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springbootbackeng.entity.AllocationPlan;
import com.example.springbootbackeng.entity.SimpleOption;
import com.example.springbootbackeng.entity.TransportRecord;
import com.example.springbootbackeng.entity.TransportRecordVO;
import com.example.springbootbackeng.entity.Vehicle;
import com.example.springbootbackeng.mapper.AllocationPlanMapper;
import com.example.springbootbackeng.mapper.TransportRecordMapper;
import com.example.springbootbackeng.mapper.VehicleMapper;
import com.example.springbootbackeng.service.TransportRecordService;

@Service
public class TransportRecordServiceImpl implements TransportRecordService {

    private final TransportRecordMapper transportRecordMapper;
    private final AllocationPlanMapper allocationPlanMapper;
    private final VehicleMapper vehicleMapper;

    public TransportRecordServiceImpl(TransportRecordMapper transportRecordMapper,
                                      AllocationPlanMapper allocationPlanMapper,
                                      VehicleMapper vehicleMapper) {
        this.transportRecordMapper = transportRecordMapper;
        this.allocationPlanMapper = allocationPlanMapper;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    public List<TransportRecordVO> listAll(Integer transportStatus, String keyword) {
        return transportRecordMapper.selectAllWithDetails(transportStatus, keyword);
    }

    @Override
    public List<SimpleOption> listAvailablePlanOptions() {
        return transportRecordMapper.selectAvailablePlanOptions();
    }

    @Override
    public List<Vehicle> listVehicleOptions() {
        return transportRecordMapper.selectVehicleOptions();
    }

    @Override
    public String create(TransportRecord record) {
        if (record == null) {
            return "请求参数不能为空";
        }
        String planId = record.getPlanId() == null ? "" : record.getPlanId().trim();
        if (planId.isEmpty()) {
            return "方案单号不能为空";
        }
        record.setPlanId(planId);

        AllocationPlan plan = allocationPlanMapper.selectById(planId);
        if (plan == null) {
            return "方案单号不存在";
        }

        long existed = transportRecordMapper.selectCount(
                new QueryWrapper<TransportRecord>().eq("plan_id", planId)
        );
        if (existed > 0) {
            return "该方案已存在运输记录，请勿重复新增";
        }

        String vehicleInput = record.getVehicleId() == null ? "" : record.getVehicleId().trim();
        if (!vehicleInput.isEmpty()) {
            Vehicle byId = vehicleMapper.selectById(vehicleInput);
            if (byId != null) {
                record.setVehicleId(byId.getVehicleId());
            } else {
                Vehicle byPlate = vehicleMapper.selectOne(
                        new QueryWrapper<Vehicle>().eq("plate_number", vehicleInput)
                );
                if (byPlate == null) {
                    return "车辆ID/车牌不存在";
                }
                record.setVehicleId(byPlate.getVehicleId());
            }
        } else {
            record.setVehicleId(null);
        }

        if (record.getRecordId() == null || record.getRecordId().trim().isEmpty()) {
            record.setRecordId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (record.getTransportStatus() == null) {
            record.setTransportStatus(0);
        }

        try {
            return transportRecordMapper.insert(record) > 0 ? null : "新增运输记录失败";
        } catch (DataIntegrityViolationException e) {
            return "新增运输记录失败：数据约束冲突（请检查方案单号与车辆信息）";
        }
    }
}
