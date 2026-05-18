package com.example.springbootbackeng.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.AllocationPlan;
import com.example.springbootbackeng.entity.AllocationPlanVO;
import com.example.springbootbackeng.entity.SimpleOption;

@Mapper
public interface AllocationPlanMapper extends BaseMapper<AllocationPlan> {

    @Select("""
            SELECT
                p.plan_id,
                p.demand_id,
                d.material_id,
                m.material_name,
                p.alloc_quantity,
                p.pick_location,
                p.pick_time,
                p.allocator_id,
                au.real_name AS allocator_name,
                p.confirm_status,
                p.sign_time,
                p.sign_user_id,
                su.real_name AS sign_user_name,
                p.sign_photo_url,
                p.create_time
            FROM tb_allocation_plan p
            LEFT JOIN tb_material_demand d ON d.demand_id = p.demand_id
            LEFT JOIN tb_material m ON m.material_id = d.material_id
            LEFT JOIN tb_user au ON au.user_id = p.allocator_id
            LEFT JOIN tb_user su ON su.user_id = p.sign_user_id
            WHERE (#{confirmStatus} IS NULL OR p.confirm_status = #{confirmStatus})
              AND (
                #{keyword} IS NULL OR #{keyword} = ''
                OR p.plan_id LIKE CONCAT('%', #{keyword}, '%')
                OR p.demand_id LIKE CONCAT('%', #{keyword}, '%')
                OR m.material_name LIKE CONCAT('%', #{keyword}, '%')
              )
            ORDER BY p.create_time DESC
            """)
    List<AllocationPlanVO> selectAllWithDetails(@Param("confirmStatus") Integer confirmStatus,
                                                @Param("keyword") String keyword);

    @Select("""
        SELECT
          d.demand_id AS id,
          CONCAT(d.demand_id, ' / ', COALESCE(m.material_name, d.material_id)) AS name
        FROM tb_material_demand d
        LEFT JOIN tb_material m ON m.material_id = d.material_id
        LEFT JOIN tb_allocation_plan p ON p.demand_id = d.demand_id
        WHERE p.plan_id IS NULL
          AND d.demand_status = 1
        ORDER BY d.submit_time DESC
        """)
    List<SimpleOption> selectDemandOptions();

    @Select("""
        SELECT
          u.user_id AS id,
          CONCAT(COALESCE(u.real_name, u.username), '(', u.username, ')') AS name
        FROM tb_user u
        LEFT JOIN tb_role r ON r.role_id = u.role_id
        WHERE u.status = 1
          AND (r.role_code = 'WAREHOUSE' OR r.role_code IS NULL)
        ORDER BY u.create_time DESC
        """)
    List<SimpleOption> selectAllocatorOptions();
}
