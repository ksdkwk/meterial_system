package com.example.springbootbackeng.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.SimpleOption;
import com.example.springbootbackeng.entity.TransportRecord;
import com.example.springbootbackeng.entity.TransportRecordVO;
import com.example.springbootbackeng.entity.Vehicle;

@Mapper
public interface TransportRecordMapper extends BaseMapper<TransportRecord> {

    @Select("""
            SELECT
                t.record_id,
                t.plan_id,
                p.demand_id,
                m.material_name,
                t.vehicle_id,
                v.plate_number,
                t.driver_name,
                t.driver_phone,
                t.depart_time,
                t.expect_arrive_time,
                t.actual_arrive_time,
                t.transport_status,
                t.current_lat,
                t.current_lng,
                t.last_update_time,
                t.create_time
            FROM tb_transport_record t
            LEFT JOIN tb_allocation_plan p ON p.plan_id = t.plan_id
            LEFT JOIN tb_material_demand d ON d.demand_id = p.demand_id
            LEFT JOIN tb_material m ON m.material_id = d.material_id
            LEFT JOIN tb_vehicle v ON v.vehicle_id = t.vehicle_id
            WHERE (#{transportStatus} IS NULL OR t.transport_status = #{transportStatus})
              AND (
                #{keyword} IS NULL OR #{keyword} = ''
                OR t.record_id LIKE CONCAT('%', #{keyword}, '%')
                OR t.plan_id LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(t.driver_name, '') LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(v.plate_number, '') LIKE CONCAT('%', #{keyword}, '%')
              )
            ORDER BY t.create_time DESC
            """)
    List<TransportRecordVO> selectAllWithDetails(@Param("transportStatus") Integer transportStatus,
                                                 @Param("keyword") String keyword);

    @Select("""
        SELECT
          p.plan_id AS id,
          CONCAT(p.plan_id, ' / ', COALESCE(d.demand_id, '-'), ' / ', COALESCE(m.material_name, '-')) AS name
        FROM tb_allocation_plan p
        LEFT JOIN tb_transport_record t ON t.plan_id = p.plan_id
        LEFT JOIN tb_material_demand d ON d.demand_id = p.demand_id
        LEFT JOIN tb_material m ON m.material_id = d.material_id
        WHERE t.record_id IS NULL
        ORDER BY p.create_time DESC
        """)
    List<SimpleOption> selectAvailablePlanOptions();

    @Select("""
        SELECT
          vehicle_id,
          plate_number,
          vehicle_type,
          driver_name,
          driver_phone,
          status,
          create_time
        FROM tb_vehicle
        WHERE status = 1
        ORDER BY create_time DESC
        """)
    List<Vehicle> selectVehicleOptions();
}
