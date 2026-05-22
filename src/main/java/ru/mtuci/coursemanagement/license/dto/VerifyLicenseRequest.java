package ru.mtuci.coursemanagement.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyLicenseRequest(
        @NotBlank String licenseKey,
        @NotBlank @Size(max = 256) String deviceExternalId
) {
}
