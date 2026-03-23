package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.WompiWebhookEvent;

public interface ProcessWompiWebhookUseCase {
    void execute(WompiWebhookEvent event);
}
