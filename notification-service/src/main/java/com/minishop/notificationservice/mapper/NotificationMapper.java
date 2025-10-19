package com.minishop.notificationservice.mapper;

import com.minishop.notificationservice.dto.response.NotificationLogResponse;
import com.minishop.notificationservice.dto.response.NotificationTemplateResponse;
import com.minishop.notificationservice.entity.NotificationLog;
import com.minishop.notificationservice.entity.NotificationTemplate;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationLogResponse toNotificationLogResponse(NotificationLog log);

    List<NotificationLogResponse> toNotificationLogResponseList(List<NotificationLog> logs);

    NotificationTemplateResponse toNotificationTemplateResponse(NotificationTemplate template);

    List<NotificationTemplateResponse> toNotificationTemplateResponseList(List<NotificationTemplate> templates);
}
