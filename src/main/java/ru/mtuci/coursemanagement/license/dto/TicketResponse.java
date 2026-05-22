package ru.mtuci.coursemanagement.license.dto;

/** Ответ верификации: билет + ЭЦП (RSA-SHA256) по каноническому представлению полей билета. */
public record TicketResponse(Ticket ticket, String electronicSignatureBase64) {
}
