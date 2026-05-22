package ru.mtuci.coursemanagement.antivirus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "antivirus_signature_audit", indexes = {
        @Index(name = "idx_av_audit_sig", columnList = "signatureId")
})
@Getter
@Setter
@NoArgsConstructor
public class AntivirusSignatureAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long signatureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SignatureChangeType action;

    @Column(nullable = false, length = 1024)
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 128)
    private String performedBy;

    public AntivirusSignatureAudit(
            Long signatureId, SignatureChangeType action, String details, String performedBy) {
        this.signatureId = signatureId;
        this.action = action;
        this.details = details;
        this.createdAt = Instant.now();
        this.performedBy = performedBy;
    }
}
