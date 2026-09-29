package com.condos.billing.service;

import com.condos.billing.model.ChargeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/** Recordatorios de pago por correo (RN-PAG-06): saldo pendiente de una unidad. */
@Service
@RequiredArgsConstructor
public class PaymentReminderService {

    private final AccountStatementService statements;
    private final BoardApiClient boardApi;
    private final UserApiClient userApi;
    private final MailgunService mail;

    public void sendReminder(String unitId, String bearerToken) {
        var unit = boardApi.getUnit(unitId, bearerToken);
        if (unit == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad no encontrada");
        }
        if (unit.residentUserId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta unidad todavía no tiene un condómino con acceso vinculado.");
        }

        var user = userApi.getUser(unit.residentUserId(), bearerToken);
        if (user == null || user.email() == null || user.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se encontró un correo registrado para el condómino de esta unidad.");
        }

        var st = statements.forUnit(unitId);
        if (st.balance() == null || st.balance().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta unidad no tiene saldo pendiente.");
        }

        String subject = "Recordatorio de pago · Unidad " + unit.identifier();
        StringBuilder body = new StringBuilder();
        body.append("Hola");
        if (user.fullName() != null && !user.fullName().isBlank()) body.append(", ").append(user.fullName());
        body.append(",\n\n");
        body.append("Te recordamos que la unidad ").append(unit.identifier())
                .append(" tiene un saldo pendiente de ").append(st.balance()).append(".\n\n");
        body.append("Cargos pendientes:\n");
        st.charges().stream()
                .filter(c -> c.status() == ChargeStatus.PENDING
                        || c.status() == ChargeStatus.PARTIALLY_PAID
                        || c.status() == ChargeStatus.OVERDUE)
                .forEach(c -> {
                    BigDecimal paid = c.paidAmount() != null ? c.paidAmount() : BigDecimal.ZERO;
                    body.append("- ").append(c.concept())
                            .append(" (vence ").append(c.dueDate()).append("): ")
                            .append(c.amount().subtract(paid))
                            .append(" pendiente\n");
                });
        body.append("\nSi ya realizaste tu pago, por favor ignora este mensaje.\n");

        mail.send(user.email(), subject, body.toString());
    }
}
