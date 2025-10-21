package com.minishop.notificationservice.repository;

import com.minishop.notificationservice.entity.NotificationLog;
import com.minishop.notificationservice.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetryCount);

    @Query("SELECT n FROM NotificationLog n WHERE " +
           "(:userId IS NULL OR n.userId = :userId) AND " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:referenceId IS NULL OR n.referenceId = :referenceId) " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationLog> findWithFilters(
            @Param("userId") UUID userId,
            @Param("status") NotificationStatus status,
            @Param("referenceId") String referenceId,
            Pageable pageable
    );
}
