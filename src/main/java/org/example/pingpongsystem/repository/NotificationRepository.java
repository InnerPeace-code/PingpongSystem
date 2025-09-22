package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    // 查询用户的未读通知
    List<NotificationEntity> findByUserIdAndUserTypeAndIsReadFalse(
            Long userId, NotificationEntity.UserType userType);

    // 按类型查询通知
    List<NotificationEntity> findByUserIdAndUserTypeAndType(
            Long userId, NotificationEntity.UserType userType, NotificationEntity.NotificationType type);

    // 标记通知为已读
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.id = :notificationId")
    void markAsRead(Long notificationId);
}