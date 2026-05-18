package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.AllocationPlan;
import com.example.springbootbackeng.entity.AllocationPlanVO;
import com.example.springbootbackeng.entity.SimpleOption;

public interface AllocationPlanService {
    List<AllocationPlanVO> listAll(Integer confirmStatus, String keyword);

    List<SimpleOption> listDemandOptions();

    List<SimpleOption> listAllocatorOptions();

    String create(AllocationPlan plan);
}
