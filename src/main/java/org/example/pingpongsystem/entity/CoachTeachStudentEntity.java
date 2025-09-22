package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Entity
@Data
public class CoachTeachStudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "教练ID不能为空")
    @Positive(message = "教练ID必须为正数")
    private Long coachId;

    @NotNull(message = "学员ID不能为空")
    @Positive(message = "学员ID必须为正数")
    private Long studentId;

    private boolean isConfirmed;

    @Version
    private Integer version;
}
