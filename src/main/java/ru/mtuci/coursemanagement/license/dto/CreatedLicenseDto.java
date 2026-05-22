package ru.mtuci.coursemanagement.license.dto;

public record CreatedLicenseDto(String licenseKey, String status, int validityPeriodDays) {
}
