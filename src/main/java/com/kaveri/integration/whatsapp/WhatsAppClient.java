package com.kaveri.integration.whatsapp;

import com.kaveri.integration.whatsapp.dto.WhatsAppButton;

import java.util.List;

public interface WhatsAppClient {

    /**
     * Sends a plain text message to a WhatsApp recipient.
     * @return provider message id (e.g. wamid)
     */
    String sendTextMessage(String toPhone, String text);

    /**
     * Sends an interactive quick-action buttons message to a WhatsApp recipient.
     * Up to 3 buttons supported by Meta WhatsApp Cloud API.
     * @return provider message id (e.g. wamid)
     */
    String sendInteractiveButtonsMessage(
            String toPhone,
            String headerText,
            String bodyText,
            String footerText,
            List<WhatsAppButton> buttons);
}

