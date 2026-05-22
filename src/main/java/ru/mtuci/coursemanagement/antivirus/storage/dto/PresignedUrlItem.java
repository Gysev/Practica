package ru.mtuci.coursemanagement.antivirus.storage.dto;

import java.time.Instant;

public record PresignedUrlItem(long id, String url, Instant expiresAt) {}
