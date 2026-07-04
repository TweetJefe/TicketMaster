# TicketMaster Database Schema

TicketMaster uses a **Schema-per-Service** database design where all microservices connect to a single database instance (`ticketmaster`) but operate in their own logically isolated schemas. All schemas are defined in PostgreSQL.

## Entity Relationship (ER) Diagram

```mermaid
erDiagram
    %% users_schema
    USER {
        uuid id PK
        string email "UNIQUE, NOT NULL"
        string password "NOT NULL"
        enum role "ADMIN, CUSTOMER, ORGANIZATION"
    }

    %% events_schema
    HALL {
        uuid id PK
        string name "NOT NULL"
        string city "NOT NULL"
        string address "NOT NULL"
        int capacity "NOT NULL"
    }

    EVENT {
        uuid id PK
        string name "NOT NULL"
        string address "NOT NULL"
        string city "NOT NULL"
        timestamp time "NOT NULL"
        uuid hall_id FK "NOT NULL"
    }
    
    EVENT_CATEGORY {
        uuid id PK
        uuid event_id FK "NOT NULL"
        enum type "VIP, STANDARD, ECONOMY"
        decimal price "NOT NULL"
        int quantity "NOT NULL"
    }
    
    PERFORMER {
        uuid id PK
        string name "NOT NULL"
        string genre "NOT NULL"
        text description
    }
    
    EVENT_PERFORMERS {
        uuid event_id FK
        uuid performer_id FK
    }

    %% tickets_schema
    TICKET {
        uuid id PK
        uuid event_id "FK (Logical reference to Event)"
        uuid user_id "FK (Logical reference to User, NULLABLE)"
        decimal price "NOT NULL"
        string sector
        string row
        string seat "NOT NULL"
        enum status "AVAILABLE, SOLD"
        enum type "VIP, STANDARD, ECONOMY"
    }

    %% bookings_schema
    ORDER {
        uuid id PK
        uuid user_id "NOT NULL (Logical reference to User)"
        uuid event_id "NOT NULL (Logical reference to Event)"
        decimal total_amount "NOT NULL"
        string status "PENDING, PAID, FAILED, CANCELED"
        timestamp created_at "NOT NULL"
    }

    ORDER_TICKETS {
        uuid order_id FK "NOT NULL"
        uuid ticket_id "FK (Logical reference to Ticket)"
    }

    %% Relationships (inside event service schema)
    HALL ||--o{ EVENT : "hosts"
    EVENT ||--o{ EVENT_CATEGORY : "contains"
    EVENT }o--o{ EVENT_PERFORMERS : "has"
    PERFORMER }o--o{ EVENT_PERFORMERS : "performs in"
    
    %% Relationships (inside booking service schema)
    ORDER ||--o{ ORDER_TICKETS : "contains"
    
    %% Logical Cross-Service Relationships (dotted lines)
    EVENT ..o{ TICKET : "has pool (logical FK)"
    USER ..o{ TICKET : "owns (logical FK)"
    USER ..o{ ORDER : "places (logical FK)"
    TICKET ..o{ ORDER_TICKETS : "reserved in (logical FK)"
```

---

## Database Details

All data resides within a single PostgreSQL database named `ticketmaster`. Each microservice connects to this database and targets its own dedicated schema.

### 1. Schema `users_schema` (User Service)
* **Table `users`**:
  * `id`: Unique identifier (Primary Key).
  * `email`: User email address (used for credentials, unique index).
  * `password`: Encrypted password hash (BCrypt).
  * `role`: User role (`ADMIN`, `CUSTOMER`, `ORGANIZATION`).

### 2. Schema `events_schema` (Event Service)
* **Table `halls`**:
  * `id`: Unique venue identifier (Primary Key).
  * `name`: Venue name (e.g., "Madison Square Garden").
  * `city`: City location.
  * `address`: Physical address.
  * `capacity`: Total venue capacity.
* **Table `events`**:
  * `id`: Unique event identifier (Primary Key).
  * `name`: Event title.
  * `address`/`city`/`time`: Event location details and date/time.
  * `hall_id`: Foreign key referencing `halls.id`.
* **Table `event_categories`**:
  * Associates the event with pricing/tickets categories.
  * `type`: Ticket type (`VIP`, `STANDARD`, `ECONOMY`).
  * `price`: Ticket price in this category.
  * `quantity`: Number of tickets allocated.
* **Table `performers`**:
  * Band / artist details (name, genre, description).
* **Join Table `event_performers`**:
  * Many-to-many link between events and performers.

### 3. Schema `tickets_schema` (Ticket Service)
* **Table `tickets`**:
  * Details of individual tickets generated for purchase.
  * `event_id`: Logical link to the event.
  * `user_id`: Logical link to the ticket owner (populated after order is successfully paid).
  * `seat`: Seat designation.
  * `status`: Ticket availability status (`AVAILABLE`, `SOLD`).
  * `type`: Ticket category tier (`VIP`, `STANDARD`, `ECONOMY`).

### 4. Schema `bookings_schema` (Booking Service)
* **Table `orders`**:
  * Order header details. Tracks checkout status (`PENDING`, `PAID`, `FAILED`, `CANCELED`), event ID, total amount, and creation time.
* **Collection Table `order_tickets`**:
  * Maps orders to reserved ticket IDs.