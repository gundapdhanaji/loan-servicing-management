package com.loanservicing.notification.internal;

/**
 * How messages are sent. Locally they are only written to the console log.
 * In production you would add an implementation that sends real email/SMS (e.g. via AWS SES).
 */
public interface NotificationSender {

    void send(String toEmail, String subject, String body);
}
