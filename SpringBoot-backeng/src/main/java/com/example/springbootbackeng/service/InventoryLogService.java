package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.InventoryLog;
import com.example.springbootbackeng.entity.InventoryLogVO;

public interface InventoryLogService {
    List<InventoryLogVO> listAll(Integer changeType, String action, String keyword);

    String create(InventoryLog log);

    String update(String logId, InventoryLog log);

    String softDelete(String logId, String operatorId);
}
