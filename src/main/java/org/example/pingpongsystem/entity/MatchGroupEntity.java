package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class MatchGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long monthlyMatchId; // 关联月赛ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchRegistrationEntity.GroupType groupType; // 所属组别

    private Integer subgroupNumber; // 小组编号，多于6人时分小组用

    @Column(nullable = false)
    private Integer size; // 小组人数
}