package org.example.pingpongsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.TableEntity;
import org.example.pingpongsystem.service.TableService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/table")
@RestController
@RequiredArgsConstructor
public class TableController {
    private final TableService tableService;

    /**
     * 获取所有球台列表
     */
    @GetMapping("/all")
    public Result<List<TableEntity>> getAllTables() {
        return tableService.getAllTables();
    }

    @GetMapping("/by-school")
    public Result<List<TableEntity>> getTablesBySchoolId(@RequestParam Long schoolId) {
        return tableService.getTablesBySchoolId(schoolId);
    }
}
