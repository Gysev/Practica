package ru.mtuci.coursemanagement.antivirus.storage.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PresignedUrlsRequest(
        @NotNull(message = "Ожидался объект ids") @NotEmpty(message = "Список id не может быть пустым") List<Long> ids) {}
