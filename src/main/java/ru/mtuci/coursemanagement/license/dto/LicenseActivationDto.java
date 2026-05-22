package ru.mtuci.coursemanagement.license.dto;

import java.time.Instant;

public record LicenseActivationDto(String licenseKey, String status, String deviceExternalId, Instant validUntil) {
}
