package org.example.pingpongsystem.dto;

import lombok.Data;

@Data
public class SchoolDTO {
    private Long id;
    private String schoolname; // 对应SchoolEntity中的学校名称字段
    private Long adminId;

    public SchoolDTO(Long id, String schoolname,Long adminId) {
        this.id = id;
        this.schoolname = schoolname;
        this.adminId = adminId;
    }
}