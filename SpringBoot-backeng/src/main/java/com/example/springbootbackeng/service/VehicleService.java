package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.Vehicle;

public interface VehicleService {
    List<Vehicle> list(String keyword, Integer status);

    String create(Vehicle vehicle);

    String update(String vehicleId, Vehicle vehicle);

    String delete(String vehicleId);
}
