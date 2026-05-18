package com.example.springbootbackeng.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springbootbackeng.entity.Team;
import com.example.springbootbackeng.mapper.TeamMapper;
import com.example.springbootbackeng.service.TeamService;

@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Override
    public List<Team> list(String keyword, Integer status) {
        LambdaQueryWrapper<Team> qw = new LambdaQueryWrapper<>();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        qw.like(hasKeyword, Team::getTeamName, keyword)
          .or(hasKeyword)
          .like(hasKeyword, Team::getLeaderName, keyword)
          .eq(status != null, Team::getStatus, status)
          .orderByDesc(Team::getCreateTime);
        return list(qw);
    }

    @Override
    public String create(Team team) {
      if (team == null) {
        return "请求参数不能为空";
      }
      if (team.getTeamName() == null || team.getTeamName().trim().isEmpty()) {
        return "班组名称不能为空";
      }
      if (team.getLeaderName() == null || team.getLeaderName().trim().isEmpty()) {
        return "班组长不能为空";
      }

      if (team.getTeamId() == null || team.getTeamId().trim().isEmpty()) {
        team.setTeamId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
      }
      if (team.getStatus() == null) {
        team.setStatus(1);
      }

      return save(team) ? null : "新增施工班组失败";
    }

    @Override
    public String update(String teamId, Team team) {
      if (teamId == null || teamId.trim().isEmpty()) {
        return "班组ID不能为空";
      }
      Team exists = getById(teamId);
      if (exists == null) {
        return "施工班组不存在";
      }
      if (team == null) {
        return "请求参数不能为空";
      }
      if (team.getTeamName() == null || team.getTeamName().trim().isEmpty()) {
        return "班组名称不能为空";
      }
      if (team.getLeaderName() == null || team.getLeaderName().trim().isEmpty()) {
        return "班组长不能为空";
      }

      team.setTeamId(teamId);
      if (team.getStatus() == null) {
        team.setStatus(exists.getStatus() == null ? 1 : exists.getStatus());
      }

      return updateById(team) ? null : "更新施工班组失败";
    }

    @Override
    public String delete(String teamId) {
      if (teamId == null || teamId.trim().isEmpty()) {
        return "班组ID不能为空";
      }
      Team exists = getById(teamId);
      if (exists == null) {
        return "施工班组不存在";
      }
      return removeById(teamId) ? null : "删除施工班组失败";
    }
}
