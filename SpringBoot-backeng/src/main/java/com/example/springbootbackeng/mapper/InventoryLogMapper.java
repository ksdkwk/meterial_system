package com.example.springbootbackeng.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.InventoryLog;
import com.example.springbootbackeng.entity.InventoryLogVO;

@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {

		@Select("""
						SELECT
								l.log_id,
								l.inventory_id,
								l.material_id,
								m.material_name,
								l.change_type,
								l.change_quantity,
								l.before_quantity,
								l.after_quantity,
								l.reason,
								l.log_Change AS log_change,
								l.operator_id,
								u.real_name AS operator_name,
								l.operate_time,
								l.log_status
						FROM tb_inventory_log l
						LEFT JOIN tb_material m ON m.material_id = l.material_id
						LEFT JOIN tb_user u ON u.user_id = l.operator_id
						WHERE l.log_status = 1
							AND (#{changeType} IS NULL OR l.change_type = #{changeType})
							AND (#{action} IS NULL OR #{action} = '' OR l.log_Change = #{action})
							AND (
								#{keyword} IS NULL OR #{keyword} = ''
								OR l.log_id LIKE CONCAT('%', #{keyword}, '%')
								OR l.inventory_id LIKE CONCAT('%', #{keyword}, '%')
								OR l.material_id LIKE CONCAT('%', #{keyword}, '%')
								OR COALESCE(m.material_name, '') LIKE CONCAT('%', #{keyword}, '%')
								OR COALESCE(u.real_name, '') LIKE CONCAT('%', #{keyword}, '%')
								OR l.operator_id LIKE CONCAT('%', #{keyword}, '%')
							)
						ORDER BY l.operate_time DESC
						""")
		List<InventoryLogVO> selectAllWithDetails(@Param("changeType") Integer changeType,
																						 @Param("action") String action,
																						 @Param("keyword") String keyword);
}
