package ru.mtuci.coursemanagement.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds) {}
