package com.skyways.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundResult {
    private String refundId;
    private String status;
}
