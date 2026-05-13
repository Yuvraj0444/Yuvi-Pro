package com.skyways.notification.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.skyways.common.exception.notification.NotificationDeliveryException;
import com.skyways.common.security.SecretManagerService;
import com.skyways.notification.dto.BookingConfirmationDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridService {

    private static final Logger log = LogManager.getLogger(SendGridService.class);
    private static final String FROM_EMAIL = "bookings@skyways.com";
    private static final String FROM_NAME  = "SkyWays Airlines";

    private final SecretManagerService secretManagerService;
    private final EmailTemplateService templateService;

    public SendGridService(SecretManagerService secretManagerService,
                            EmailTemplateService templateService) {
        this.secretManagerService = secretManagerService;
        this.templateService = templateService;
    }

    public void sendBookingConfirmation(BookingConfirmationDto dto) {
        String subject = "Your SkyWays Booking " + dto.getBookingRef() + " is Confirmed!";
        String htmlBody = templateService.renderConfirmationTemplate(dto);
        send(dto.getPassengerEmail(), dto.getPassengerName(), subject, htmlBody, dto.getBookingRef());
    }

    public void sendCancellationNotification(BookingConfirmationDto dto) {
        String subject = "Your SkyWays Booking " + dto.getBookingRef() + " has been Cancelled";
        String htmlBody = templateService.renderCancellationTemplate(dto);
        send(dto.getPassengerEmail(), dto.getPassengerName(), subject, htmlBody, dto.getBookingRef());
    }

    private void send(String toEmail, String toName, String subject,
                       String htmlBody, String bookingRef) {
        String apiKey = secretManagerService.getSecret("SENDGRID_API_KEY");
        SendGrid sg   = new SendGrid(apiKey);

        try {
            Email from    = new Email(FROM_EMAIL, FROM_NAME);
            Email to      = new Email(toEmail, toName);
            Content content = new Content("text/html", htmlBody);
            Mail mail     = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                throw new NotificationDeliveryException(
                    "SendGrid returned HTTP " + response.getStatusCode() +
                    " for bookingRef=" + bookingRef + ": " + response.getBody());
            }

            log.info("Email sent [to={}, bookingRef={}, statusCode={}]",
                toEmail, bookingRef, response.getStatusCode());

        } catch (IOException e) {
            throw new NotificationDeliveryException(
                "SendGrid IO error for bookingRef=" + bookingRef + ": " + e.getMessage(), e);
        }
    }
}
