package com.example.springbootbackeng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.entity.Material;
import com.example.springbootbackeng.service.MaterialService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/materials")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return Result.success(materialService.list(keyword, status));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable("id") String id) {
        return Result.success(materialService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody Material material) {
        return materialService.create(material) ? Result.success("新增成功") : Result.error("新增失败");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable("id") String id, @RequestBody Material material) {
        return materialService.update(id, material) ? Result.success("修改成功") : Result.error("修改失败");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") String id) {
        return materialService.delete(id) ? Result.success("删除成功") : Result.error("删除失败");
    }
}