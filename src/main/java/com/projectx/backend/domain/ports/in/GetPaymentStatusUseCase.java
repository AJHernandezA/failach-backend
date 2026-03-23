package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.PaymentStatusResponse;

public interface GetPaymentStatusUseCase {
    PaymentStatusResponse execute(String tenantId, String orderCode);
}
