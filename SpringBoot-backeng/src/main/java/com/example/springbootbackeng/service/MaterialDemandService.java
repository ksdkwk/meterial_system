package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.MaterialDemand;
import com.example.springbootbackeng.entity.MaterialDemandVO;
import com.example.springbootbackeng.entity.SimpleOption;

public interface MaterialDemandService {
    List<MaterialDemandVO> listAll(Integer demandStatus, String keyword);

    String create(MaterialDemand demand);

    String audit(String demandId, Integer demandStatus, String rejectReason, String auditorId);

    List<SimpleOption> listUserOptions();

    List<SimpleOption> listMaterialOptions();
}
