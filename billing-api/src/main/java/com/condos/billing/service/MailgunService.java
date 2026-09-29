package com.condos.billing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Envío de correo vía Mailgun (recordatorios de pago, RN-PAG-06). */
@Service
public class MailgunService {

    private final RestTemplate rest = new RestTemplate();

    @Value("${mailgun.api-key:}")
    private String apiKey;

    @Value("${mailgun.domain:}")
    private String domain;

    @Value("${mailgun.mail-from:noreply@lokaly.site}")
    private String mailFrom;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && domain != null && !domain.isBlank();
    }

    public void send(String to, String subject, String text) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El envío de correos no está configurado (falta MAILGUN_API_KEY/MAILGUN_DOMAIN)");
        }

        String url = "https://api.mailgun.net/v3/" + domain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("api", apiKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", mailFrom);
        form.add("to", to);
        form.add("subject", subject);
        form.add("text", text);

        try {
            rest.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo enviar el correo: " + e.getMessage());
        }
    }
}
