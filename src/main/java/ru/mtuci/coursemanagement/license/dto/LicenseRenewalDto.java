package ru.mtuci.coursemanagement.license.dto;

import ru.mtuci.coursemanagement.license.model.LicenseStatus;

import java.time.Instant;

public record LicenseRenewalDto(
        String licenseKey,
        LicenseStatus status,
        int validityPeriodDays,
        Instant validUntil
) {
}
