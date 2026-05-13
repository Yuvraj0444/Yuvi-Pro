package com.skyways.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.skyways.common.exception.payment.PaymentFailedException;
import com.skyways.common.exception.payment.PaymentGatewayUnavailableException;
import com.skyways.common.exception.payment.RefundFailedException;
import com.skyways.common.security.SecretManagerService;
import com.skyways.payment.dto.CreateOrderResponse;
import com.skyways.payment.dto.RefundResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RazorpayService {

    private static final Logger log = LogManager.getLogger(RazorpayService.class);

    private final SecretManagerService secretManagerService;

    public RazorpayService(SecretManagerService secretManagerService) {
        this.secretManagerService = secretManagerService;
    }

    private RazorpayClient client() throws RazorpayException {
        return new RazorpayClient(
            secretManagerService.getSecret("RAZORPAY_KEY_ID"),
            secretManagerService.getSecret("RAZORPAY_KEY_SECRET")
        );
    }

    /**
     * Step 1 of two-step payment flow.
     * Creates a Razorpay Order and returns the order_id + public key for the frontend checkout.
     */
    public CreateOrderResponse createOrder(BigDecimal amount, String currency,
                                           String bookingId, String receipt) {
        try {
            long amountInSmallestUnit = amount.multiply(BigDecimal.valueOf(100)).longValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInSmallestUnit);
            orderRequest.put("currency", currency.toUpperCase());
            orderRequest.put("receipt", receipt);
            orderRequest.put("notes", new JSONObject().put("booking_id", bookingId));

            Order order = client().orders.create(orderRequest);
            String orderId = order.get("id");

            log.info("Razorpay order created [orderId={}, bookingId={}, amount={} {}]",
                orderId, bookingId, amount, currency);

            return CreateOrderResponse.builder()
                .razorpayOrderId(orderId)
                .amountInSmallestUnit(amountInSmallestUnit)
                .currency(currency.toUpperCase())
                .keyId(secretManagerService.getSecret("RAZORPAY_KEY_ID"))
                .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed [bookingId={}]: {}", bookingId, e.getMessage());
            throw new PaymentGatewayUnavailableException(
                "Razorpay order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Step 2 of two-step payment flow.
     * Verifies the HMAC-SHA256 signature sent by Razorpay after the customer pays.
     * Throws PaymentFailedException if the signature is invalid (tampered response).
     */
    public void verifySignature(String razorpayOrderId, String razorpayPaymentId,
                                String razorpaySignature) {
        try {
            String keySecret = secretManagerService.getSecret("RAZORPAY_KEY_SECRET");

            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id",   razorpayOrderId);
            attributes.put("razorpay_payment_id",  razorpayPaymentId);
            attributes.put("razorpay_signature",   razorpaySignature);

            boolean valid = Utils.verifyPaymentSignature(attributes, keySecret);
            if (!valid) {
                throw new PaymentFailedException("Razorpay signature verification failed — possible tampering");
            }

            log.info("Razorpay signature verified [orderId={}, paymentId={}]",
                razorpayOrderId, razorpayPaymentId);

        } catch (RazorpayException e) {
            throw new PaymentFailedException("Signature verification error: " + e.getMessage(), e);
        }
    }

    /**
     * Issues a full or partial refund against a Razorpay payment_id.
     */
    public RefundResult refund(String razorpayPaymentId, BigDecimal amount, String bookingId) {
        try {
            long amountInSmallestUnit = amount.multiply(BigDecimal.valueOf(100)).longValue();

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountInSmallestUnit);
            refundRequest.put("notes", new JSONObject().put("booking_id", bookingId));

            com.razorpay.Refund refund = client().payments.refund(razorpayPaymentId, refundRequest);
            String refundId = refund.get("id");

            log.info("Razorpay refund created [refundId={}, paymentId={}, amount={}]",
                refundId, razorpayPaymentId, amount);

            return RefundResult.builder()
                .refundId(refundId)
                .status(refund.get("status"))
                .build();

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed [paymentId={}]: {}", razorpayPaymentId, e.getMessage());
            throw new RefundFailedException(razorpayPaymentId, e);
        }
    }
}