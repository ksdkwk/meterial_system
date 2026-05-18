package com.example.springbootbackeng.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springbootbackeng.entity.Supplier;
import com.example.springbootbackeng.mapper.SupplierMapper;
import com.example.springbootbackeng.service.SupplierService;

@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    @Override
    public List<Supplier> list(String keyword, Integer status) {
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        qw.like(hasKeyword, Supplier::getSupplierName, keyword)
          .or(hasKeyword)
          .like(hasKeyword, Supplier::getContactPerson, keyword)
          .or(hasKeyword)
          .like(hasKeyword, Supplier::getContactPhone, keyword)
          .eq(status != null, Supplier::getStatus, status)
          .orderByDesc(Supplier::getCreateTime);
        return list(qw);
    }

        @Override
        public String create(Supplier supplier) {
          if (supplier == null) {
            return "请求参数不能为空";
          }
          if (supplier.getSupplierName() == null || supplier.getSupplierName().trim().isEmpty()) {
            return "供应商公司名称不能为空";
          }

          if (supplier.getSupplierId() == null || supplier.getSupplierId().trim().isEmpty()) {
            supplier.setSupplierId(UUID.randomUUID().toString().replace("-", ""));
          }
          if (supplier.getStatus() == null) {
            supplier.setStatus(1);
          }

          return save(supplier) ? null : "新增供应商失败";
        }

        @Override
        public String update(String supplierId, Supplier supplier) {
          if (supplierId == null || supplierId.trim().isEmpty()) {
            return "供应商ID不能为空";
          }
          Supplier old = getById(supplierId);
          if (old == null) {
            return "供应商不存在";
          }
          if (supplier == null) {
            return "请求参数不能为空";
          }
          if (supplier.getSupplierName() == null || supplier.getSupplierName().trim().isEmpty()) {
            return "供应商公司名称不能为空";
          }

          supplier.setSupplierId(supplierId);
          if (supplier.getStatus() == null) {
            supplier.setStatus(old.getStatus() == null ? 1 : old.getStatus());
          }

          return updateById(supplier) ? null : "更新供应商失败";
        }

        @Override
        public String delete(String supplierId) {
          if (supplierId == null || supplierId.trim().isEmpty()) {
            return "供应商ID不能为空";
          }
          Supplier old = getById(supplierId);
          if (old == null) {
            return "供应商不存在";
          }
          return removeById(supplierId) ? null : "删除供应商失败";
        }
}
