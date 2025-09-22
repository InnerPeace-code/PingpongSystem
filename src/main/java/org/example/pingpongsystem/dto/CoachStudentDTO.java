package org.example.pingpongsystem.dto;

import lombok.Data;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.StudentEntity;

@Data
public class CoachStudentDTO {
    private CoachTeachStudentEntity relation; // 原关联记录
    private StudentEntity student; // 关联的学生信息
}
