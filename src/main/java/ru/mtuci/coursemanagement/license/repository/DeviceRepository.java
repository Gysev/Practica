package ru.mtuci.coursemanagement.license.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.coursemanagement.license.model.Device;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByExternalDeviceIdAndOwner_Id(String externalDeviceId, Long ownerUserId);
}
