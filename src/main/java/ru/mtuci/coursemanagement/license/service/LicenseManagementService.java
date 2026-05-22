package ru.mtuci.coursemanagement.license.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mtuci.coursemanagement.license.config.LicenseProperties;
import ru.mtuci.coursemanagement.license.dto.ActivateLicenseRequest;
import ru.mtuci.coursemanagement.license.dto.CreatedLicenseDto;
import ru.mtuci.coursemanagement.license.dto.CreateLicenseRequest;
import ru.mtuci.coursemanagement.license.dto.LicenseActivationDto;
import ru.mtuci.coursemanagement.license.dto.LicenseRenewalDto;
import ru.mtuci.coursemanagement.license.dto.RenewLicenseRequest;
import ru.mtuci.coursemanagement.license.dto.Ticket;
import ru.mtuci.coursemanagement.license.dto.TicketResponse;
import ru.mtuci.coursemanagement.license.dto.VerifyLicenseRequest;
import ru.mtuci.coursemanagement.license.model.Device;
import ru.mtuci.coursemanagement.license.model.LicenseStatus;
import ru.mtuci.coursemanagement.license.model.SoftwareLicense;
import ru.mtuci.coursemanagement.license.repository.DeviceRepository;
import ru.mtuci.coursemanagement.license.repository.SoftwareLicenseRepository;
import ru.mtuci.coursemanagement.repository.AppUserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LicenseManagementService {

    private final AppUserRepository appUserRepository;
    private final DeviceRepository deviceRepository;
    private final SoftwareLicenseRepository licenseRepository;
    private final TicketSignatureService ticketSignatureService;
    private final LicenseProperties licenseProperties;

    @Transactional
    public CreatedLicenseDto create(CreateLicenseRequest req) {
        var user = appUserRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        String key = UUID.randomUUID().toString();
        SoftwareLicense lic = new SoftwareLicense(key, user, req.validityPeriodDays());
        licenseRepository.save(lic);
        return new CreatedLicenseDto(key, lic.getStatus().name(), lic.getValidityPeriodDays());
    }

    @Transactional
    public LicenseActivationDto activate(String licenseKey, ActivateLicenseRequest req, Authentication auth) {
        SoftwareLicense lic = licenseRepository.findByLicenseKey(licenseKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Лицензия не найдена"));
        assertOwnerOrAdmin(lic, auth);
        if (lic.getStatus() != LicenseStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Активация возможна только в состоянии CREATED");
        }
        var user = lic.getUser();
        Device device = deviceRepository
                .findByExternalDeviceIdAndOwner_Id(req.deviceExternalId(), user.getId())
                .orElseGet(() -> deviceRepository.save(new Device(req.deviceExternalId(), user)));

        Instant now = Instant.now();
        lic.setDevice(device);
        lic.setActivatedAt(now);
        lic.setValidUntil(now.plus(lic.getValidityPeriodDays(), ChronoUnit.DAYS));
        lic.setStatus(LicenseStatus.ACTIVE);
        licenseRepository.save(lic);
        return new LicenseActivationDto(
                lic.getLicenseKey(), lic.getStatus().name(), device.getExternalDeviceId(), lic.getValidUntil());
    }

    @Transactional
    public TicketResponse verify(VerifyLicenseRequest req, Authentication auth) throws Exception {
        SoftwareLicense lic = licenseRepository.findByLicenseKey(req.licenseKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Лицензия не найдена"));
        assertOwnerOrAdmin(lic, auth);
        if (lic.getStatus() != LicenseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Лицензия не активирована");
        }
        if (lic.getDevice() == null || !req.deviceExternalId().equals(lic.getDevice().getExternalDeviceId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Неверное устройство");
        }
        if (lic.isBlocked()) {
            return signedTicket(buildTicket(Instant.now(), lic));
        }

        Instant now = Instant.now();
        if (lic.isExpiredNow(now)) {
            lic.setStatus(LicenseStatus.EXPIRED);
            licenseRepository.save(lic);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Срок лицензии истёк");
        }

        return signedTicket(buildTicket(now, lic));
    }

    @Transactional
    public LicenseRenewalDto renew(String licenseKey, RenewLicenseRequest req, Authentication auth) {
        SoftwareLicense lic = licenseRepository.findByLicenseKey(licenseKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Лицензия не найдена"));
        assertOwnerOrAdmin(lic, auth);
        if (lic.getStatus() == LicenseStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Лицензия отозвана");
        }
        if (lic.getStatus() == LicenseStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сначала продлите через новую лицензию или уточните правила");
        }
        int addDays = req.additionalDays();
        if (lic.getStatus() == LicenseStatus.ACTIVE) {
            Instant base = lic.getValidUntil();
            Instant newUntil = base.plus(addDays, ChronoUnit.DAYS);
            lic.setValidUntil(newUntil);
            licenseRepository.save(lic);
            return new LicenseRenewalDto(
                    lic.getLicenseKey(), lic.getStatus(), lic.getValidityPeriodDays(), lic.getValidUntil());
        }
        // CREATED: увеличиваем будущий период действия после активации
        lic.setValidityPeriodDays(lic.getValidityPeriodDays() + addDays);
        licenseRepository.save(lic);
        return new LicenseRenewalDto(
                lic.getLicenseKey(), lic.getStatus(), lic.getValidityPeriodDays(), lic.getValidUntil());
    }

    private TicketResponse signedTicket(Ticket ticket) throws Exception {
        String signature = ticketSignatureService.signTicket(ticket);
        return new TicketResponse(ticket, signature);
    }

    private Ticket buildTicket(Instant now, SoftwareLicense lic) {
        long ttlSecs = Math.max(1, licenseProperties.getTicketLifetime().getSeconds());
        var deviceExt = lic.getDevice() != null ? lic.getDevice().getExternalDeviceId() : "";
        return new Ticket(
                now,
                ttlSecs,
                lic.getActivatedAt(),
                lic.getValidUntil(),
                lic.getUser().getId(),
                deviceExt,
                lic.isBlocked()
        );
    }

    private void assertOwnerOrAdmin(SoftwareLicense lic, Authentication auth) {
        if (isAdmin(auth)) {
            return;
        }
        if (auth == null || !lic.getUser().getUsername().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к лицензии");
        }
    }

    private static boolean isAdmin(Authentication auth) {
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
