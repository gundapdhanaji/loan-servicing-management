package com.loanservicing.notification.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Local "email": prints the message to the console instead of sending it. */
@Slf4j
@Component
public class LogNotificationSender implements NotificationSender {

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("""

                ====== EMAIL (not really sent) ======
                To:      {}
                Subject: {}
                {}
                =====================================""", toEmail, subject, body);
    }
}
