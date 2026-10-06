# KT4 Kotlin

Итоговый backend-проект на Kotlin и Ktor. В одном приложении объединены REST API, CRUD, JWT-аутентификация, роли пользователей, PostgreSQL, Swagger/OpenAPI, WebSocket-уведомления, централизованная обработка ошибок, логирование и Docker-деплой.

## Стек

- Kotlin 2.4.20
- Ktor 3.6.0
- PostgreSQL
- JDBC
- HikariCP
- JWT
- BCrypt
- WebSocket
- Swagger UI
- OpenAPI 3.0
- Docker
- Docker Compose
- JUnit/Ktor Test Host
- H2 для интеграционных тестов

## Архитектура

```text
HTTP / WebSocket
       |
       v
    Routes
       |
       v
   Services
       |
       v
 Repositories
       |
       v
 PostgreSQL
```

Основные модули:

```text
src/main/kotlin/com/example
├── Application.kt
├── config
│   └── AppConfig.kt
├── database
│   └── DatabaseFactory.kt
├── dto
│   ├── AuthDtos.kt
│   ├── BookDtos.kt
│   └── NotificationDtos.kt
├── error
│   └── ApiExceptions.kt
├── model
│   ├── Book.kt
│   └── User.kt
├── plugins
│   ├── Monitoring.kt
│   ├── Security.kt
│   ├── Serialization.kt
│   ├── StatusPages.kt
│   └── WebSockets.kt
├── realtime
│   └── NotificationHub.kt
├── repository
│   ├── BookRepository.kt
│   └── UserRepository.kt
├── routes
│   ├── AdminRoutes.kt
│   ├── AuthRoutes.kt
│   ├── BookRoutes.kt
│   ├── HealthRoutes.kt
│   ├── NotificationRoutes.kt
│   └── Routing.kt
└── service
    ├── AdminService.kt
    ├── AuthService.kt
    └── BookService.kt
```

## Роли

В системе две роли:

- `user`
- `admin`

Регистрация через `POST /auth/register` всегда создаёт пользователя с ролью `user`.

Администратор создаётся при запуске приложения из переменных окружения `ADMIN_LOGIN` и `ADMIN_PASSWORD`.

По умолчанию для локального запуска:

```text
login: admin
password: admin123
```

Для реального развёртывания эти значения необходимо изменить.

## Разграничение доступа

| Метод | Маршрут | Доступ |
| --- | --- | --- |
| GET | /health | публичный |
| POST | /auth/register | публичный |
| POST | /auth/login | публичный |
| GET | /auth/me | user, admin |
| GET | /books | публичный |
| GET | /books/{id} | публичный |
| POST | /books | user, admin |
| PUT | /books/{id} | user, admin |
| DELETE | /books/{id} | только admin |
| GET | /admin/users | только admin |
| WS | /ws/notifications | авторизованный пользователь |

## База данных

Приложение использует PostgreSQL.

При старте автоматически создаются таблицы:

- `users`
- `books`

Данные сохраняются между перезапусками контейнера в Docker volume.

## Переменные окружения

| Переменная | Значение по умолчанию |
| --- | --- |
| DB_URL | jdbc:postgresql://localhost:5432/kt4 |
| DB_USER | kt4 |
| DB_PASSWORD | kt4 |
| DB_DRIVER | org.postgresql.Driver |
| JWT_SECRET | local-development-secret-change-me |
| JWT_ISSUER | kt4-kotlin |
| JWT_AUDIENCE | kt4-users |
| JWT_REALM | KT4 API |
| ADMIN_LOGIN | admin |
| ADMIN_PASSWORD | admin123 |

## Запуск через Docker Compose

Для полного запуска приложения вместе с PostgreSQL достаточно:

```bash
docker compose up --build
```

После запуска:

```text
API: http://localhost:8080
Swagger UI: http://localhost:8080/swagger
OpenAPI: http://localhost:8080/openapi
Health: http://localhost:8080/health
```

Остановка:

```bash
docker compose down
```

Остановка с удалением данных PostgreSQL:

```bash
docker compose down -v
```

## Локальный запуск без Docker

Нужны JDK 21, Gradle и запущенный PostgreSQL.

Создать БД и пользователя со значениями из переменных окружения, затем:

```bash
gradle run
```

## Тесты

Для тестов PostgreSQL не требуется. Используется H2 в режиме совместимости с PostgreSQL.

Запуск:

```bash
gradle test
```

Тесты проверяют:

- запуск всех модулей в одном приложении;
- Swagger и OpenAPI;
- регистрацию и JWT-вход;
- `/auth/me`;
- создание и изменение книги обычным пользователем;
- запрет удаления книги для роли `user`;
- доступ роли `admin` к списку пользователей;
- удаление книги администратором;
- централизованные ответы 400 и 404;
- WebSocket-подключение;
- получение WebSocket-события после создания книги.

## Swagger и OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger
```

OpenAPI:

```text
http://localhost:8080/openapi
```

Документация содержит все REST-маршруты, требования к JWT, ограничения по ролям и WebSocket-эндпоинт.

## Регистрация

```bash
curl -i -X POST http://localhost:8080/auth/register   -H "Content-Type: application/json"   -d '{"login":"john","password":"secret123"}'
```

## Вход

```bash
curl -i -X POST http://localhost:8080/auth/login   -H "Content-Type: application/json"   -d '{"login":"john","password":"secret123"}'
```

Ответ содержит JWT:

```json
{
  "token": "JWT_TOKEN",
  "expiresInSeconds": 3600,
  "role": "user"
}
```

## Создание книги

```bash
curl -i -X POST http://localhost:8080/books   -H "Content-Type: application/json"   -H "Authorization: Bearer JWT_TOKEN"   -d '{"title":"Clean Code","author":"Robert C. Martin","year":2008}'
```

## Обновление книги

```bash
curl -i -X PUT http://localhost:8080/books/1   -H "Content-Type: application/json"   -H "Authorization: Bearer JWT_TOKEN"   -d '{"title":"Clean Code","author":"Robert C. Martin","year":2009}'
```

## Удаление книги администратором

Сначала войти под администратором и получить его JWT, затем:

```bash
curl -i -X DELETE http://localhost:8080/books/1   -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

## Список пользователей

```bash
curl -i http://localhost:8080/admin/users   -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

## WebSocket

WebSocket принимает JWT через query-параметр `token`:

```text
ws://localhost:8080/ws/notifications?token=JWT_TOKEN
```

Пример подключения через `wscat`:

```bash
wscat -c "ws://localhost:8080/ws/notifications?token=JWT_TOKEN"
```

При создании книги приходит событие:

```json
{
  "type": "created",
  "book": {
    "id": 1,
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "year": 2008
  }
}
```

Для изменения приходит `updated`, для удаления — `deleted`.

## Деплой

На сервере с Docker:

```bash
git clone https://github.com/Zuika1166/kt4_kotlin.git
cd kt4_kotlin
docker compose up -d --build
```

Перед публичным деплоем изменить как минимум:

```text
JWT_SECRET
ADMIN_LOGIN
ADMIN_PASSWORD
POSTGRES_PASSWORD
DB_PASSWORD
```

При необходимости порт приложения меняется в `docker-compose.yml`.

## Проверка после деплоя

```bash
curl -i http://localhost:8080/health
```

Ожидаемый ответ:

```json
{
  "status": "ok"
}
```
