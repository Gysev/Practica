package ru.mtuci.coursemanagement.antivirus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignatureHistory;

import java.util.List;

public interface AntivirusSignatureHistoryRepository extends JpaRepository<AntivirusSignatureHistory, Long> {

    List<AntivirusSignatureHistory> findAllBySignatureIdOrderByCreatedAtAsc(Long signatureId);
}
