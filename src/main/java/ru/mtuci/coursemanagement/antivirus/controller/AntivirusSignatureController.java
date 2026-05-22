package ru.mtuci.coursemanagement.antivirus.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.mtuci.coursemanagement.antivirus.dto.AntivirusSignatureAuditDto;
import ru.mtuci.coursemanagement.antivirus.dto.AntivirusSignatureDto;
import ru.mtuci.coursemanagement.antivirus.dto.AntivirusSignatureExportRow;
import ru.mtuci.coursemanagement.antivirus.dto.AntivirusSignatureHistoryDto;
import ru.mtuci.coursemanagement.antivirus.dto.CreateAntivirusSignatureRequest;
import ru.mtuci.coursemanagement.antivirus.dto.UpdateAntivirusSignatureRequest;
import ru.mtuci.coursemanagement.antivirus.service.AntivirusBinaryExportService;
import ru.mtuci.coursemanagement.antivirus.service.AntivirusSignatureManagementService;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/antivirus-signatures")
@RequiredArgsConstructor
public class AntivirusSignatureController {

    private final AntivirusSignatureManagementService managementService;
    private final AntivirusBinaryExportService binaryExportService;

    @PostMapping
    public AntivirusSignatureDto create(
            @Valid @RequestBody CreateAntivirusSignatureRequest body, Authentication authentication) {
        return managementService.create(body, authentication);
    }

    @GetMapping("/export/full")
    public List<AntivirusSignatureExportRow> exportFull() {
        return managementService.exportFull();
    }

    /** Инкремент: строки с {@code updatedAt > since}, в том числе с deleted=true. Параметр {@code since} — ISO-8601, например {@code 2026-03-01T12:00:00Z}. */
    @GetMapping("/export/incremental")
    public List<AntivirusSignatureExportRow> exportIncremental(@RequestParam String since) {
        String trimmed = since == null ? "" : since.trim();
        try {
            return managementService.exportIncremental(Instant.parse(trimmed));
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ожидался параметр since в формате ISO-8601 Instant");
        }
    }

    /** Бинарная полная выгрузка активных записей как {@code multipart/mixed} (часть 1 — манифест, часть 2 — payload). */
    @GetMapping("/export/binary/full")
    public ResponseEntity<byte[]> exportBinaryFull() {
        try {
            AntivirusBinaryExportService.BinaryMultipartEnvelope env =
                    binaryExportService.multipartFullExport();
            return multipartResponse(env);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сформировать бинарную выдачу", e);
        }
    }

    @GetMapping("/export/binary/incremental")
    public ResponseEntity<byte[]> exportBinaryIncremental(@RequestParam String since) {
        String trimmed = since == null ? "" : since.trim();
        try {
            AntivirusBinaryExportService.BinaryMultipartEnvelope env = binaryExportService.multipartIncrementalExport(
                    Instant.parse(trimmed));
            return multipartResponse(env);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ожидался параметр since в формате ISO-8601 Instant");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сформировать инкрементальную выдачу", e);
        }
    }

    private static ResponseEntity<byte[]> multipartResponse(AntivirusBinaryExportService.BinaryMultipartEnvelope env) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.MULTIPART_MIXED_VALUE
                + "; boundary="
                + env.boundary());
        return new ResponseEntity<>(env.body(), headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/history")
    public List<AntivirusSignatureHistoryDto> history(@PathVariable Long id) {
        return managementService.history(id);
    }

    @GetMapping("/{id}/audit")
    public List<AntivirusSignatureAuditDto> audit(@PathVariable Long id) {
        return managementService.auditLog(id);
    }

    @GetMapping("/{id}")
    public AntivirusSignatureDto get(@PathVariable Long id) {
        return managementService.getById(id);
    }

    @PutMapping("/{id}")
    public AntivirusSignatureDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAntivirusSignatureRequest body,
            Authentication authentication) {
        return managementService.update(id, body, authentication);
    }

    @DeleteMapping("/{id}")
    public AntivirusSignatureDto softDelete(@PathVariable Long id, Authentication authentication) {
        return managementService.softDelete(id, authentication);
    }
}
