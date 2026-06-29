# TicketMaster

Languages / Idiomas / Языки:
* [English](#english)
* [Español](#español)
* [Русский](#русский)

---

## English

TicketMaster is a microservice-based ticketing and booking platform.

### Tech Stack
* Java 21 and Spring Boot 3.2
* Spring Cloud (Gateway, Eureka for service registry)
* Apache Kafka (async communication between services)
* Redis (shopping cart cache and temporary seat locks)
* PostgreSQL (separate database for each microservice, managed via Liquibase)

### Services
* User Service: Handles registration, login, user profiles, and JWT generation.
* Event Service: Manages events, performers, and halls.
* Ticket Service: Listens to Kafka event-created topic, automatically generates a pool of tickets for new events, handles purchases and ticket status changes.
* Booking Service: Manages carts, orders, and uses Redis locks to prevent double-booking.

### Authentication
A user logs in via User Service and gets a JWT. The API Gateway validates the JWT signature and forwards the user ID and role downstream to other services using X-User-Id and X-User-Role HTTP headers.

### How to run
Run in the root folder:
docker compose up --build -d

### Ports & Access
* API Gateway: http://localhost:8090
* Swagger UI: http://localhost:8080 (aggregates API docs for all services)
* Eureka Dashboard: http://localhost:8761

Default admin credentials for testing:
* Email: admin@ticketmaster.com
* Password: admin

Detailed architecture schemes and database layouts are in the docs/ folder.

---

## Español

TicketMaster es una plataforma de reserva y venta de entradas basada en microservicios.

### Tecnologías
* Java 21 y Spring Boot 3.2
* Spring Cloud (Gateway, Eureka como registro de servicios)
* Apache Kafka (comunicación asíncrona entre servicios)
* Redis (caché de carritos de compra y bloqueos temporales de asientos)
* PostgreSQL (base de datos independiente por microservicio, gestionada con Liquibase)

### Servicios
* User Service: Gestiona registro, inicio de sesión, perfiles y generación de tokens JWT.
* Event Service: Administra eventos, intérpretes y recintos/salas de conciertos (halls).
* Ticket Service: Escucha el tema de Kafka event-created, genera automáticamente las entradas para nuevos eventos y gestiona compras y estados de tickets.
* Booking Service: Administra carritos, pedidos y usa bloqueos de Redis para evitar doble reserva.

### Autenticación
El usuario inicia sesión en User Service y obtiene un token JWT. API Gateway valida la firma del token y propaga el ID de usuario y rol a los demás microservicios mediante las cabeceras HTTP X-User-Id y X-User-Role.

### Cómo ejecutar
Ejecuta en la carpeta raíz:
docker compose up --build -d

### Puertos y Acceso
* API Gateway: http://localhost:8090
* Swagger UI: http://localhost:8080 (agrupa la documentación de todos los servicios)
* Eureka Dashboard: http://localhost:8761

Credenciales de administrador por defecto:
* Correo: admin@ticketmaster.com
* Contraseña: admin

Los esquemas de arquitectura y el diseño de la base de datos están en la carpeta docs/.

---

## Русский

TicketMaster — микросервисная платформа для бронирования и продажи билетов.

### Стек технологий
* Java 21 и Spring Boot 3.2
* Spring Cloud (Gateway, реестр сервисов Eureka)
* Apache Kafka (асинхронное взаимодействие сервисов)
* Redis (кэш корзин и временная блокировка мест)
* PostgreSQL (отдельная база данных для каждого микросервиса, миграции через Liquibase)

### Сервисы
* User Service: Регистрация, авторизация, профили пользователей и генерация JWT.
* Event Service: Управление концертами, артистами и концертными площадками (залами).
* Ticket Service: Слушает топик Kafka о создании ивентов, генерирует под них билеты, отвечает за покупку и смену статусов билетов.
* Booking Service: Управление заказами, корзинами и блокировкой мест через Redis для защиты от овербукинга.

### Аутентификация
Пользователь логинится в User Service и получает JWT. API Gateway проверяет подпись токена и пробрасывает ID и роль пользователя в заголовках X-User-Id и X-User-Role в остальные микросервисы.

### Как запустить
Запустить в корневой папке:
docker compose up --build -d

### Адреса и порты
* API Gateway: http://localhost:8090
* Swagger UI: http://localhost:8080 (документация всех сервисов в одном месте)
* Eureka Dashboard: http://localhost:8761

Данные администратора для тестов:
* Email: admin@ticketmaster.com
* Пароль: admin

Подробные схемы архитектуры и структуры баз данных лежат в папке docs/.
