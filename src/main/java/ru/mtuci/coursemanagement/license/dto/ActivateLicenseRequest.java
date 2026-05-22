package ru.mtuci.coursemanagement.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateLicenseRequest(@NotBlank @Size(max = 256) String deviceExternalId) {
}
