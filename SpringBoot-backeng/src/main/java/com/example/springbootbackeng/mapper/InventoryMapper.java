package com.example.springbootbackeng.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.Inventory;
import com.example.springbootbackeng.entity.InventoryVO;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Select("""
            SELECT
                i.inventory_id,
                i.material_id,
                m.material_name,
                i.storage_location,
                i.stock_quantity,
                i.last_check_time,
                i.last_checker_id,
                u.real_name AS last_checker_name,
                i.update_time
            FROM tb_inventory i
            LEFT JOIN tb_material m ON m.material_id = i.material_id
            LEFT JOIN tb_user u ON u.user_id = i.last_checker_id
            WHERE (
                #{keyword} IS NULL OR #{keyword} = ''
                OR i.inventory_id LIKE CONCAT('%', #{keyword}, '%')
                OR i.material_id LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(m.material_name, '') LIKE CONCAT('%', #{keyword}, '%')
                OR i.storage_location LIKE CONCAT('%', #{keyword}, '%')
            )
            ORDER BY i.update_time DESC
            """)
    List<InventoryVO> selectAllWithDetails(@Param("keyword") String keyword);

        @Update("""
                        UPDATE tb_inventory
                        SET stock_quantity = stock_quantity - #{quantity}
                        WHERE inventory_id = #{inventoryId}
                            AND stock_quantity >= #{quantity}
                        """)
        int decreaseStockIfEnough(@Param("inventoryId") String inventoryId,
                                                            @Param("quantity") Integer quantity);
}
