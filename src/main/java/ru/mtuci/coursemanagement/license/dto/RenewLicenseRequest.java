package ru.mtuci.coursemanagement.license.dto;

import jakarta.validation.constraints.Positive;

public record RenewLicenseRequest(@Positive int additionalDays) {
}
