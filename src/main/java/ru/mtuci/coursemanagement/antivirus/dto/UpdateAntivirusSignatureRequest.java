package ru.mtuci.coursemanagement.antivirus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAntivirusSignatureRequest(
        @NotBlank @Size(max = 256) String name,
        @NotBlank String content
) {}
