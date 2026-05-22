package ru.mtuci.coursemanagement.license.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mtuci.coursemanagement.model.AppUser;

import java.time.Instant;

@Entity
@Table(name = "software_licenses")
@Getter
@Setter
@NoArgsConstructor
public class SoftwareLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Публичный ключ лицензии для URL и API (UUID строкой). */
    @Column(name = "license_key", nullable = false, unique = true, length = 64)
    private String licenseKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LicenseStatus status;

    private Instant createdAt;

    /** Дата успешной активации привязки к устройству. */
    private Instant activatedAt;

    /** Срок действия после активации. */
    private Instant validUntil;

    /**
     * Период (дней), на который выпущена лицензия при активации или наращивается при создании без продления активной.
     */
    @Column(nullable = false)
    private int validityPeriodDays;

    @Column(nullable = false)
    private boolean blocked;

    public SoftwareLicense(String licenseKey, AppUser user, int validityPeriodDays) {
        this.licenseKey = licenseKey;
        this.user = user;
        this.validityPeriodDays = validityPeriodDays;
        this.status = LicenseStatus.CREATED;
        this.createdAt = Instant.now();
        this.blocked = false;
    }

    /** Проверка срока; не меняет статус автоматически в БД. */
    public boolean isExpiredNow(Instant now) {
        return validUntil != null && now.isAfter(validUntil);
    }
}
