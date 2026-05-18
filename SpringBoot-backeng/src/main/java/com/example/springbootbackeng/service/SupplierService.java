package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.Supplier;

public interface SupplierService {
    List<Supplier> list(String keyword, Integer status);

    String create(Supplier supplier);

    String update(String supplierId, Supplier supplier);

    String delete(String supplierId);
}
