package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.SimpleOption;
import com.example.springbootbackeng.entity.TransportRecord;
import com.example.springbootbackeng.entity.TransportRecordVO;
import com.example.springbootbackeng.entity.Vehicle;

public interface TransportRecordService {
    List<TransportRecordVO> listAll(Integer transportStatus, String keyword);

    List<SimpleOption> listAvailablePlanOptions();

    List<Vehicle> listVehicleOptions();

    String create(TransportRecord record);
}
