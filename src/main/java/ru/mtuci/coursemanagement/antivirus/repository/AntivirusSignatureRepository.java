package ru.mtuci.coursemanagement.antivirus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignature;

import java.time.Instant;
import java.util.List;

public interface AntivirusSignatureRepository extends JpaRepository<AntivirusSignature, Long> {

    List<AntivirusSignature> findAllByDeletedFalseOrderByIdAsc();

    List<AntivirusSignature> findAllByUpdatedAtAfterOrderByUpdatedAtAsc(Instant since);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByName(String name);
}
