package com.skyways.notification.service;

import com.skyways.notification.dto.BookingConfirmationDto;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String renderConfirmationTemplate(BookingConfirmationDto dto) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>SkyWays Booking Confirmation</title></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;padding:30px;">
                <h1 style="color:#003366;">SkyWays Airlines</h1>
                <h2 style="color:#28a745;">✈ Booking Confirmed!</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your booking is confirmed. Here are your flight details:</p>
                <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                  <tr><td style="padding:8px;background:#f8f9fa;"><strong>Booking Ref</strong></td>
                      <td style="padding:8px;">%s</td></tr>
                  <tr><td style="padding:8px;background:#f8f9fa;"><strong>Flight</strong></td>
                      <td style="padding:8px;">%s</td></tr>
                  <tr><td style="padding:8px;background:#f8f9fa;"><strong>Route</strong></td>
                      <td style="padding:8px;">%s → %s</td></tr>
                  <tr><td style="padding:8px;background:#f8f9fa;"><strong>Departure</strong></td>
                      <td style="padding:8px;">%s</td></tr>
                  <tr><td style="padding:8px;background:#f8f9fa;"><strong>Arrival</strong></td>
                      <td style="padding:8px;">%s</td></tr>
                  <tr><td style="padding:8px;background:#f8f9fa;"><strong>Total Paid</strong></td>
                      <td style="padding:8px;"><strong>%s %s</strong></td></tr>
                </table>
                <p style="color:#6c757d;font-size:12px;">
                  Please carry a valid photo ID matching your passport. Safe travels!<br/>
                  SkyWays Airlines Customer Support: support@skyways.com
                </p>
              </div>
            </body>
            </html>
            """.formatted(
                dto.getPassengerName(),
                dto.getBookingRef(),
                dto.getFlightNumber(),
                dto.getOriginCity(), dto.getDestinationCity(),
                dto.getDepartureTime(),
                dto.getArrivalTime(),
                dto.getCurrency(), dto.getTotalAmount()
            );
    }

    public String renderCancellationTemplate(BookingConfirmationDto dto) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;padding:30px;">
                <h1 style="color:#003366;">SkyWays Airlines</h1>
                <h2 style="color:#dc3545;">Booking Cancelled</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your booking <strong>%s</strong> has been cancelled.</p>
                <p>If a payment was made, a refund will be processed within 5-7 business days.</p>
                <p>Contact support@skyways.com for assistance.</p>
              </div>
            </body>
            </html>
            """.formatted(dto.getPassengerName(), dto.getBookingRef());
    }
}
