package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.OperationLog;
import com.example.springbootbackeng.entity.OperationLogVO;

public interface OperationLogService {
    List<OperationLogVO> listAll(String keyword, String operationType);

    String create(OperationLog log);
}
