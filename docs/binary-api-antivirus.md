# Бинарный API выгрузки антивирусных сигнатур (задание 1.5)

Документ фиксирует протокол, реализованный в коде сервера. Если в методичке указан другой формат строк/полей, адаптируйте под требование — основа (LE, multipart, ЭЦП) останется тем же.

## Общее

| Эндпоинт | Ответ |
|----------|-------|
| `GET /api/antivirus-signatures/export/binary/full` | `multipart/mixed`, две части |
| `GET /api/antivirus-signatures/export/binary/incremental?since=ISO-8601` | То же правило набора строк, что и JSON `/export/incremental` |

Оба маршрута требуют JWT; правила доступа совпадают с JSON-экспортом.

### Заголовок HTTP

Пример:

```http
Content-Type: multipart/mixed; boundary=avboundary_<uuid>
```

Тело сообщения содержит **две части** `application/octet-stream`:

1. `manifest.bin` — подписанный бинарный манифест.
2. `signature-payload.bin` — упорядоченный каталог записей (`full`: только `deleted=false`, порядок `id`; `incremental`: `updated_at > since`, включая удалённые, порядок `updated_at`).

Разделитель в теле образуется строкой CRLF-допустимых байт (см. `MultipartMixedWriter`).

---

## Часть 1 — манифест (`manifest.bin`)

Манифест = **неизменный блок для подписи** (ровно **40 байт**, little-endian) + **длина ЭЦП** (`uint32` LE, количество байт PKCS#1) + **сама ЭЦП** (байты PKCS#1).

### Поля блока перед ЭЦП (40 байт, все целые — little-endian «по‑Intel»)

| Смещение | Поле | Тип | Описание |
|---------:|------|-----|----------|
| 0 | `magic` | 4 байта ASCII | всегда `AVBM` |
| 4 | `format_version` | `uint16` | текущее значение в коде: `1` |
| 6 | `export_kind` | `uint8` | `1` — полная выдача активных, `2` — инкремент |
| 7 | `reserved` | `uint8` | `0` |
| 8 | `generated_epoch_ms` | `int64` | момент формирования ответа (UTC как эпоха в мс) |
| 16 | `since_exclusive_ms` | `int64` | для инкремента — тот же `since`, что в query (`updatedAt > since`), для полной выдачи `0` |
| 24 | `record_count` | `uint32` | число записей в части payload |
| 28 | `payload_size` | `int64` | длина `signature-payload.bin` в байтах |
| 36 | `payload_crc32` | `uint32` | контрольная сумма CRC32 (IEEE polynomial) всего файла части 2 без знака |

Подписание: **`SHA256withRSA`** PKCS#1 (отсоединённая подпись) над первыми **40** байтами манифеста. То же ключевое материало, что и для лицензий/JCS-сигнатур (`EdsDetachedSigner.signBytesPkcs1`).

---

## Часть 2 — payload (`signature-payload.bin`)

Каждая запись кодируется **последовательным** набором полей (little-endian):

| Поле | Тип | Примечание |
|------|-----|-------------|
| `signature_id` | `int64` | первичный ключ |
| `content_version` | `int32` | поле `version` из БД |
| `flags` | `uint16` | младший бит: `deleted` |
| `updated_at_epoch_ms` | `int64` | время `updated_at` |
| имя UTF-8 | `uint32` длина + байты UTF-8 | |
| содержание UTF-8 | `uint32` длина + байты UTF-8 | |
| ЭЦП записи ASCII | `uint32` длина + байты US‑ASCII | Base64-строка из поля БД без декодирования |

Записи конкатыруются без выравнивающего паддинга.

---

## Инструментальные классы сервера

| Класс | Назначение |
|-------|-------------|
| `LeSink` | запись little-endian последовательностей и длина-префикс UTF-8/ASCII |
| `AntivirusExportBinaryProtocol` | magic, версия формата, константы `export_kind` |
| `AntivirusPayloadCodec` | сериализация списка сущностей + CRC32 всего блока payload |
| `AntivirusManifestCodec` | сборка подписанного манифеста |
| `MultipartMixedWriter` | заголовки и границы `multipart/mixed` |
| `AntivirusBinaryExportService` | бизнес-оркестрация + выбор набора строк из БД |
| `EdsDetachedSigner.signBytesPkcs1` / `verifyBytesPkcs1` | ЭЦП бинарного манифеста |
