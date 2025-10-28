package com.minishop.userservice.event.producer;

import com.minishop.userservice.event.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_USER_REGISTERED = "user.registered";

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing {} for userId: {}, email: {}", TOPIC_USER_REGISTERED, event.getUserId(), event.getEmail());
        kafkaTemplate.send(TOPIC_USER_REGISTERED, event.getUserId().toString(), event);
    }
}
