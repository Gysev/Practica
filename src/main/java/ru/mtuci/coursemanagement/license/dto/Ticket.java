package ru.mtuci.coursemanagement.license.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;

/**
 * Ответ клиенту с данными о лицензии на момент проверки.
 *
 * @param serverDate              текущие дата/время на сервере
 * @param ticketLifetimeSeconds    время жизни выданного билета (сек)
 * @param licenseActivationDate   дата активации лицензии
 * @param licenseExpirationDate    дата истечения лицензии
 * @param userId                  идентификатор пользователя
 * @param deviceId               идентификатор устройства (внешний)
 * @param licenseBlocked          признак блокировки лицензии
 */
@JsonPropertyOrder({
        "serverDate", "ticketLifetimeSeconds", "licenseActivationDate", "licenseExpirationDate",
        "userId", "deviceId", "licenseBlocked"
})
public record Ticket(
        Instant serverDate,
        long ticketLifetimeSeconds,
        Instant licenseActivationDate,
        Instant licenseExpirationDate,
        Long userId,
        String deviceId,
        boolean licenseBlocked
) {
}
