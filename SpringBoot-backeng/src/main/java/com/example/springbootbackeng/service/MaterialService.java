package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.Material;

public interface MaterialService {
    List<Material> list(String keyword, Integer status);
    Material getById(String materialId);
    boolean create(Material material);
    boolean update(String materialId, Material material);
    boolean delete(String materialId); // 执行逻辑删除：status=0
}