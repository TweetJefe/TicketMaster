# Project Roadmap & Future Architecture Plans

Languages / Idiomas / Языки:
* [English](#english)
* [Español](#español)
* [Русский](#русский)

---

## English

This document outlines the planned improvements and refactoring steps for the TicketMaster project.

### 1. Extract Hall Service
* **Objective:** Decouple venue (hall) management into a standalone service.
* **Details:** 
  * Move halls and seating configurations into a dedicated `Hall Service` with its own isolated database.
  * This isolation will allow the Hall Service to directly interact with frontend seating layouts and interactive schema managers.

### 2. Implement Saga Pattern (DONE!)
* **Objective:** Ensure eventual consistency across distributed databases during order checkout.
* **Details:** 
  * Implement an orchestrator-based Saga pattern to manage the ticket booking flow.
  * If the payment fails in `Booking Service` or ticket status updates fail in `Ticket Service`, execute compensating transactions to unlock seats and revert ticket statuses automatically.

### 3. Implement Transactional Outbox Pattern
* **Objective:** Guarantee reliable event publishing to Apache Kafka.
* **Details:** 
  * Save outgoing Kafka events (e.g., `EventCreatedMessage`) to an `outbox` table in the local service database within the same database transaction.
  * Use a separate polling publisher to dispatch events to Kafka only after the database transaction is successfully committed, preventing data inconsistencies during database rollbacks.

---

## Español

Este documento detalla las mejoras y pasos de refactorización planeados para el proyecto TicketMaster.

### 1. Extraer el Hall Service
* **Objetivo:** Desacoplar la gestión de salas (recintos) en un servicio independiente.
* **Detalles:** 
  * Mover las salas y las configuraciones de asientos a un `Hall Service` dedicado con su propia base de datos aislada.
  * Esta separación permitirá que el Hall Service interactúe directamente con mapas de asientos interactivos y esquemas de salas en el frontend.

### 2. Implementar el Patrón Saga (LISTO!)
* **Objetivo:** Garantizar la consistencia eventual en bases de datos distribuidas durante la reserva de entradas.
* **Detalles:** 
  * Implementar un patrón Saga basado en un orquestador para gestionar el flujo de reserva de entradas.
  * Si el pago falla en `Booking Service` o la actualización del estado de las entradas falla en `Ticket Service`, ejecutar transacciones de compensación para desbloquear los asientos y revertir los estados de las entradas automáticamente.

### 3. Implementar el Patrón Transactional Outbox
* **Objetivo:** Garantizar la publicación fiable de eventos en Apache Kafka.
* **Detalles:** 
  * Guardar los eventos salientes de Kafka en una tabla `outbox` en la base de datos local del servicio dentro de la misma transacción de la base de datos.
  * Usar un publicador externo para enviar los eventos a Kafka solo después de que la transacción de la base de datos se haya confirmado con éxito, evitando inconsistencias durante reversiones de base de datos (rollbacks).

---

## Русский

В этом документе описаны планируемые улучшения и шаги по рефакторингу проекта TicketMaster.

### 1. Выделение сервиса залов (Hall Service)
* **Цель:** Вынести управление концертными площадками (залами) в отдельный микросервис.
* **Детали:** 
  * Перенести залы и настройки посадочных мест в выделенный сервис `Hall Service` с его собственной изолированной базой данных.
  * Это разделение позволит сервису залов напрямую взаимодействовать со схемами залов и интерактивным выбором мест на фронтенде.

### 2. Внедрение паттерна Saga (DONE!)
* **Цель:** Гарантировать согласованность данных в распределенных базах при оформлении и оплате билетов.
* **Детали:** 
  * Реализовать оркестрируемый паттерн Сага (Saga) для управления процессом покупки билетов.
  * Если оплата в `Booking Service` не прошла или обновление статуса билетов в `Ticket Service` завершилось ошибкой, автоматически запустить компенсирующие транзакции для разблокировки мест и возврата билетов в свободную продажу.

### 3. Внедрение паттерна Transactional Outbox
* **Цель:** Гарантировать надежную отправку сообщений в Apache Kafka.
* **Детали:** 
  * Сохранять исходящие события Kafka в таблицу `outbox` в локальной базе данных сервиса в рамках той же транзакции БД.
  * Использовать отдельный фоновый отправитель для доставки сообщений в Kafka только после успешного завершения (коммита) транзакции в БД, что исключает рассинхронизацию данных при откатах транзакций в базе данных.
