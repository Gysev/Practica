package ru.mtuci.coursemanagement.license.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.coursemanagement.license.config.KeyPairHolder;
import ru.mtuci.coursemanagement.license.dto.ActivateLicenseRequest;
import ru.mtuci.coursemanagement.license.dto.CreatedLicenseDto;
import ru.mtuci.coursemanagement.license.dto.CreateLicenseRequest;
import ru.mtuci.coursemanagement.license.dto.LicenseActivationDto;
import ru.mtuci.coursemanagement.license.dto.LicenseRenewalDto;
import ru.mtuci.coursemanagement.license.dto.RenewLicenseRequest;
import ru.mtuci.coursemanagement.license.dto.TicketResponse;
import ru.mtuci.coursemanagement.license.dto.VerifyLicenseRequest;
import ru.mtuci.coursemanagement.license.service.LicenseManagementService;

import java.util.Base64;

@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
@Validated
public class LicenseController {

    private final LicenseManagementService licenseService;
    private final KeyPairHolder signingKeys;

    @GetMapping(value = "/signing-public-key.pem", produces = MediaType.TEXT_PLAIN_VALUE)
    public String signingPublicKeyPem() {
        byte[] der = signingKeys.publicKey().getEncoded();
        String b64 = Base64.getMimeEncoder().encodeToString(der);
        return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----\n";
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CreatedLicenseDto create(@Valid @RequestBody CreateLicenseRequest body) {
        return licenseService.create(body);
    }

    @PostMapping(path = "/{licenseKey}/activate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LicenseActivationDto activate(
            @PathVariable String licenseKey,
            @Valid @RequestBody ActivateLicenseRequest body,
            Authentication authentication) {
        return licenseService.activate(licenseKey, body, authentication);
    }

    @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TicketResponse verify(@Valid @RequestBody VerifyLicenseRequest body, Authentication authentication)
            throws Exception {
        return licenseService.verify(body, authentication);
    }

    @PostMapping(path = "/{licenseKey}/renew", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LicenseRenewalDto renew(
            @PathVariable String licenseKey,
            @Valid @RequestBody RenewLicenseRequest body,
            Authentication authentication) {
        return licenseService.renew(licenseKey, body, authentication);
    }
}
