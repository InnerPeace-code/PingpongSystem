package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private boolean isMale;

    private Integer age;

    @NotNull
    @Column(nullable = false)
    private Long schoolId;

    @NotBlank
    @Column(nullable = false)
    private String phone;

    private String email;

    @Version
    private Integer version;
}
