package ru.mtuci.coursemanagement.antivirus.storage.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mtuci.coursemanagement.antivirus.storage.config.SignatureMinioProperties;
import ru.mtuci.coursemanagement.antivirus.storage.dto.PresignedUrlItem;
import ru.mtuci.coursemanagement.antivirus.storage.model.AntivirusSignatureFile;
import ru.mtuci.coursemanagement.antivirus.storage.repository.AntivirusSignatureFileRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AntivirusSignatureFileServiceTest {

    @Mock
    MinioClient minioClient;

    @Mock
    AntivirusSignatureFileRepository repository;

    @Mock
    SignatureMinioProperties props;

    @InjectMocks
    AntivirusSignatureFileService service;

    @Test
    void presignedGetUrlsKeepsOrderUsesBucketAndObjectKeyAndTtlFromProps() throws Exception {
        when(props.getPresignTtl()).thenReturn(Duration.ofMinutes(30));

        AntivirusSignatureFile f11 = signatureRow(11L, "buck", "dir/a.sig");
        AntivirusSignatureFile f22 = signatureRow(22L, "buck", "dir/b.sig");
        when(repository.findById(11L)).thenReturn(Optional.of(f11));
        when(repository.findById(22L)).thenReturn(Optional.of(f22));
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://presigned/1", "http://presigned/2");

        List<PresignedUrlItem> items = service.presignedGetUrls(List.of(11L, 22L));

        ArgumentCaptor<GetPresignedObjectUrlArgs> cap = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient, times(2)).getPresignedObjectUrl(cap.capture());

        List<GetPresignedObjectUrlArgs> vals = cap.getAllValues();
        assertThat(vals.get(0).bucket()).isEqualTo("buck");
        assertThat(vals.get(0).object()).isEqualTo("dir/a.sig");
        assertThat(vals.get(1).bucket()).isEqualTo("buck");
        assertThat(vals.get(1).object()).isEqualTo("dir/b.sig");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).id()).isEqualTo(11L);
        assertThat(items.get(1).id()).isEqualTo(22L);
        assertThat(Duration.between(Instant.now(), items.getFirst().expiresAt()))
                .isBetween(Duration.ofMinutes(29), Duration.ofMinutes(31));
    }

    private static AntivirusSignatureFile signatureRow(long id, String bucket, String key) {
        AntivirusSignatureFile f = new AntivirusSignatureFile();
        f.setId(id);
        f.setBucketName(bucket);
        f.setObjectKey(key);
        return f;
    }
}
