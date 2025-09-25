package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.SystemActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemActivationRepository extends JpaRepository<SystemActivation, Long> {
    // 查询当前激活状态（系统应只有一条有效记录）
    Optional<SystemActivation> findTopByIsActiveTrue();

    // 通过秘钥查询
    Optional<SystemActivation> findBySecretKey(String secretKey);

    // 新增：按设备ID和激活状态查询（查询当前设备的有效激活记录）
    Optional<SystemActivation> findByDeviceIdAndIsActive(String deviceId, boolean isActive);
}
