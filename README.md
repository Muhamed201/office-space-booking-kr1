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

_Раздел довести до ума перед сдачей (это отдельный артефакт «инструкция по запуску»)._

### Вариант A — Docker (нужен только Docker Desktop)

```
cp .env.example .env          # один раз
docker compose up -d db       # поднять PostgreSQL
docker compose run --rm app   # собрать и запустить консоль
```

Остановить БД: `docker compose down` (данные сохраняются в томе `db-data`;
полностью снести — `docker compose down -v`).

### Вариант B — локально (JDK 21 + Maven + свой PostgreSQL)

- создать пустую БД, задать подключение в `src/main/resources/application.properties`
  или переменными окружения `DB_URL` / `DB_USER` / `DB_PASSWORD`
- миграции Flyway применяются при старте приложения (см. docs/07), вручную — `mvn flyway:migrate`
- сборка: `mvn clean package` → запуск: `java -jar target/office-booking.jar`

> Для разработки удобнее всего: `docker compose up -d db` + запуск `Main` из IDE.

> **Нужно в коде:** `DatabaseManager` (и конфиг Flyway) читают сначала переменные
> окружения `DB_URL` / `DB_USER` / `DB_PASSWORD`, при их отсутствии —
> `application.properties`. Без этого контейнер `app` не найдёт БД по хосту `db`.

## Что сдаём (чек-лист защиты)

- [ ] Исходный код Java-проекта
- [ ] `pom.xml`
- [ ] SQL-скрипт создания БД (Flyway-миграции `src/main/resources/db/migration/`)
- [ ] ER-диаграмма (PNG — dbdiagram.io или реверс из DBeaver / pgAdmin)
- [ ] Экспортированный Excel-файл
- [ ] Инструкция по запуску
- [ ] Рабочее консольное приложение
