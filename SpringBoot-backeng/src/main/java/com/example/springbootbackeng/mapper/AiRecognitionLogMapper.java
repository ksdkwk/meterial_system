package com.example.springbootbackeng.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.AiRecognitionLog;
import com.example.springbootbackeng.entity.AiRecognitionLogVO;

@Mapper
public interface AiRecognitionLogMapper extends BaseMapper<AiRecognitionLog> {

    @Select("""
            <script>
            SELECT
                l.rec_id,
                l.user_id,
                u.real_name AS user_name,
                l.original_image_url,
                l.ai_result_json,
                l.recognize_material_name,
                l.recognize_quantity,
                l.confidence_score,
                l.is_verified,
                l.verify_user_id,
                vu.real_name AS verify_user_name,
                l.create_time
            FROM tb_ai_recognition_log l
            LEFT JOIN tb_user u ON u.user_id = l.user_id
            LEFT JOIN tb_user vu ON vu.user_id = l.verify_user_id
            WHERE 1 = 1
              <if test='filterUserId != null and filterUserId != ""'>
                AND l.user_id = #{filterUserId}
              </if>
              <if test='keyword != null and keyword != ""'>
                AND (
                     l.rec_id LIKE CONCAT('%', #{keyword}, '%')
                  OR l.user_id LIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(u.real_name, '') LIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(l.recognize_material_name, '') LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY l.create_time DESC
            </script>
            """)
    List<AiRecognitionLogVO> selectVisibleLogs(@Param("filterUserId") String filterUserId,
                                               @Param("keyword") String keyword);
}
