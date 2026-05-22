package ru.mtuci.coursemanagement.license.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.coursemanagement.license.model.SoftwareLicense;

import java.util.Optional;

public interface SoftwareLicenseRepository extends JpaRepository<SoftwareLicense, Long> {

    Optional<SoftwareLicense> findByLicenseKey(String licenseKey);
}
