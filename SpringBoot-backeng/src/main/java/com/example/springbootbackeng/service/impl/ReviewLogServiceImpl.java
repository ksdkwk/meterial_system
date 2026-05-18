package com.example.springbootbackeng.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.springbootbackeng.entity.ReviewLogVO;
import com.example.springbootbackeng.mapper.ReviewLogMapper;
import com.example.springbootbackeng.service.ReviewLogService;

@Service
public class ReviewLogServiceImpl implements ReviewLogService {

    private final ReviewLogMapper reviewLogMapper;

    public ReviewLogServiceImpl(ReviewLogMapper reviewLogMapper) {
        this.reviewLogMapper = reviewLogMapper;
    }

    @Override
    public List<ReviewLogVO> listAll(String keyword, String reviewState, String reviewResult) {
        return reviewLogMapper.selectAll(keyword, reviewState, reviewResult);
    }
}
