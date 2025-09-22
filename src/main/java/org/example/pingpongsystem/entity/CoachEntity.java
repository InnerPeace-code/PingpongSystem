package org.example.pingpongsystem.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class CoachEntity {
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

    // 显式指定JSON中的键为"isMale"
    @JsonProperty("isMale")
    private boolean isMale;

    private int age;

    @NotNull
    @Column(nullable = false)
    private Long schoolId;

    @NotBlank
    @Column(nullable = false)
    private String phone;

    private String email;

    @NotBlank
    @Column(nullable = false)
    private String photoPath;

    @NotBlank
    @Column(nullable = false)
    private String description;

    private int level;

    private boolean isCertified;

    private String avatar;

    @Version
    private Integer version;
    // 手动定义setIsMale，覆盖Lombok的默认实现
    public void setIsMale(boolean isMale) {
        this.isMale = isMale;
    }
    public boolean getIsMale() { return this.isMale; }
}
