package ru.mtuci.coursemanagement.license.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mtuci.coursemanagement.model.AppUser;

@Entity
@Table(name = "devices",
        uniqueConstraints = @UniqueConstraint(name = "ux_devices_owner_external",
                columnNames = {"owner_user_id", "external_device_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Идентификатор клиентского устройства (hardware / installation id). */
    @Column(name = "external_device_id", nullable = false, length = 256)
    private String externalDeviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id")
    private AppUser owner;

    public Device(String externalDeviceId, AppUser owner) {
        this.externalDeviceId = externalDeviceId;
        this.owner = owner;
    }
}
