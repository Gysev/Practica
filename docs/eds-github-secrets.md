# ЭЦП и GitHub Secrets / CI

## Локальное хранилище (PKCS#12)

Создание ключа и сертификата для подписи билетов (пример, 10 лет):

```bash
keytool -genkeypair -alias license -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore license-signing.p12 -validity 3650 \
  -dname "CN=Practica License EDS, O=YourOrg" \
  -storepass changeit
```

Файл **не коммитить**. Пароль и путь задать через переменные окружения (см. `env.example`).

## GitHub Actions

В репозитории: **Settings → Secrets and variables → Actions** добавьте:

| Secret | Назначение |
|--------|------------|
| `LICENSE_KEYSTORE_B64` | Содержимое `license-signing.p12` в Base64 (одна строка, без переносов). Пример: `base64 -w0 license-signing.p12` (Linux) или PowerShell `Convert.ToBase64String([IO.File]::ReadAllBytes(...))` |
| `LICENSE_KEYSTORE_PASSWORD` | Пароль от PKCS#12 |

Workflow [`.github/workflows/ci.yml`](../.github/workflows/ci.yml): если задан `LICENSE_KEYSTORE_B64`, перед `mvn verify` создаётся временный `.p12`, выставляются `LICENSE_KEYSTORE_LOCATION`, `LICENSE_SIGN_DEV_KEY=false` и пароль.

Без секретов CI использует **dev-ключ** (`LICENSE_SIGN_DEV_KEY=true` по умолчанию в `application.yaml`).

## Ссылки

- [RFC 8785 — JSON Canonicalization Scheme (JCS)](https://www.rfc-editor.org/rfc/rfc8785)
- [Java Signature](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/security/Signature.html)
- [keytool](https://docs.oracle.com/en/java/javase/21/docs/specs/man/keytool.html)
