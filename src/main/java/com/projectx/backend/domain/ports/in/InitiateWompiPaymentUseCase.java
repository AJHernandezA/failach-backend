package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.PaymentInitData;

public interface InitiateWompiPaymentUseCase {
    PaymentInitData execute(String tenantId, String orderCode, String redirectUrl);
}
