package com.skyways.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrderResponse {
    private String razorpayOrderId;
    private long   amountInSmallestUnit; // paise for INR (amount × 100)
    private String currency;
    private String keyId;                // public Razorpay key — safe to expose to frontend
}