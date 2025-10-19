package com.minishop.notificationservice.repository;

import com.minishop.notificationservice.entity.NotificationChannel;
import com.minishop.notificationservice.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByCodeAndChannel(String code, NotificationChannel channel);

    Optional<NotificationTemplate> findByCode(String code);
}
