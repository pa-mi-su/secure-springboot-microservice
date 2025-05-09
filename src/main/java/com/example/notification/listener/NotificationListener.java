package com.example.notification.listener;

import com.example.notification.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.json.JSONObject;

@Component
public class NotificationListener {

    @Autowired
    private EmailService emailService;

    @KafkaListener(topics = "user.registered", groupId = "notification-group")
    public void listen(String message) {
        JSONObject json = new JSONObject(message);
        String email = json.getString("email");
        String username = json.getString("username");
        emailService.sendWelcomeEmail(email, username);
    }
}
