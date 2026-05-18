package com.example.springbootbackeng.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.OperationLog;
import com.example.springbootbackeng.entity.OperationLogVO;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("""
            <script>
            SELECT
                l.log_id,
                l.user_id,
                u.real_name AS user_name,
                l.operation_module,
                l.operation_type,
                l.operation_desc,
                l.request_ip,
                l.operate_time
            FROM tb_operation_log l
            LEFT JOIN tb_user u ON u.user_id = l.user_id
            WHERE 1 = 1
              <if test='operationType != null and operationType != ""'>
                AND UPPER(COALESCE(l.operation_type, '')) = UPPER(#{operationType})
              </if>
              <if test='keyword != null and keyword != ""'>
                AND (
                     l.log_id LIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(l.user_id, '') LIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(u.real_name, '') LIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(l.operation_module, '') LIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(l.operation_type, '') LIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(l.operation_desc, '') LIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(l.request_ip, '') LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY l.operate_time DESC
            </script>
            """)
        List<OperationLogVO> selectAllWithDetails(@Param("keyword") String keyword,
                              @Param("operationType") String operationType);
}
