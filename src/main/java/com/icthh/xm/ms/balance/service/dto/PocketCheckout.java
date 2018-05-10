package com.icthh.xm.ms.balance.service.dto;

import com.icthh.xm.ms.balance.domain.Pocket;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
public class PocketCheckout {

    private final Pocket pocket;
    @Getter
    private final BigDecimal amount;

    public String getLabel() {
        return pocket.getLabel();
    }

    public Instant getStartDateTime() {
        return pocket.getStartDateTime();
    }

    public Instant getEndDateTime() {
        return pocket.getEndDateTime();
    }
}
