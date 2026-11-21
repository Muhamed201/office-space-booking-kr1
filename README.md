# Office Booking

Система бронирования рабочих мест. КР №3 содержит Spring Boot REST API, PostgreSQL и веб-клиент React.

## Запуск

Понадобятся Docker с Compose, JDK 21 для локальной сборки и Node.js 20.19+ или 22.12+ для веб-клиента.

1. Скопируйте `.env.example` в `.env`. Задайте `POSTGRES_PASSWORD`, `BOOTSTRAP_ADMIN_PASSWORD` и `JWT_SECRET_BASE64`. Для ключа нужны минимум 32 случайных байта в Base64. Например, в PowerShell 7:

   ```powershell
   [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   ```

2. Соберите и запустите backend и PostgreSQL:

   ```sh
   docker compose up --build -d
   docker compose ps
   ```

   API доступен на `http://localhost:8080`, PostgreSQL — на `localhost:55432`. Состояние API проверяется по `/actuator/health`. Команда `docker compose down` сохраняет данные PostgreSQL в именованном томе.

3. Запустите веб-клиент:

   ```sh
   cd frontend
   npm ci
   npm run dev
   ```

   Веб-клиент доступен на `http://localhost:3000`. Прокси читает `APP_PORT` из корневого `.env`. Для другого адреса API задайте `VITE_API_TARGET` в `frontend/.env.local`.

4. Войдите с `BOOTSTRAP_ADMIN_EMAIL` и временным паролем из `.env`, затем смените пароль. Новые учётные записи создаёт администратор. Swagger находится по адресу `http://localhost:8080/swagger-ui.html`. Для доступа в браузере используются email и постоянный пароль администратора.

Для локального запуска backend без контейнера передайте настройки подключения `SPRING_DATASOURCE_*`, `JWT_SECRET_BASE64` и параметры администратора в окружение процесса, затем выполните `./mvnw spring-boot:run` или `.\mvnw.cmd spring-boot:run` в PowerShell.

## Роли

| Роль | Доступ |
| --- | --- |
| Сотрудник | Просмотр и создание своих броней, заметки и экспорт в пределах доступных записей |
| Офис-менеджер | Просмотр и редактирование всех броней, архив и восстановление, массовые операции, импорт |
| Администратор | Доступ менеджера, управление местами и учётными записями, аудит, Swagger |

Окончательное удаление доступно администратору при `PERMANENT_DELETE_ENABLED=true`. Архивная запись освобождает интервал. Восстановление активной брони проверяет, свободен ли интервал. Время бронирования отображается в московской зоне.

## REST API

| Сценарий | Маршруты |
| --- | --- |
| Сессия | `POST /api/auth/login`, `/refresh`, `/logout`, `/change-password`, `GET /api/auth/me` |
| Брони | `GET /api/bookings`, `POST /api/bookings`, `GET`, `PUT`, `DELETE /api/bookings/{id}`, `POST /api/bookings/{id}/restore`, `DELETE /api/bookings/{id}/permanent` |
| Массовые действия | `POST /api/bookings/bulk` |
| CSV | `GET /api/bookings/export`, `POST /api/bookings/import/preview`, `POST /api/bookings/import/apply` |
| Заметки | `GET`, `POST /api/bookings/{id}/notes`, `PUT`, `DELETE /api/notes/{id}` |
| Справочник | `GET`, `POST /api/workspaces`, `PUT /api/workspaces/{id}`, `POST /api/workspaces/{id}/enable`, `/disable` |
| Аналитика | `GET /api/analytics/summary` |
| Администрирование | `GET`, `POST /api/admin/users`, `PUT /api/admin/users/{id}`, `POST /api/admin/users/{id}/reset-password`, `GET /api/admin/audit` |

Запросы используют `Authorization: Bearer <accessToken>`. Для обновления и выхода отправьте JSON с полем `refreshToken`. Список броней принимает `q`, `workspaceId`, `status`, `isRecurring`, `requiresEquipment`, `priority`, `tag`, `archived`, `minAttendees`, `minDuration` и `maxDuration` (в минутах), `page`, `size`, `sort`. В ответе списков есть `content` и метаданные `page`.

CSV импорт принимает `multipart/form-data` с полями `file` и `mode` (`CREATE` или `UPDATE`). Используйте заголовок из экспорта. Режим UPDATE ищет бронь по `bookingNumber`. Проверка не сохраняет строки. Применение повторно проверяет файл и возвращает результат по каждой ошибочной строке.

## Обновление базы и проверка

Flyway сохраняет историю КР 1/2 и переводит связанные ключи на UUID новой миграцией. Для базы черновой КР 3 используется отдельная история Flyway. При первом обновлении такой базы старые refresh-сессии отзываются, а для временной учётной записи администратора из черновика требуется новый пароль в `.env`.

```sh
./mvnw clean verify
cd frontend
npm ci
npm run build
npm audit
```

В PowerShell запускайте Maven через `.\mvnw.cmd clean verify`. Тесты проверяют загрузку контекста на H2, приоритет, фильтрацию, блокировку входа и доступ к API.
