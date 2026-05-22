package ru.mtuci.coursemanagement.antivirus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "antivirus_signatures", indexes = {
        @Index(name = "idx_antivirus_signatures_updated_at", columnList = "updatedAt")
})
@Getter
@Setter
@NoArgsConstructor
public class AntivirusSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 256)
    private String name;

    @Lob
    @Column(nullable = false)
    private String content;

    /** Монотонно растёт при успешном update. */
    @Column(nullable = false)
    private int version = 1;

    /** ЭЦП (Base64) по каноническому JSON полей подписания. */
    @Column(nullable = false, length = 1024)
    private String edsSignature;

    @Column(nullable = false)
    private boolean deleted = false;

    private Instant deletedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
