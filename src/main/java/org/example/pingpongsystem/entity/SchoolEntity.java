package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class SchoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String schoolname;

    @NotBlank
    @Column(nullable = false)
    private String address;

    private String name;

    private String phone;

    private String email;

    private Long adminId;

    @Column(nullable = false)
    private int table_num;

    @Version
    private Integer version;
}
