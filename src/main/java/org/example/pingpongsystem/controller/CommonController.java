package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.dto.SchoolDTO;
import org.example.pingpongsystem.service.CommonService;
import org.example.pingpongsystem.service.SuperAdminService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/common")
@RestController
public class CommonController {

    private final CommonService commonService;

    public CommonController(CommonService commonService) {
        this.commonService = commonService;
    }

    /**
     * 获取所有学校的ID和名称，用于前端下拉选择
     */
    @GetMapping("/school_options")
    public Result<List<SchoolDTO>> getSchoolOptions() {
        return commonService.getSchoolIdAndNames();
    }
}