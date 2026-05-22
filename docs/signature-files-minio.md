# Дополнительное задание 1.6 — файлы сигнатур и MinIO

При `signature.minio.enabled=true` приложение сохраняет загружаемые файлы в **приватный** бакет MinIO, метаданные и **SHA-256** (hex) — в таблице `antivirus_signature_files`.

## Конфигурация Spring Boot

Через `application.yaml` и переменные окружения (см. `env.example`):

| Переменная | Назначение |
|------------|-------------|
| `SIGNATURE_MINIO_ENABLED` | `true` включает бины MinIO и REST под `/api/admin/...` |
| `MINIO_ENDPOINT` | API MinIO для Java-клиента (например `http://127.0.0.1:9000`) |
| `MINIO_REGION` | Регион для подписей S3 API (часто `us-east-1`) |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | **Сервисные** ключи для приложения, не пара root-администратора |
| `MINIO_BUCKET` | Имя бакета |
| `MINIO_PRESIGN_TTL` | TTL pre-signed URL (Spring `Duration`, например `15m`) |

Если MinIO включён, пустые ключи недопустимы: так вы случайно не подставите root в приложение.

## Docker Compose

Файл [`docker-compose.minio.yml`](../docker-compose.minio.yml) поднимает `minio` и одноразовый `minio-init`, который создаёт бакет и выполняет `mc anonymous set none` (приватный бакет без анонимного чтения).

```bash
docker compose -f docker-compose.minio.yml up -d
```

Веб-консоль по умолчанию: [http://localhost:9001](http://localhost:9001).

## Отдельные access / secret для сервера (не root)

1. Откройте консоль MinIO как `MINIO_ROOT_USER`.
2. **Identity → Service Accounts → Create** и выдайте политику с доступом только к нужному бакету (`s3:PutObject`, `s3:GetObject` и генерация временных GET-ссылок обычно покрывается этими правами для ваших объектов).
3. Полученную пару передайте в `MINIO_ACCESS_KEY` и `MINIO_SECRET_KEY`.

Либо воспользуйтесь вашей версией `mc` (справка: `mc admin accesskey create --help`).

## REST API (роль ADMIN, JWT обязателен)

Префикс: `/api/admin/antivirus-signature-files`.

| Метод | Путь | Запрос |
|-------|------|--------|
| `POST` | `/api/admin/antivirus-signature-files` | `multipart/form-data`, поле **`file`** |
| `POST` | `/api/admin/antivirus-signature-files/presigned-download-urls` | JSON `{"ids":[1,2,3]}` |

Ответ второго метода: список `{ id, url, expiresAt }` — временные GET-URL для скачивания из закрытого бакета.

## Пример с curl

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"password\"}" | jq -r '.access')

curl -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/rules.dat" \
  http://localhost:8080/api/admin/antivirus-signature-files

curl -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"ids\":[1]}" \
  http://localhost:8080/api/admin/antivirus-signature-files/presigned-download-urls
```
