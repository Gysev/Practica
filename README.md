# Practica — серверная часть (Spring Boot)

Репозиторий для практики: аутентификация JWT (access + refresh), роли и правила доступа, подключение к PostgreSQL, HTTPS (TLS-профиль), CI на GitHub Actions.

Upstream: https://github.com/Gysev/Practica

## Что уже сделано относительно задания 1.1

1. **Аутентификация** — заголовок `Authorization: Bearer <JWT access>`; эндпоинты `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh` возвращают пару токенов; refresh сохраняется в БД в виде SHA-256 хэша от случайной строки.
2. **Авторизация** — роли `STUDENT`, `TEACHER`, `ADMIN` (Spring: `ROLE_*`). Пример: `POST /api/xml/**` доступен `TEACHER` и `ADMIN`; `/api/proxy` — только `ADMIN`.
3. **Шифрование трафика (HTTPS)** — профиль Spring `ssl` + переменные `SSL_*` и `HTTPS` через reverse-proxy (nginx, Caddy) поверх приложения см. ниже.
4. **PostgreSQL** — профиль `postgres`; для локальной разработки по умолчанию `dev` + H2 in-memory и консоль `/h2-console`.
5. **Секреты** — см. файл `env.example`; в проде использовать GitHub Secrets / переменные CI и окружение хостинга.

## Задание 1.2 — модуль лицензий

- Таблицы `devices` и `software_licenses`, связь с пользователем — см. ER и DDL в [`docs/er-licenses.md`](docs/er-licenses.md).
- REST API (все методы ниже `/api/licenses/...`, кроме отмеченного, требуют JWT; см. ограничения по ролям):

| HTTP | Путь | Доступ | Назначение |
|------|------|--------|-------------|
| `GET` | `/api/licenses/signing-public-key.pem` | без авторизации | SPKI публичного ключа (PEM) |
| `GET` | `/api/licenses/signing-certificate.pem` | без авторизации | сертификат X.509 ключа подписи (PEM) |
| `POST` | `/api/licenses` | **ADMIN** | создание `{ "userId", "validityPeriodDays" }` → ключ + `CREATED` |
| `POST` | `/api/licenses/{licenseKey}/activate` | владелец или ADMIN | `{ "deviceExternalId" }` → `ACTIVE`, срок действия |
| `POST` | `/api/licenses/verify` | владелец или ADMIN | `{ "licenseKey", "deviceExternalId" }` → `Ticket` + подпись |
| `POST` | `/api/licenses/{licenseKey}/renew` | владелец или ADMIN | `{ "additionalDays" }`: для `ACTIVE` продлевается `validUntil`, для `CREATED` — `validityPeriodDays` |

- **`Ticket`**: текущее время сервера, TTL тикета (сек), даты активации и истечения лицензии, `userId`, `deviceId`, флаг блокировки.
- **`TicketResponse`**: объект `ticket` + **`electronicSignatureBase64`** — ЭЦП `SHA256withRSA` над телом билета; тело после **RFC 8785 JCS** (канонический JSON как UTF‑8 байты).

Переменные `LICENSE_*` — в `env.example`, префикс в `application.yaml` — `license.*`.

## Задание 1.3 — модуль ЭЦП (RFC 8785 / JCS + PKCS#12)

- Канонизация JSON билета перед подписью: **RFC 8785** ([JCS](https://www.rfc-editor.org/rfc/rfc8785)), библиотека `org.erdtman.jcs.JsonCanonicalizer` — см. [`Rfc8785TicketCanon`](src/main/java/ru/mtuci/coursemanagement/eds/jcs/Rfc8785TicketCanon.java).
- Подпись и проверка: `java.security.Signature` **SHA256withRSA** над UTF-8 октетами JCS; интеграция с лицензиями — [`TicketSignatureService`](src/main/java/ru/mtuci/coursemanagement/license/service/TicketSignatureService.java).
- Хранилище: **PKCS#12** с закрытым ключом и **X.509** (загрузка в [`EdsSigningBeans`](src/main/java/ru/mtuci/coursemanagement/eds/config/EdsSigningBeans.java)); в dev — RSA 2048 + самоподписанный сертификат (BouncyCastle).
- Публичные эндпоинты (без JWT): `GET /api/licenses/signing-public-key.pem`, `GET /api/licenses/signing-certificate.pem`.
- **CI / GitHub Secrets**: пошагово в [`docs/eds-github-secrets.md`](docs/eds-github-secrets.md); workflow декодирует опциональный secret `LICENSE_KEYSTORE_B64`.

## Задание 1.4 — модуль антивирусных сигнатур

- Таблицы и связи см. ER и DDL в [`docs/er-antivirus.md`](docs/er-antivirus.md).
- ЭЦП: тот же ключ и `EdsDetachedSigner`, что для лицензий (`license.signing.*` / PKCS#12); полезная нагрузка для подписи — канонический JSON (JCS RFC 8785) с ключами **`content`, `name`, `signatureId`, `version`** (см. [`AntivirusEdsPayload`](src/main/java/ru/mtuci/coursemanagement/antivirus/crypto/AntivirusEdsPayload.java)).
- REST под префиксом `/api/antivirus-signatures` (JWT обязателен; создание / изменение / удаление — **TEACHER** и **ADMIN**):

| HTTP | Путь | Назначение |
|------|------|-------------|
| `POST` | `/api/antivirus-signatures` | Создание `{ "name", "content" }` → сохранение, пересчёт ЭЦП, History+Audit (**CREATE**) |
| `GET` | `/api/antivirus-signatures/{id}` | Чтение сущности по id (в т.ч. с `deleted: true`) |
| `PUT` | `/api/antivirus-signatures/{id}` | Обновление `{ "name", "content" }` → `version++`, пересчёт ЭЦП, History (**UPDATE**) + Audit |
| `DELETE` | `/api/antivirus-signatures/{id}` | Логическое удаление (**без** пересчёта ЭЦП), History (**DELETE**) + Audit |
| `GET` | `/api/antivirus-signatures/export/full` | Полная выгрузка активных строк (`deleted = false`) |
| `GET` | `/api/antivirus-signatures/export/incremental?since=<ISO-8601>` | Инкремент: все строки с `updatedAt > since`, **включая** удалённые |
| `GET` | `/api/antivirus-signatures/{id}/history` | История изменений по идентификатору сигнатуры |
| `GET` | `/api/antivirus-signatures/{id}/audit` | Журнал аудита по идентификатору сигнатуры |

## CI/CD

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml): этапы **test** (`mvn verify`) и сборка артефакта (jar после `package`).

## Запуск

```bash
./mvnw spring-boot:run
```

Демо-учётки (если БД пустая и `app.seed-demo-users=true`, как в профиле `dev`):

| Логин   | Пароль   | Роль    |
|---------|----------|---------|
| teacher | password | TEACHER |
| student | password | STUDENT |
| admin   | password | ADMIN   |

PostgreSQL:

```bash
export SPRING_PROFILES_ACTIVE=postgres
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/practica
export SPRING_DATASOURCE_USERNAME=...
export SPRING_DATASOURCE_PASSWORD=...
export JWT_SECRET="$(openssl rand -base64 48)"
```

HTTPS встроенным Tomcat (`ssl` активен вместе с другими профилями, например `postgres,ssl`):

```bash
export SSL_KEYSTORE_PATH=file:/absolute/path/to/keystore.p12
export SSL_KEYSTORE_PASSWORD=...
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=postgres,ssl
```

На проде обычно проще терминировать TLS на reverse-proxy и проксировать на HTTP приложения.

## Публикация на GitHub

Репозиторий с GitHub пока пустой — после локальной проверки:

```bash
git init
git remote add origin https://github.com/Gysev/Practica.git
git branch -M main
git add .
git commit -m "feat: базовый сервер с JWT, ролями, Postgres и CI"
git push -u origin main
```

## Теория UML и ER

Краткая шпаргалка для отчёта:

- **UML**: *use case* (сценарии и актёры), *class/object* (структура и связи), *sequence* (вызовы во времени), *activity/state* (процессы и состояния), *component/deployment* (модули и узлы).
- **ER-модель**: сущности, атрибуты, ключи; связи 1:1, 1:N, M:N через ассоциативную сущность; кардинальность и ограничения целостности.

Подробнее: [UML Specification](https://www.omg.org/spec/UML/) и учебники по концептуальному/логическому проектированию БД.
