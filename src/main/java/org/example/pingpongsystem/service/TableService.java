package org.example.pingpongsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.TableEntity;
import org.example.pingpongsystem.repository.TableRepository;
import org.example.pingpongsystem.utility.Result;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TableService {
    private final TableRepository tableRepository;

    /**
     * 获取所有球台（含名称，用于前端ID转名称）
     */
    public Result<List<TableEntity>> getAllTables() {
        return Result.success(tableRepository.findAll());
    }

    public Result<List<TableEntity>> getTablesBySchoolId(Long schoolId) {
        List<TableEntity> tables = tableRepository.findAllBySchoolId(schoolId);
        return Result.success(tables);
    }
}