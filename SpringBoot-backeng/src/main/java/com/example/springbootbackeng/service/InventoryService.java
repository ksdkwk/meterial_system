package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.Inventory;
import com.example.springbootbackeng.entity.InventoryVO;

public interface InventoryService {
    List<InventoryVO> listAll(String keyword);

    String create(Inventory inventory, String operatorId);

    String update(String inventoryId, Inventory inventory, String operatorId);

    String delete(String inventoryId, String operatorId);
}
