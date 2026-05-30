# E-commerce Order Processor

A small Spring Boot application that lets registered users place and manage
customer orders through a single-page web UI. Orders move through a fulfilment
lifecycle automatically via a background scheduler.

It is intentionally compact — a single Maven module with an in-memory database —
so it runs with no external setup while still demonstrating a realistic layered
architecture (entities → repositories → services → REST controllers), Spring
Security form login, and scheduled background processing.

---

## Tech stack

| Concern        | Choice                                            |
|----------------|---------------------------------------------------|
| Language       | Java 21                                           |
| Framework      | Spring Boot 4.0.6 (Web MVC, Data JPA, Security)   |
| Persistence    | Hibernate / Spring Data JPA                       |
| Database       | H2 (in-memory) by default; MySQL driver available |
| Security       | Spring Security (form login, BCrypt passwords)    |
| Build          | Maven (via the bundled `mvnw` wrapper)            |
| Frontend       | Static `index.html` (vanilla JS `fetch`)          |

---

## Features

- **User registration & login** — self-service signup; passwords are stored
  BCrypt-hashed and authenticated against the database via form login.
- **Place orders** — create an order with a customer name and one or more line
  items (product name, quantity, unit price).
- **View & filter orders** — list all orders or filter by status.
- **Manage orders** — cancel a `PENDING` order or manually move an order to the
  next status.
- **Automatic fulfilment** — a scheduled job advances active orders through the
  lifecycle without manual intervention.
- **H2 console** — browse the live database at `/h2-console` for debugging.

---

## Order lifecycle

```
PENDING ──▶ PROCESSING ──▶ SHIPPED ──▶ DELIVERED
   │
   └──▶ CANCELLED        (only allowed while PENDING)
```

- New orders always start as `PENDING`.
- `CANCELLED` and `DELIVERED` are terminal — no further transitions are allowed.
- The `OrderScheduler` automatically promotes `PENDING → PROCESSING → SHIPPED →
  DELIVERED` on a fixed interval (default every 30s).

---

## Project structure

```
src/main/java/com/peerisland/ecomm
├── EcommApplication.java              # Boot entry point (@EnableScheduling)
│
├── order/                            # Order domain
│   ├── Order.java                     # Order entity (1..* OrderItem)
│   ├── OrderItem.java                 # Line item entity
│   ├── OrderStatus.java               # Lifecycle enum
│   ├── OrderRepository.java           # Spring Data JPA repository
│   ├── OrderService.java              # Business rules & transitions
│   ├── OrderController.java           # REST API under /api/orders
│   ├── OrderScheduler.java            # Background lifecycle advancer
│   ├── OrderNotFoundException.java    # → HTTP 404
│   └── IllegalOrderStateException.java# → HTTP 409
│
├── user/                             # Authentication domain
│   ├── User.java                      # User entity (unique username)
│   ├── UserRepository.java            # Spring Data JPA repository
│   ├── CustomUserDetailsService.java  # Loads users for Spring Security
│   └── AuthController.java            # Signup API under /api/auth
│
└── config/
    └── SecurityConfig.java            # Security filter chain & PasswordEncoder

src/main/resources
├── application.properties             # Datasource / JPA / H2 console config
└── static/index.html                  # Single-page frontend
```

The code is grouped by **feature** (`order`, `user`, `config`) rather than by
technical layer, which keeps related classes together and makes the project
easy to extend.

### How the layers fit together

- **Entities** (`Order`, `OrderItem`, `User`) are JPA `@Entity` classes mapped to
  database tables. `Order ──< OrderItem` is a bidirectional one-to-many; the back
  reference is `@JsonIgnore`'d to prevent infinite JSON recursion.
- **Repositories** extend `JpaRepository`, giving CRUD plus derived queries such
  as `findByStatus(...)` and `findByUsername(...)` for free.
- **Services** (`OrderService`) hold the business rules and run inside
  `@Transactional` boundaries — e.g. "only PENDING orders can be cancelled".
  Invalid operations throw exceptions mapped to the right HTTP status.
- **Controllers** expose thin REST endpoints and delegate to the services.
- **Scheduler** (`OrderScheduler`) runs on a timer (enabled by
  `@EnableScheduling` on the application class) and advances active orders.
- **Security** (`SecurityConfig` + `CustomUserDetailsService`) wires form login
  to database-backed users with BCrypt-encoded passwords.

---

## REST API

All `/api/orders/**` endpoints require authentication. `signup`, the landing
page, `/error`, and the H2 console are public.

### Auth

| Method | Path                | Body                          | Description                     |
|--------|---------------------|-------------------------------|---------------------------------|
| POST   | `/api/auth/signup`  | `{ "username", "password" }`  | Register a new user             |

Responses: `200` on success, `409` if the username is taken, `400` if input is
missing.

### Orders

| Method | Path                              | Body / Params                                   | Description                          |
|--------|-----------------------------------|-------------------------------------------------|--------------------------------------|
| POST   | `/api/orders`                     | `{ "customerName", "items": [...] }`            | Create a new order (starts PENDING)  |
| GET    | `/api/orders`                     | `?status=PENDING` (optional)                    | List all orders, or filter by status |
| POST   | `/api/orders/{id}/cancel`         | —                                               | Cancel a PENDING order               |
| PATCH  | `/api/orders/{id}/status`         | `?status=SHIPPED`                               | Move an order to a specific status   |

Each item is `{ "productName", "quantity", "price" }`.

Example — create an order:

```bash
curl -u alice:secret -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
        "customerName": "Alice",
        "items": [
          { "productName": "Keyboard", "quantity": 1, "price": 49.99 },
          { "productName": "Mouse",    "quantity": 2, "price": 19.50 }
        ]
      }'
```

---

## Running the application

### Prerequisites
- JDK 21 or newer (the project compiles to Java 21 bytecode).
- No database install needed — H2 runs in memory.

### Start it

```bash
# from the ecomm/ directory
./mvnw spring-boot:run          # macOS / Linux
mvnw.cmd spring-boot:run        # Windows
```

Then open <http://localhost:8080>.

1. Use the **Sign Up** card to register a user.
2. Click **Sign In** and log in with that user (Spring Security's default login page).
3. Place orders and watch them advance through their statuses on refresh.

### Run the tests

```bash
./mvnw test
```

### Build a runnable jar

```bash
./mvnw clean package
java -jar target/ecomm-0.0.1-SNAPSHOT.jar
```

---

## Configuration

Key settings in `src/main/resources/application.properties`:

| Property                          | Default                  | Notes                                  |
|-----------------------------------|--------------------------|----------------------------------------|
| `spring.datasource.url`           | `jdbc:h2:mem:ecommdb`    | In-memory; data resets on restart      |
| `spring.jpa.hibernate.ddl-auto`   | `update`                 | Schema auto-created from entities      |
| `spring.h2.console.enabled`       | `true`                   | Console served at `/h2-console`        |

The scheduler interval can be overridden (defaults to 30 000 ms):

```properties
ecomm.scheduler.advance-rate-ms=30000
```

### Switching to MySQL

The MySQL driver (`mysql-connector-j`) is already on the classpath. To use it,
replace the H2 datasource properties with your MySQL connection details, e.g.:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommdb
spring.datasource.username=youruser
spring.datasource.password=yourpassword
```

---

## Notes & caveats

- **In-memory data**: with H2, all orders and users are lost on restart.
- **CSRF is disabled** so the browser `fetch()` calls work without a token. This
  is fine for the demo but should be revisited before any production deployment.
- **Redis starter** is on the classpath but currently unused; it can be removed
  if you don't intend to add caching/session storage.
```
