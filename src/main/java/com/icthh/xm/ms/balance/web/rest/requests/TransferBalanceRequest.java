package com.icthh.xm.ms.balance.web.rest.requests;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class TransferBalanceRequest {
    @NotNull
    private Long sourceBalanceId;
    @NotNull
    private Long targetBalanceId;
    @NotNull
    private BigDecimal amount;
}