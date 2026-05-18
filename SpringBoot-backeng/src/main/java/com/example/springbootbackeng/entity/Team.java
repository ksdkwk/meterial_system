package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_team")
public class Team {

    @TableId(value = "team_id", type = IdType.INPUT)
    private String teamId;

    private String teamName;
    private String leaderName;
    private String leaderPhone;
    private String teamMember;
    private LocalDateTime createTime;
    private Integer status;
}
