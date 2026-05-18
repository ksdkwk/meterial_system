package com.example.springbootbackeng.service;

import java.util.List;

import com.example.springbootbackeng.entity.Team;

public interface TeamService {
    List<Team> list(String keyword, Integer status);

    String create(Team team);

    String update(String teamId, Team team);

    String delete(String teamId);
}
