package com.example.springbootbackeng.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.springbootbackeng.entity.ReviewLogVO;

@Mapper
public interface ReviewLogMapper {

    @Select("""
            SELECT
                review_id,
                review_demand_id,
                review_apply_id,
                review_apply_name,
                review_approve_id,
                review_approve_name,
                review_state,
                review_time,
                review_result,
                review_reason,
                review_other
            FROM tb_review_log
            WHERE (
                #{keyword} IS NULL OR #{keyword} = ''
                OR review_id LIKE CONCAT('%', #{keyword}, '%')
                OR review_demand_id LIKE CONCAT('%', #{keyword}, '%')
                OR review_apply_id LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(review_apply_name, '') LIKE CONCAT('%', #{keyword}, '%')
                OR review_approve_id LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(review_approve_name, '') LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(review_state, '') LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(review_result, '') LIKE CONCAT('%', #{keyword}, '%')
            )
            AND (
                #{reviewState} IS NULL OR #{reviewState} = ''
                OR review_state = #{reviewState}
            )
            AND (
                #{reviewResult} IS NULL OR #{reviewResult} = ''
                OR review_result = #{reviewResult}
            )
            ORDER BY review_time DESC
            """)
    List<ReviewLogVO> selectAll(@Param("keyword") String keyword,
                                @Param("reviewState") String reviewState,
                                @Param("reviewResult") String reviewResult);

    @Update("""
            UPDATE tb_review_log
            SET
                review_approve_id = #{approveId},
                review_approve_name = #{approveName},
                review_state = #{reviewState},
                review_time = NOW(),
                review_result = #{reviewResult},
                review_reason = #{reviewReason},
                review_other = #{reviewOther}
            WHERE review_demand_id = #{demandId}
            """)
    int updateAuditByDemandId(@Param("demandId") String demandId,
                              @Param("approveId") String approveId,
                              @Param("approveName") String approveName,
                              @Param("reviewState") String reviewState,
                              @Param("reviewResult") String reviewResult,
                              @Param("reviewReason") String reviewReason,
                              @Param("reviewOther") String reviewOther);

    @Insert("""
            INSERT INTO tb_review_log (
                review_demand_id,
                review_apply_id,
                review_apply_name,
                review_approve_id,
                review_approve_name,
                review_state,
                review_time,
                review_result,
                review_reason,
                review_other
            )
            SELECT
                d.demand_id,
                d.demand_user_id,
                COALESCE(app.real_name, d.demand_user_id),
                #{approveId},
                #{approveName},
                #{reviewState},
                NOW(),
                #{reviewResult},
                #{reviewReason},
                #{reviewOther}
            FROM tb_material_demand d
            LEFT JOIN tb_user app ON app.user_id = d.demand_user_id
            WHERE d.demand_id = #{demandId}
            LIMIT 1
            """)
    int insertAuditRecordByDemandId(@Param("demandId") String demandId,
                                    @Param("approveId") String approveId,
                                    @Param("approveName") String approveName,
                                    @Param("reviewState") String reviewState,
                                    @Param("reviewResult") String reviewResult,
                                    @Param("reviewReason") String reviewReason,
                                    @Param("reviewOther") String reviewOther);
}
