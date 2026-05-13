package com.skyways.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChargeResult {
    private String chargeId;
    private String status;
    private boolean paid;
}
