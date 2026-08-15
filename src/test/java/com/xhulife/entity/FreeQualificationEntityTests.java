package com.xhulife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FreeQualificationEntityTests {

    @Test
    void activityUsesFreeQualificationActivityTable() {
        TableName tableName = FreeQualificationActivity.class.getAnnotation(TableName.class);

        assertThat(tableName.value()).isEqualTo("tb_free_qualification_activity");
    }

    @Test
    void recordUsesFreeQualificationRecordTable() {
        TableName tableName = FreeQualificationRecord.class.getAnnotation(TableName.class);

        assertThat(tableName.value()).isEqualTo("tb_free_qualification_record");
    }
}
