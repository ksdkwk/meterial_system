package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.ReviewLogVO;

public interface ReviewLogService {
    List<ReviewLogVO> listAll(String keyword, String reviewState, String reviewResult);
}
