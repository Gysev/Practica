package ru.mtuci.coursemanagement.license.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateLicenseRequest(@NotNull Long userId, @Positive int validityPeriodDays) {
}
