# Система управления офисными местами — КР 1

Консольная информационная система (Java + JDBC + PostgreSQL) для учёта сотрудников,
рабочих мест и бронирований рабочих мест.

Первая из четырёх контрольных работ. Предметная область и вариант далее не меняются:
**КР 1 — консоль → КР 2 — JavaFX → КР 3 — Spring + REST API → КР 4 — Android.**

- **Вариант:** система управления офисными местами
- **Основная сущность:** бронирование рабочего места
- **Команда:** 4 человека, один общий проект
- **Базовый пакет:** `ru.mirea.officebooking`

## Документация

| Документ | О чём |
|---|---|
| [docs/01-domain-model.md](docs/01-domain-model.md) | Сущности, перечисления, диаграмма классов, зафиксированные решения |
| [docs/02-business-rules.md](docs/02-business-rules.md) | 8 бизнес-правил: формулировка, слой, исключение, как проверять |
| [docs/03-architecture.md](docs/03-architecture.md) | Слои, пакеты, интерфейсы, полиморфизм, карта требований ООП |
| [docs/04-database.md](docs/04-database.md) | Три таблицы, ограничения, ER-диаграмма, начальные данные |
| [docs/05-menu-and-features.md](docs/05-menu-and-features.md) | Меню, поиск, фильтрация, сортировка, статистика, экспорт |
| [docs/06-defense-prep.md](docs/06-defense-prep.md) | Вопросы защиты и где в коде ответ |
| [docs/07-migrations.md](docs/07-migrations.md) | Flyway с КР 1: раскладка, именование, запуск, что в какой миграции |

## Зафиксированные решения

1. **Гранулярность брони — часы.** Бронь — полуоткрытый интервал `[начало; конец)`
   с датой и временем. Смежные интервалы (конец одной = начало другой) не считаются
   пересечением.
2. **Полиморфизм — абстрактный `Workspace` с подтипами** `OpenDesk` / `MeetingRoom` /
   `PhoneBooth`. Подтип задаёт свои ограничения на бронь (предел длительности,
   нужна ли вместимость).

## Структура проекта (ориентир)

```
office-space-booking/
├── pom.xml
├── sql/
│   └── queries/                    отладочные SELECT'ы (не схема)
├── src/main/java/ru/mirea/officebooking/
│   ├── Main.java                   собирает граф объектов, прогоняет Flyway, запускает меню
│   ├── ui/                         см. docs/03
│   ├── model/
│   ├── repository/
│   ├── service/
│   ├── exception/
│   └── util/
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/               Flyway — схема и данные БД (см. docs/07)
│       ├── V1__create_schema.sql   ← пишете по docs/04
│       └── V2__seed_data.sql       ← пишете по docs/04
└── exports/                        ← сюда пишутся Excel-файлы
```

## Запуск

Требования: JDK 21 и Docker Desktop (для PostgreSQL). Maven ставить не нужно — есть `mvnw`.

### Вариант A — всё в Docker

```
cp .env.example .env          # один раз
docker compose up -d db       # поднять PostgreSQL
docker compose run --rm app   # собрать и запустить консольное приложение
```

Файлы экспорта из контейнера появляются в папке `exports/` проекта.

Остановить БД: `docker compose down` (данные сохраняются в томе `db-data`;
полностью снести — `docker compose down -v`).

### Вариант B — приложение локально, БД в Docker

```
cp .env.example .env
docker compose up -d db
./mvnw clean package
java -jar target/office-booking.jar
```

Параметры подключения по умолчанию лежат в `src/main/resources/application.properties`
(`localhost:5434`, БД `office_booking`, пользователь `office`). Их можно переопределить
переменными окружения `DB_URL` / `DB_USER` / `DB_PASSWORD` — они важнее файла.
Для разработки удобно поднять БД в Docker и запускать `Main` из IDE.

Схема и демо-данные создаются автоматически при старте приложения (Flyway,
`src/main/resources/db/migration/`). Вручную: `./mvnw flyway:migrate`.

Если нужен другой порт БД, поменяйте `DB_PORT` в `.env` и `db.url` в `application.properties`.

Если консоль показывает русский текст «кракозябрами» (Windows), запускайте так:
`java -Dstdout.encoding=UTF-8 -jar target/office-booking.jar`.

### Тесты

```
./mvnw test
```

Тесты не требуют базы данных: валидация моделей, бизнес-правила `BookingService` (на подставных
репозиториях и фиксированных часах), перевод ошибок БД в понятные исключения.

## Что сдаём (чек-лист защиты)

- [x] Исходный код Java-проекта
- [x] `pom.xml`
- [x] SQL-скрипт создания БД (Flyway-миграции `src/main/resources/db/migration/`)
- [ ] ER-диаграмма (PNG — реверс из DataGrip / IntelliJ IDEA: правой кнопкой по схеме `public` → Diagrams → Show Visualization → Export)
- [x] Экспортированный Excel-файл (`exports/`)
- [x] Инструкция по запуску
- [x] Рабочее консольное приложение
