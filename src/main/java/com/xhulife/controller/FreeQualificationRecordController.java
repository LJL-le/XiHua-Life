package com.xhulife.controller;

import com.xhulife.dto.Result;
import com.xhulife.service.IFreeQualificationRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/free-qualification/records")
public class FreeQualificationRecordController {

    @Resource
    private IFreeQualificationRecordService recordService;

    @GetMapping("/me")
    public Result queryMyRecords(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return recordService.queryMyRecords(current);
    }

    @GetMapping("/{id}")
    public Result queryMyRecord(@PathVariable Long id) {
        return recordService.queryMyRecord(id);
    }
}
