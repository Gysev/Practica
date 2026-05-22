package ru.mtuci.coursemanagement.antivirus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "antivirus_signature_history", indexes = {
        @Index(name = "idx_av_hist_sig", columnList = "signatureId")
})
@Getter
@Setter
@NoArgsConstructor
public class AntivirusSignatureHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long signatureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SignatureChangeType changeType;

    @Lob
    private String beforeJson;

    @Lob
    private String afterJson;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 128)
    private String performedBy;

    public AntivirusSignatureHistory(
            Long signatureId,
            SignatureChangeType changeType,
            String beforeJson,
            String afterJson,
            String performedBy) {
        this.signatureId = signatureId;
        this.changeType = changeType;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.createdAt = Instant.now();
        this.performedBy = performedBy;
    }
}
