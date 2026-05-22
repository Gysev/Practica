# Practica — серверная часть (Spring Boot)

Репозиторий для практики: аутентификация JWT (access + refresh), роли и правила доступа, подключение к PostgreSQL, HTTPS (TLS-профиль), CI на GitHub Actions.

Upstream: https://github.com/Gysev/Practica

## Что уже сделано относительно задания 1.1

1. **Аутентификация** — заголовок `Authorization: Bearer <JWT access>`; эндпоинты `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh` возвращают пару токенов; refresh сохраняется в БД в виде SHA-256 хэша от случайной строки.
2. **Авторизация** — роли `STUDENT`, `TEACHER`, `ADMIN` (Spring: `ROLE_*`). Пример: `POST /api/xml/**` доступен `TEACHER` и `ADMIN`; `/api/proxy` — только `ADMIN`.
3. **Шифрование трафика (HTTPS)** — профиль Spring `ssl` + переменные `SSL_*` и `HTTPS` через reverse-proxy (nginx, Caddy) поверх приложения см. ниже.
4. **PostgreSQL** — профиль `postgres`; для локальной разработки по умолчанию `dev` + H2 in-memory и консоль `/h2-console`.
5. **Секреты** — см. файл `env.example`; в проде использовать GitHub Secrets / переменные CI и окружение хостинга.

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
