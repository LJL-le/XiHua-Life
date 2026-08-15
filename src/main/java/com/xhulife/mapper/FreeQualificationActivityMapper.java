package com.xhulife.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhulife.entity.FreeQualificationActivity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface FreeQualificationActivityMapper extends BaseMapper<FreeQualificationActivity> {

    @Update("UPDATE tb_free_qualification_activity "
            + "SET remaining_quota = remaining_quota - 1 "
            + "WHERE id = #{activityId} AND remaining_quota > 0")
    int decrementQuotaIfAvailable(@Param("activityId") Long activityId);
}
