package com.kaveri.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "whatsapp")
@Getter
@Setter
public class WhatsAppConfigProperties {

    private Api api = new Api();
    private Webhook webhook = new Webhook();
    private Restaurant restaurant = new Restaurant();
    private Mock mock = new Mock();

    @Getter
    @Setter
    public static class Api {
        private String token = "";
        private String phoneNumberId = "";
        private String version = "v20.0";
        private String baseUrl = "https://graph.facebook.com";
    }

    @Getter
    @Setter
    public static class Webhook {
        private String verifyToken = "kaveri_whatsapp_verify_token_est1947";
        private String appSecret = "";
    }

    @Getter
    @Setter
    public static class Restaurant {
        private String phone = "918235520520";
        private String name = "Kaveri Restaurant Kitchen";
    }

    @Getter
    @Setter
    public static class Mock {
        private boolean enabled = true;
    }

    public boolean isConfigured() {
        return !mock.isEnabled()
                && api.getToken() != null && !api.getToken().isBlank()
                && api.getPhoneNumberId() != null && !api.getPhoneNumberId().isBlank();
    }
}

