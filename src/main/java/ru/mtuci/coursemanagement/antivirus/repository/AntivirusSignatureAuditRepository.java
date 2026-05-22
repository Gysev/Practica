package ru.mtuci.coursemanagement.antivirus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignatureAudit;

import java.util.List;

public interface AntivirusSignatureAuditRepository extends JpaRepository<AntivirusSignatureAudit, Long> {

    List<AntivirusSignatureAudit> findAllBySignatureIdOrderByCreatedAtAsc(Long signatureId);
}
