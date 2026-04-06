# Task Manager

REST-сервис для управления задачами на Kotlin + Spring Boot + Reactor.

## Стек

- **Kotlin** + **Spring Boot 3.3**
- **Spring WebFlux** — реактивный HTTP-слой
- **Project Reactor** — `Mono`/`Flux` в сервисном слое
- **Spring JDBC `JdbcClient`** — native SQL без ORM
- **PostgreSQL 16** — основная БД
- **Liquibase** — миграции схемы
- **H2** — in-memory БД для тестов

---

## Требования

| Инструмент | Версия |
|------------|--------|
| JDK        | 21     |
| Docker     | 24+    |
| Docker Compose | v2 (`docker compose`) |

> Gradle Wrapper включён в репозиторий — отдельно устанавливать Gradle не нужно.

---

## Быстрый старт

### 1. Только база данных (для локальной разработки)

Поднять PostgreSQL в Docker:

```bash
docker compose up db -d
```

Запустить приложение локально:

```bash
./gradlew bootRun
```

Приложение стартует на `http://localhost:8080`.

---

### 2. Всё в Docker (приложение + БД)

Собрать JAR и запустить все сервисы:

```bash
./gradlew bootJar
docker compose up --build
```

Остановить:

```bash
docker compose down
```

Остановить и удалить данные БД:

```bash
docker compose down -v
```

---

## Тесты

Запустить все тесты (используют H2, Docker не нужен):

```bash
./gradlew test
```

Отчёт после прогона: `build/reports/tests/test/index.html`

### Что покрыто

| Слой       | Тип теста          | Инструменты                        |
|------------|--------------------|------------------------------------|
| Service    | Unit               | MockK, StepVerifier                |
| Controller | Unit (slice)       | `@WebFluxTest`, SpringMockK, WebTestClient |
| Repository | Integration        | `@JdbcTest`, H2 in-memory          |

---

## API

Base URL: `http://localhost:8080/api/tasks`

### Создать задачу

```
POST /api/tasks
Content-Type: application/json

{
  "title": "Prepare report",
  "description": "Monthly financial report"
}
```

Ответ `201 Created`:

```json
{
  "id": 1,
  "title": "Prepare report",
  "description": "Monthly financial report",
  "status": "NEW",
  "createdAt": "2026-03-26T12:00:00",
  "updatedAt": "2026-03-26T12:00:00"
}
```

---

### Получить список задач

```
GET /api/tasks?page=0&size=10
GET /api/tasks?page=0&size=10&status=NEW
```

Параметры:

| Параметр | Обязательный | Описание                              |
|----------|:------------:|---------------------------------------|
| `page`   | да           | Номер страницы, начиная с 0           |
| `size`   | да           | Размер страницы                       |
| `status` | нет          | Фильтр: `NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED` |

Ответ `200 OK`:

```json
{
  "content": [ { ... } ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### Получить задачу по ID

```
GET /api/tasks/{id}
```

Ответ `200 OK` — объект задачи.  
Ответ `404 Not Found` — если задача не найдена.

---

### Обновить статус задачи

```
PATCH /api/tasks/{id}/status
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

Допустимые значения статуса: `NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED`.

Ответ `200 OK` — обновлённый объект задачи.

---

### Удалить задачу

```
DELETE /api/tasks/{id}
```

Ответ `204 No Content`.

---

## Переменные окружения

| Переменная    | Значение по умолчанию | Описание            |
|---------------|-----------------------|---------------------|
| `DB_HOST`     | `localhost`           | Хост PostgreSQL     |
| `DB_PORT`     | `5432`                | Порт PostgreSQL     |
| `DB_NAME`     | `taskdb`              | Имя базы данных     |
| `DB_USER`     | `taskuser`            | Пользователь БД     |
| `DB_PASSWORD` | `taskpassword`        | Пароль              |

---

## Структура проекта

```
src/main/kotlin/com/taskmanager/
├── TaskManagerApplication.kt
├── controller/     # HTTP-слой, маппинг запросов
├── service/        # Бизнес-логика, Mono/Flux
├── repository/     # JdbcClient + native SQL
├── model/          # Task, TaskStatus
├── dto/            # CreateTaskRequest, TaskResponse, PageResponse, …
└── exception/      # TaskNotFoundException, GlobalExceptionHandler

src/main/resources/
├── application.yml
└── db/changelog/
    └── db.changelog-master.xml
```

---

## Быстрая проверка через curl

```bash
# Создать задачу
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy groceries","description":"Milk, eggs"}' | jq

# Получить список
curl -s "http://localhost:8080/api/tasks?page=0&size=10" | jq

# Обновить статус
curl -s -X PATCH http://localhost:8080/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"DONE"}' | jq

# Удалить
curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8080/api/tasks/1
```
