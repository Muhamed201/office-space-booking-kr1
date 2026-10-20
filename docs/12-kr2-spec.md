# КР 2 — JavaFX-клиент: детальная спецификация

Ветка: `kr2`. Предметная область, БД и бизнес-правила — из КР 1 (см. `01-domain-model.md`, `02-business-rules.md`).
Подключение к БД: `src/main/resources/database.properties` (`db.url`, `db.user`, `db.password`), переопределяется переменными `DB_URL`, `DB_USER`, `DB_PASSWORD`.

Слово «эндпоинт» из задания здесь означает **публичный метод сервиса**, который вызывает контроллер. REST в КР 2 нет: цепочка такая:

```
FXML (fx:id, onAction) → Controller → Service → Repository (JDBC) → PostgreSQL
```

---

## 0. Что уже есть и что переиспользуем

| Слой | Класс | Что умеет |
|---|---|---|
| model | `Booking`, `Employee`, `Workspace` (+ `OpenDesk`, `MeetingRoom`, `PhoneBooth`), enum'ы `BookingStatus`, `WorkspaceStatus`, `WorkspaceType`, `EmployeeRole`, `Validation` | Валидация в конструкторах, `ValidationException` |
| repository | `BookingRepository`, `EmployeeRepository`, `WorkspaceRepository`, `SqlExceptionTranslator` | CRUD на `PreparedStatement`, поиск/фильтры по одному критерию |
| service | `BookingService`, `EmployeeService`, `WorkspaceService`, `StatisticsService`, `BookingSearchService`, `ExportService` | Бизнес-правила КР 1 |
| util | `DatabaseManager`, `CsvExporter`, `ExcelExporter` | Подключение, экспорт 3 таблиц в `exports/` |
| exception | `AppException` → `ValidationException`, `BusinessRuleException` и др. | Тексты на русском, уже годятся для показа пользователю |
| ui | консольные меню | **Не используются в КР 2**, удалять не обязательно |

Главная сущность КР 2: **Booking (бронь)**. Связанные: **Employee**, **Workspace**. Для них тоже делаем CRUD-вкладки (сервисы готовы).

### Чего не хватает (разрыв между КР 1 и заданием)

1. Таблица должна показывать **ФИО сотрудника и код места**, а не `employee_id`/`workspace_id`. Нужен DTO со связанными данными (JOIN).
2. Поиск и фильтры в КР 1 — отдельные методы по одному критерию. Для UI нужен **один метод, комбинирующий все критерии** + сортировка.
3. Статистика в КР 1 — набор разрозненных методов, печатаемых в консоль. Нужен **один отчёт-объект** с 5+ показателями и периодом.
4. Экспорт в КР 1 пишет 3 файла в `exports/` под своим именем. Нужен экспорт **текущего набора строк таблицы в файл, выбранный через `FileChooser`**.
5. Нет точки сборки зависимостей для JavaFX (аналог `Main`) и нет применения миграций вне `Main`.
6. JDBC-вызовы нельзя выполнять в потоке JavaFX (UI зависнет) — нужен фоновый запуск.
7. Новые тесты (старые удалены).

---

## 1. Архитектура и пакеты

```
ru.mirea.officebooking
├── dto/                  НОВОЕ  BookingRow, BookingFilter, BookingSort, StatisticsReport
├── repository/           + BookingQueryRepository (JOIN-запросы для таблицы) — или методы в BookingRepository
├── service/              + BookingQueryService, StatisticsService.buildReport(...), ExportService.exportRows(...)
├── util/                 + BookingRowExporter, CsvBookingExporter, XlsxBookingExporter, DatabaseMigrator
└── fx/                   НОВОЕ
    ├── OfficeBookingFxApplication.java   (extends Application, start(Stage))
    ├── AppContext.java                   (создаёт repo/service, единая точка сборки)
    ├── controller/       MainController, BookingDialogController, EmployeesController, WorkspacesController, StatisticsController
    └── util/             Alerts, AsyncRunner, ErrorMessages
resources/
├── database.properties
├── fxml/                 main.fxml, booking-dialog.fxml, employees.fxml, workspaces.fxml, statistics.fxml
└── css/                  app.css
```

Правила слоёв (проверяем grep'ом перед защитой):
- `fx/**` не импортирует `java.sql` и `repository`. Контроллер знает только сервисы и DTO.
- SQL только в `repository`. Нет SQL в обработчиках.
- Бизнес-проверки только в `model`/`service`. Контроллер только переводит текст полей в типы (`LocalDate`, `Integer`) и сообщает об ошибке разбора.
- Пароль БД не в коде — только `database.properties`/env.

---

## 2. Backend: что написать (подетально)

### B0. Сборка приложения и миграции

**`util/DatabaseMigrator`** (вынести из `Main`)
```java
public final class DatabaseMigrator {
    public static void migrate(DatabaseManager db);   // Flyway.configure().dataSource(url,user,password).load().migrate()
}
```
- Бросает `DatabaseException("Не удалось применить миграции базы данных", e)` (как сейчас в `Main`).
- `Main` переключить на вызов этого класса (чтобы не дублировать).

**`fx/AppContext`**
```java
public final class AppContext {
    public AppContext();                       // new DatabaseManager(); migrate; создать репозитории и сервисы
    public BookingService bookings();
    public BookingQueryService bookingQueries();
    public EmployeeService employees();
    public WorkspaceService workspaces();
    public StatisticsService statistics();
    public ExportService export();
}
```
Тот же порядок создания, что в `Main` (строки 53–69). Исключение при старте (нет БД) → окно ошибки «Не удалось подключиться к БД: …», приложение закрывается, не падает со stack trace.

### B1. DTO (пакет `dto`, `record`'ы — неизменяемые)

```java
public record BookingRow(
    long id,
    long employeeId, String employeeName, String employeeEmail,
    long workspaceId, String workspaceCode, WorkspaceType workspaceType, int floor,
    LocalDateTime startTime, LocalDateTime endTime,
    BookingStatus status, Integer attendees, String purpose) {
    public Duration duration();               // end - start
}

public record BookingFilter(
    String text,                  // подстрока: ФИО, email, код места, цель; null/blank = не искать
    BookingStatus status,         // null = все
    WorkspaceType workspaceType,  // null = все
    Integer floor,                // null = все
    LocalDate dateFrom,           // null = без границы; включительно
    LocalDate dateTo) {           // null = без границы; включительно
    public static BookingFilter empty();
}

public enum BookingSort { START_TIME, EMPLOYEE_NAME, WORKSPACE_CODE, DURATION, STATUS }   // + boolean ascending передаётся отдельно
```

Валидация в компактном конструкторе `BookingFilter`: если обе даты заданы и `dateFrom > dateTo` → `ValidationException("Дата «с» не может быть позже даты «по»")`.

### B2. Выборка для таблицы — **главный метод экрана**

**Repository:** `BookingQueryRepository` (или добавить в `BookingRepository`)
```java
List<BookingRow> search(BookingFilter filter, BookingSort sort, boolean ascending);
Optional<BookingRow> findRowById(long id);
```
SQL (динамически собирается список условий, значения только через `?`, **никакой конкатенации пользовательского ввода**):
```sql
SELECT b.id, e.id, e.full_name, e.email, w.id, w.code, w.type, w.floor,
       b.start_time, b.end_time, b.status, b.attendees, b.purpose
FROM booking b
JOIN employee  e ON e.id = b.employee_id
JOIN workspace w ON w.id = b.workspace_id
WHERE (lower(e.full_name) LIKE ? OR lower(e.email) LIKE ? OR lower(w.code) LIKE ? OR lower(coalesce(b.purpose,'')) LIKE ?)   -- если text задан
  AND b.status = ?                                  -- если status задан
  AND w.type   = ?                                  -- если workspaceType задан
  AND w.floor  = ?                                  -- если floor задан
  AND b.end_time   > ?   -- dateFrom.atStartOfDay()  -- пересечение периода, как в filterByDateRange
  AND b.start_time < ?   -- dateTo.plusDays(1).atStartOfDay()
ORDER BY <whitelist-колонка> <ASC|DESC>, b.id
```
- `ORDER BY` подставляется **только из `switch` по enum** (`START_TIME → b.start_time`, `EMPLOYEE_NAME → e.full_name`, `WORKSPACE_CODE → w.code`, `DURATION → (b.end_time - b.start_time)`, `STATUS → b.status`). Пользовательская строка в SQL не попадает.
- Экранирование `%`/`_` в `text` — переиспользовать `likePattern` из `BookingRepository` (вынести в общий helper).
- Ошибки SQL → `SqlExceptionTranslator` → `DatabaseException("Не удалось загрузить список броней")`.

**Service:** `BookingQueryService`
```java
public List<BookingRow> search(BookingFilter filter, BookingSort sort, boolean ascending);
public BookingRow getRow(long bookingId);               // EntityNotFoundException, если нет
public List<BookingRow> findAll();                      // = search(empty, START_TIME, false)
```
Проверки: `filter`, `sort` не null (`Validation.requireNotNull`).

> Это закрывает требования «таблица», «поиск по нескольким полям» (ФИО, email, код, цель), «2+ фильтра» (статус, тип места, этаж, период), «2+ сортировки» (5 штук), «просмотр связанных данных» (сотрудник и место в одной строке).

### B3. Операции над бронью (уже готово, только уточняем контракт для UI)

`BookingService` (без изменений):

| Метод | Для чего в UI | Что бросает (показываем текст `getMessage()`) |
|---|---|---|
| `create(employeeId, workspaceId, start, end, attendees, purpose)` | Диалог «Добавить» | `ValidationException` (заблокирован, вне 08–22, шаг не 1 час, горизонт 30 дней, вместимость), `EntityNotFoundException`, `WorkspaceUnavailableException`, `BookingConflictException` |
| `updateTime(id, start, end, attendees, purpose)` | Диалог «Редактировать» (только время/участники/цель; сотрудник и место не меняются) | то же + «изменять можно только активную», «уже началась» |
| `changeStatus(id, target)` / `cancel(id)` | Кнопки «Отменить», «Завершить», «Неявка» | `InvalidStatusTransitionException` |
| `delete(id)` | Кнопка «Удалить» (после подтверждения) | `EntityNotFoundException`, `DatabaseException` |

Что **добавить**: ничего в логику. Но заранее решить (рекомендую): кнопки статусов в UI показываем/включаем по `BookingStatus.canTransitionTo` — так пользователь не видит заведомо невозможных действий.

### B4. Справочники для ComboBox (связанные сущности)

Без идентификаторов вручную — выбор объекта из списка.
```java
// EmployeeService
public List<Employee> findAll();                    // уже есть; для ComboBox использовать только !blocked
public List<Employee> findAvailableForBooking();    // НОВОЕ: findAll() без blocked
// WorkspaceService
public List<Workspace> findAll();                   // уже есть
public List<Workspace> findAvailableForBooking();   // НОВОЕ: status == AVAILABLE
```
В UI у `ComboBox` задаётся `StringConverter` (ФИО — email; «A-18 · этаж 2 · OPEN_DESK»). В бронь уходит `employee.getId()`, `workspace.getId()`.

### B5. Статистика

**DTO**
```java
public record StatisticsReport(
    long totalBookings,
    Map<BookingStatus, Long> byStatus,
    Map<WorkspaceType, Long> byWorkspaceType,
    double averageDurationHours,           // по всем (кроме CANCELLED)
    double minDurationHours, double maxDurationHours,
    double noShowPercent,                  // NO_SHOW / (COMPLETED + NO_SHOW) * 100, 0 если знаменатель 0
    long bookingsInPeriod,                 // за выбранный период
    List<StatisticsService.NamedCount> topWorkspaces,   // топ-5 по числу броней
    List<StatisticsService.NamedCount> topEmployees,    // топ-5
    long totalEmployees, long blockedEmployees) {}
```
**Service**
```java
public StatisticsReport buildReport(LocalDate from, LocalDate to);   // from/to могут быть null = за всё время
```
- Агрегаты желательно считать SQL-ом (`COUNT/AVG/MIN/MAX … GROUP BY`) в repository: `countByStatus()`, `durationStats()`, `countInPeriod(from,to)`. Уже готовые `bookingsByStatus`, `workspacesByType`, `noShowRate`, `topWorkspaces`, `topEmployees`, `averageDurationHoursCompleted` — переиспользовать или обернуть.
- Минимум 5 показателей по заданию набирается с запасом: всего броней; по статусам; по типам места; среднее/мин/макс длительность; % неявок; в периоде; топы.
- Деление на ноль не допускать (пустая БД → нули, а не `NaN`).

### B6. Экспорт текущей таблицы

```java
// util
public interface BookingRowExporter {
    String extension();                                           // "csv" / "xlsx"
    void export(List<BookingRow> rows, Path target);              // бросает ExportException
}
public class CsvBookingExporter implements BookingRowExporter {}   // ';' и BOM (как CsvExporter), экранирование кавычек
public class XlsxBookingExporter implements BookingRowExporter {}  // POI, заголовок жирным, автоширина

// service
public void exportRows(List<BookingRow> rows, Path target);        // выбирает экспортёр по расширению target; нет подходящего → ValidationException("Поддерживаются .csv и .xlsx")
```
- Колонки: №, Сотрудник, Email, Место, Тип, Этаж, Начало, Конец, Статус, Участников, Цель.
- Экспортируем **ровно то, что видно в таблице** (после фильтров/поиска) → контроллер передаёт `table.getItems()`.
- `target == null` (пользователь закрыл диалог) обрабатывается в контроллере: ничего не делаем и не показываем ошибку.
- Ошибки ввода-вывода → `ExportException("Не удалось записать файл …")`.

### B7. Фоновое выполнение (чтобы UI не зависал)

`fx/util/AsyncRunner`
```java
public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError);
```
Внутри — `javafx.concurrent.Task`; `onSuccess/onError` выполняются в потоке FX (`setOnSucceeded/setOnFailed`). Все загрузки (таблица, статистика, справочники) идут через него.

### B8. Перевод исключений в тексты

`fx/util/ErrorMessages`
```java
public static String of(Throwable t);
```
- `AppException` → `getMessage()` (уже понятное русское сообщение).
- `NumberFormatException`/`DateTimeParseException` → «Поле «…» заполнено неверно» (контроллер указывает поле).
- Любое прочее → «Внутренняя ошибка. Подробности в журнале» (stack trace в UI **не** показываем; логируем через `System.Logger`/slf4j; пустых `catch` не оставляем).

---

## 3. Frontend: экраны, `fx:id`, обработчики

Фронт держим максимально простым: один `TabPane`, на вкладках таблицы.

### F1. Каркас
- `OfficeBookingFxApplication extends Application`: `start(Stage)` → создаёт `AppContext` → грузит `fxml/main.fxml` через `FXMLLoader`, `controllerFactory` передаёт `AppContext` в контроллер → `Scene` + `app.css` → `stage.setTitle(...)`, `stage.show()`.
- Объяснение для защиты: **Stage** — окно, **Scene** — содержимое окна, **Node** — любой элемент дерева (Button, TableView, VBox).

### F2. `main.fxml` + `MainController` — вкладка «Брони»

| Элемент | fx:id / onAction | Связь с backend |
|---|---|---|
| Поле поиска | `searchField` (слушатель текста с задержкой ~300 мс) | `BookingFilter.text` |
| Фильтр «Статус» | `statusCombo` (Все + значения enum) | `BookingFilter.status` |
| Фильтр «Тип места» | `typeCombo` | `.workspaceType` |
| Фильтр «Этаж» | `floorCombo` (Все + этажи из данных) | `.floor` |
| Период | `dateFromPicker`, `dateToPicker` (`DatePicker`) | `.dateFrom/.dateTo` |
| Сортировка | `sortCombo` + `ascToggle`; также клик по заголовку столбца | `BookingSort`, `ascending` |
| Кнопка «Сбросить» | `onReset` | `BookingFilter.empty()` |
| Кнопка «Обновить» | `onRefresh` | `search(...)` |
| Таблица | `bookingTable` (`TableView<BookingRow>`), колонки: №, Сотрудник, Место, Тип, Этаж, Начало, Конец, Статус, Участников, Цель | `ObservableList<BookingRow>` |
| Кнопки | `onAdd`, `onEdit`, `onDelete`, `onCancelBooking`, `onComplete`, `onNoShow`, `onExport` | `BookingService.*`, `ExportService.exportRows` |
| Статус-строка | `statusLabel` («Найдено: N») | — |

Поведение:
- Любое изменение фильтра/поиска/сортировки → `reload()` → `AsyncRunner.run(() -> queries.search(...), items::setAll, showError)`.
- Кнопки `Edit/Delete/статусы` отключены (`disableProperty` через binding к `selectionModel.selectedItemProperty().isNull()`).
- После add/edit/delete/смены статуса — `reload()` и выделение изменённой строки (по id).
- `onDelete`: `Alert(CONFIRMATION)` «Удалить бронь #id (сотрудник, место, время)?» → только по OK `bookings.delete(id)`.
- `onExport`: `FileChooser` с фильтрами `*.xlsx`, `*.csv`, имя по умолчанию `bookings.xlsx`; путь не прописан в коде.
- Сортировка: сортируем на стороне БД (`ORDER BY`), поэтому у столбцов `TableColumn.setSortable(false)`, а заголовок-клик вызывает `reload()` с нужным `BookingSort`. (Альтернатива — `SortedList` в памяти: проще, но тогда на защите объяснять, что сортируем в Java; выбираем **SQL** — единообразнее и быстрее на больших данных.)

### F3. `booking-dialog.fxml` + `BookingDialogController` — добавление/редактирование
Один диалог на два режима (`Mode.CREATE` / `Mode.EDIT`).

| Поле | Элемент | Валидация до вызова сервиса |
|---|---|---|
| Сотрудник | `employeeCombo` (`ComboBox<Employee>`) | не пусто; при EDIT — `disable` |
| Место | `workspaceCombo` (`ComboBox<Workspace>`) | не пусто; при EDIT — `disable` |
| Дата | `datePicker` | не пусто |
| Начало | `startHourCombo` (08…21) | не пусто |
| Длительность/конец | `endHourCombo` (09…22, > начала) | конец > начала |
| Участников | `attendeesField` | пусто или целое 1…1000; для переговорной обязательно |
| Цель | `purposeArea` | ≤ 255 символов |
| Ошибка | `errorLabel` (красный, CSS-класс `error`) | показываем первое найденное нарушение |

- Кнопка «Сохранить» вызывает `create`/`updateTime`; при исключении диалог **не закрывается**, текст ошибки в `errorLabel`.
- Часы выбираем из списков, а не вводим вручную (правило «шаг 1 час, 08–22» нарушить нельзя).
- В EDIT поля заполняются из выбранного `BookingRow`.

### F4. Вкладка «Сотрудники» (`employees.fxml`, `EmployeesController`)
- `TableView<Employee>`: ФИО, Email, Отдел, Должность, Дата приёма, Роль, Заблокирован.
- Кнопки: Добавить / Изменить / Удалить (с подтверждением) / Блокировать-разблокировать → `EmployeeService.create/update/delete/setBlocked`.
- Поиск по ФИО/email — фильтр в памяти (`FilteredList`), набор малый.
- Ошибка удаления сотрудника, у которого есть брони (`ON DELETE RESTRICT`), уже даёт понятный текст из сервиса.

### F5. Вкладка «Рабочие места» (`workspaces.fxml`, `WorkspacesController`)
- `TableView<Workspace>`: Код, Этаж, Тип, Статус, Монитор, Вместимость.
- Кнопки: Добавить / Изменить (код, этаж, монитор) / Сменить статус / Удалить → `WorkspaceService.*`.
- Вместимость вводится только для `MEETING_ROOM` (поле активируется по выбранному типу).

### F6. Вкладка «Статистика» (`statistics.fxml`, `StatisticsController`)
- Период: `fromPicker`, `toPicker`, кнопка «Рассчитать» → `statistics.buildReport(from, to)`.
- Плитки (Label): всего броней, в периоде, % неявок, средняя/мин/макс длительность.
- Таблицы/простые диаграммы (`PieChart` по статусам, `BarChart`/`TableView` по типам и топам).
- Загрузка через `AsyncRunner`.

### F7. `app.css`
Стили для: `.root`, `.button` / `.button-primary` / `.button-danger`, `.table-view`, `.text-field`/`.combo-box`, `.title-label`, `.error` (красный текст), `.tile` (карточки статистики). Подключается к `Scene`, а не прописывается в коде элементов.

---

## 4. Валидация (сводка по заданию)

| Что проверяем | Где | Как |
|---|---|---|
| Пустые обязательные поля | Контроллер диалога | Перед вызовом сервиса, `errorLabel` с названием поля |
| Числа и диапазоны | Контроллер → `Booking.validateAttendees` | Парсинг `Integer`, 1…1000 |
| Даты и их логика | Контроллер + `BookingService.validateInterval` | Конец > начала, горизонт 30 дней, не в прошлом, 08:00–22:00 |
| Длина строк | `Validation.optionalText` | Цель ≤ 255, ФИО/email — в моделях |
| Уникальность | БД (`UNIQUE`) + `SqlExceptionTranslator` | email сотрудника, код места → понятное сообщение |
| Ссылки на связанные сущности | `ComboBox` + `EntityNotFoundException` | Нельзя ввести несуществующий id |
| Бизнес-ограничения | `BookingService`/`Workspace.validateBooking` | Конфликты, заблокирован, место недоступно, вместимость |

Принцип: **UI проверяет форму (тип, пусто), сервис/модель — смысл.** Одно правило в одном месте.

---

## 5. Тесты (пишем новые)

Все без запуска JavaFX. Для сервисов — либо тестовая БД (`office_booking_test`), либо подмена репозиториев. Рекомендую **тестовую БД** (быстро проверить SQL).

| Тест | Что проверяет |
|---|---|
| `BookingFilterTest` | `dateFrom > dateTo` → `ValidationException`; `empty()` ничего не фильтрует |
| `BookingQueryRepositoryTest` | поиск по ФИО/email/коду/цели; каждый фильтр отдельно и вместе; период пересекающихся броней; все `BookingSort` в обе стороны; `%`/`_` в тексте не ломают запрос |
| `BookingQueryServiceTest` | `getRow` несуществующего → `EntityNotFoundException`; `null` аргументы |
| `StatisticsServiceTest` | пустая БД → нули без `NaN`; процент неявок; период |
| `CsvBookingExporterTest`, `XlsxBookingExporterTest` | файл создаётся во `@TempDir`, число строк = входных, заголовки, кавычки/`;` в цели |
| `ExportServiceTest` | `.txt` → `ValidationException`; выбор экспортёра по расширению |
| `ErrorMessagesTest` | `AppException` → его текст; `RuntimeException` → общий текст без stack trace |
| Правила моделей | вернуть `ModelRulesTest`, `BookingServiceTest` из КР 1 (в git они помечены удалёнными — решить, восстанавливать ли) |

---

## 6. Порядок работы (маленькие задания)

**Бэкенд (делаете вы, я подсказываю)**
1. B0 — `DatabaseMigrator` + `AppContext` (заодно проверка, что подключение работает).
2. B1 — DTO `BookingRow`, `BookingFilter`, `BookingSort` + `BookingFilterTest`.
3. B2 — `BookingQueryRepository.search` + `BookingQueryService` + тесты. **Самый важный шаг.**
4. B4 — два метода для ComboBox.
5. B5 — `StatisticsReport` + `buildReport` + тесты.
6. B6 — экспортёры строк + `ExportService.exportRows` + тесты.
7. B7/B8 — `AsyncRunner`, `ErrorMessages` + тест.

**Фронт (минимум вникания, делаем по шаблону)**
8. F1 + `app.css` — пустое окно запускается (`mvn javafx:run`).
9. F2 — таблица и загрузка (после шага 3).
10. F2 — поиск, фильтры, сортировка.
11. F3 — диалог брони; удаление с подтверждением; статусы.
12. F6 — статистика (после шага 5).
13. F2 `onExport` (после шага 6).
14. F4, F5 — вкладки сотрудников и мест.
15. Прогон чек-листа ниже, проверка слоёв grep'ом.

---

## 7. Чек-лист «обязательный минимум» из задания

- [ ] Запуск без ошибок (`mvn javafx:run`)
- [ ] БД и предметная область из КР 1
- [ ] Основная сущность в `TableView`
- [ ] Добавление / редактирование / удаление, подтверждение удаления
- [ ] Просмотр связанных данных (ФИО и код места в строке брони)
- [ ] Поиск (≥ 3 поля), фильтры (≥ 2), сортировка (≥ 2)
- [ ] Валидация, понятные сообщения, без stack trace
- [ ] FXML + CSS
- [ ] Разделение Controller / Service / DAO
- [ ] ≥ 5 показателей статистики
- [ ] Экспорт `.xlsx`/`.csv` через `FileChooser`
- [ ] Нет пустых обработчиков и пустых `catch`
- [ ] Нет жёстких путей и пароля БД в коде

## 8. Вопросы защиты → где ответ

| Вопрос | Ответ |
|---|---|
| Как запускается приложение | `Application.start` → `AppContext` → `FXMLLoader` → `Scene` → `Stage` |
| Зачем FXML | Разметка отдельно от логики; связь с контроллером через `fx:controller`, `@FXML` поля по `fx:id`, `onAction` |
| Как обновляется `TableView` | Данные в `ObservableList`; `setAll` после загрузки, таблица перерисовывается сама |
| Зачем `ObservableList` | Уведомляет таблицу об изменениях |
| Где бизнес-логика | `model` + `service` |
| Где работа с данными | `repository` (JDBC, `PreparedStatement`) |
| Как обрабатываются исключения | Репозиторий → `DatabaseException`; сервис → `AppException`-наследники; контроллер → `ErrorMessages` + `Alert`/`errorLabel` |
| Поиск/фильтры | `BookingFilter` → динамический `WHERE` с параметрами |
| Статистика | `StatisticsService.buildReport`, агрегаты `GROUP BY` |
| Экспорт | `BookingRowExporter` по расширению файла из `FileChooser` |
| Почему не в потоке FX | `Task` в `AsyncRunner`, иначе интерфейс замирает на время запроса |
