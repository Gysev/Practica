package ru.mtuci.coursemanagement.antivirus.storage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.coursemanagement.antivirus.storage.model.AntivirusSignatureFile;

public interface AntivirusSignatureFileRepository extends JpaRepository<AntivirusSignatureFile, Long> {}
