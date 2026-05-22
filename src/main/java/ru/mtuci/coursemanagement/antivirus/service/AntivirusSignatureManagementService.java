package ru.mtuci.coursemanagement.antivirus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mtuci.coursemanagement.antivirus.crypto.AntivirusEdsPayload;
import ru.mtuci.coursemanagement.antivirus.dto.AntivirusSignatureAuditDto;
import ru.mtuci.coursemanagement.antivirus.dto.AntivirusSignatureDto;
import ru.mtuci.coursemanagement.antivirus.dto.AntivirusSignatureExportRow;
import ru.mtuci.coursemanagement.antivirus.dto.AntivirusSignatureHistoryDto;
import ru.mtuci.coursemanagement.antivirus.dto.CreateAntivirusSignatureRequest;
import ru.mtuci.coursemanagement.antivirus.dto.SnapshotDto;
import ru.mtuci.coursemanagement.antivirus.dto.UpdateAntivirusSignatureRequest;
import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignature;
import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignatureAudit;
import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignatureHistory;
import ru.mtuci.coursemanagement.antivirus.model.SignatureChangeType;
import ru.mtuci.coursemanagement.antivirus.repository.AntivirusSignatureAuditRepository;
import ru.mtuci.coursemanagement.antivirus.repository.AntivirusSignatureHistoryRepository;
import ru.mtuci.coursemanagement.antivirus.repository.AntivirusSignatureRepository;
import ru.mtuci.coursemanagement.eds.EdsDetachedSigner;
import ru.mtuci.coursemanagement.eds.jcs.EdsJsonCanon;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AntivirusSignatureManagementService {

    private static final String UNSIGNED_PLACEHOLDER = "";

    private final AntivirusSignatureRepository signatureRepository;
    private final AntivirusSignatureHistoryRepository historyRepository;
    private final AntivirusSignatureAuditRepository auditRepository;
    private final EdsDetachedSigner edsDetachedSigner;

    private final ObjectMapper json = EdsJsonCanon.mapper();

    private static String performer(Authentication auth) {
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }

    private String snapshotJson(AntivirusSignature s) {
        try {
            return json.writeValueAsString(SnapshotDto.fromEntity(s));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сериализовать снимок");
        }
    }

    private void recalcAndVerifyEds(AntivirusSignature s) {
        try {
            byte[] payload = AntivirusEdsPayload.canonicalUtf8(s);
            s.setEdsSignature(edsDetachedSigner.signRawPayloadUtf8(payload));
            if (!edsDetachedSigner.verifyRawPayloadUtf8(payload, s.getEdsSignature())) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка ЭЦП");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка вычисления ЭЦП", e);
        }
    }

    @Transactional
    public AntivirusSignatureDto create(CreateAntivirusSignatureRequest req, Authentication auth) {
        if (signatureRepository.existsByName(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сигнатура с таким именем уже есть");
        }
        AntivirusSignature s = new AntivirusSignature();
        s.setName(req.name());
        s.setContent(req.content());
        s.setVersion(1);
        s.setEdsSignature(UNSIGNED_PLACEHOLDER);

        signatureRepository.saveAndFlush(s);
        recalcAndVerifyEds(s);
        signatureRepository.save(s);

        String afterJson = snapshotJson(s);
        historyRepository.save(new AntivirusSignatureHistory(
                s.getId(),
                SignatureChangeType.CREATE,
                null,
                afterJson,
                performer(auth)));
        auditRepository.save(new AntivirusSignatureAudit(
                s.getId(),
                SignatureChangeType.CREATE,
                "Создана антивирусная сигнатура: " + s.getName(),
                performer(auth)));
        return AntivirusSignatureDto.fromEntity(s);
    }

    @Transactional(readOnly = true)
    public AntivirusSignatureDto getById(Long id) {
        return AntivirusSignatureDto.fromEntity(signatureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сигнатура не найдена")));
    }

    @Transactional
    public AntivirusSignatureDto update(Long id, UpdateAntivirusSignatureRequest req, Authentication auth) {
        AntivirusSignature s = signatureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сигнатура не найдена"));
        if (s.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сигнатура удалена и недоступна для изменений");
        }
        if (!s.getName().equals(req.name()) && signatureRepository.existsByNameAndIdNot(req.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Имя занято другой сигнатурой");
        }

        String beforeJson = snapshotJson(s);
        s.setName(req.name());
        s.setContent(req.content());
        s.setVersion(s.getVersion() + 1);
        signatureRepository.saveAndFlush(s);
        recalcAndVerifyEds(s);
        signatureRepository.save(s);

        String afterJson = snapshotJson(s);
        historyRepository.save(new AntivirusSignatureHistory(
                s.getId(),
                SignatureChangeType.UPDATE,
                beforeJson,
                afterJson,
                performer(auth)));
        auditRepository.save(new AntivirusSignatureAudit(
                s.getId(),
                SignatureChangeType.UPDATE,
                "Обновлены поля антивирусной сигнатуры (версия " + s.getVersion() + ")",
                performer(auth)));
        return AntivirusSignatureDto.fromEntity(s);
    }

    @Transactional
    public AntivirusSignatureDto softDelete(Long id, Authentication auth) {
        AntivirusSignature s = signatureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сигнатура не найдена"));
        if (s.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сигнатура уже помечена как удалённая");
        }

        String beforeJson = snapshotJson(s);
        s.setDeleted(true);
        s.setDeletedAt(Instant.now());
        signatureRepository.save(s);

        String afterJson = snapshotJson(s);
        historyRepository.save(new AntivirusSignatureHistory(
                s.getId(),
                SignatureChangeType.DELETE,
                beforeJson,
                afterJson,
                performer(auth)));
        auditRepository.save(new AntivirusSignatureAudit(
                s.getId(),
                SignatureChangeType.DELETE,
                "Логическое удаление антивирусной сигнатуры",
                performer(auth)));
        return AntivirusSignatureDto.fromEntity(s);
    }

    @Transactional(readOnly = true)
    public List<AntivirusSignatureExportRow> exportFull() {
        return signatureRepository.findAllByDeletedFalseOrderByIdAsc().stream()
                .map(AntivirusSignatureExportRow::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AntivirusSignatureExportRow> exportIncremental(Instant sinceExclusive) {
        return signatureRepository.findAllByUpdatedAtAfterOrderByUpdatedAtAsc(sinceExclusive).stream()
                .map(AntivirusSignatureExportRow::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AntivirusSignatureHistoryDto> history(Long signatureId) {
        ensureSignatureRowExists(signatureId);
        return historyRepository.findAllBySignatureIdOrderByCreatedAtAsc(signatureId).stream()
                .map(AntivirusSignatureHistoryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AntivirusSignatureAuditDto> auditLog(Long signatureId) {
        ensureSignatureRowExists(signatureId);
        return auditRepository.findAllBySignatureIdOrderByCreatedAtAsc(signatureId).stream()
                .map(AntivirusSignatureAuditDto::from)
                .toList();
    }

    private void ensureSignatureRowExists(Long signatureId) {
        if (!signatureRepository.existsById(signatureId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сигнатура не найдена");
        }
    }
}
