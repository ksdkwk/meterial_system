package com.example.springbootbackeng.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.MaterialDemand;
import com.example.springbootbackeng.entity.MaterialDemandVO;

@Mapper
public interface MaterialDemandMapper extends BaseMapper<MaterialDemand> {

    @Select("""
            SELECT
                d.demand_id,
                d.demand_user_id,
                COALESCE(u.real_name, u.username, d.demand_user_id) AS demand_user_name,
                u.username AS demand_user_account,
                d.material_id,
                m.material_name,
                d.demand_quantity,
                d.use_time,
                d.demand_status,
                d.reject_reason,
                d.site_photo_url,
                d.submit_time,
                d.audit_time,
                d.auditor_id,
                au.real_name AS auditor_name
            FROM tb_material_demand d
            LEFT JOIN tb_user u ON u.user_id = d.demand_user_id
            LEFT JOIN tb_material m ON m.material_id = d.material_id
            LEFT JOIN tb_user au ON au.user_id = d.auditor_id
            WHERE (#{demandStatus} IS NULL OR d.demand_status = #{demandStatus})
              AND (
                #{keyword} IS NULL OR #{keyword} = ''
                OR m.material_name LIKE CONCAT('%', #{keyword}, '%')
                OR u.real_name LIKE CONCAT('%', #{keyword}, '%')
                OR d.demand_id LIKE CONCAT('%', #{keyword}, '%')
              )
            ORDER BY d.submit_time DESC
            """)
    List<MaterialDemandVO> selectAllWithDetails(@Param("demandStatus") Integer demandStatus,
                                                @Param("keyword") String keyword);
}
