package com.example.springbootbackeng.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springbootbackeng.entity.Vehicle;
import com.example.springbootbackeng.mapper.VehicleMapper;
import com.example.springbootbackeng.service.VehicleService;

@Service
public class VehicleServiceImpl extends ServiceImpl<VehicleMapper, Vehicle> implements VehicleService {

    @Override
    public List<Vehicle> list(String keyword, Integer status) {
        LambdaQueryWrapper<Vehicle> qw = new LambdaQueryWrapper<>();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        qw.like(hasKeyword, Vehicle::getPlateNumber, keyword)
          .or(hasKeyword)
          .like(hasKeyword, Vehicle::getVehicleType, keyword)
          .or(hasKeyword)
          .like(hasKeyword, Vehicle::getDriverName, keyword)
          .or(hasKeyword)
          .like(hasKeyword, Vehicle::getDriverPhone, keyword)
          .eq(status != null, Vehicle::getStatus, status)
          .orderByDesc(Vehicle::getCreateTime);
        return list(qw);
    }

        @Override
        public String create(Vehicle vehicle) {
          if (vehicle == null) {
            return "请求参数不能为空";
          }
          if (vehicle.getPlateNumber() == null || vehicle.getPlateNumber().trim().isEmpty()) {
            return "车牌号码不能为空";
          }

          Long exists = count(new LambdaQueryWrapper<Vehicle>()
              .eq(Vehicle::getPlateNumber, vehicle.getPlateNumber().trim()));
          if (exists != null && exists > 0) {
            return "车牌号码已存在";
          }

          if (vehicle.getVehicleId() == null || vehicle.getVehicleId().trim().isEmpty()) {
            vehicle.setVehicleId(UUID.randomUUID().toString().replace("-", ""));
          }
          vehicle.setPlateNumber(vehicle.getPlateNumber().trim());
          if (vehicle.getStatus() == null) {
            vehicle.setStatus(1);
          }

          return save(vehicle) ? null : "新增车辆失败";
        }

        @Override
        public String update(String vehicleId, Vehicle vehicle) {
          if (vehicleId == null || vehicleId.trim().isEmpty()) {
            return "车辆ID不能为空";
          }
          Vehicle old = getById(vehicleId);
          if (old == null) {
            return "车辆不存在";
          }
          if (vehicle == null) {
            return "请求参数不能为空";
          }

          String plate = vehicle.getPlateNumber();
          if (plate == null || plate.trim().isEmpty()) {
            return "车牌号码不能为空";
          }
          plate = plate.trim();

          Long exists = count(new LambdaQueryWrapper<Vehicle>()
              .eq(Vehicle::getPlateNumber, plate)
              .ne(Vehicle::getVehicleId, vehicleId));
          if (exists != null && exists > 0) {
            return "车牌号码已存在";
          }

          vehicle.setVehicleId(vehicleId);
          vehicle.setPlateNumber(plate);
          if (vehicle.getStatus() == null) {
            vehicle.setStatus(old.getStatus() == null ? 1 : old.getStatus());
          }

          return updateById(vehicle) ? null : "更新车辆失败";
        }

        @Override
        public String delete(String vehicleId) {
          if (vehicleId == null || vehicleId.trim().isEmpty()) {
            return "车辆ID不能为空";
          }
          Vehicle old = getById(vehicleId);
          if (old == null) {
            return "车辆不存在";
          }
          return removeById(vehicleId) ? null : "删除车辆失败";
        }
}
