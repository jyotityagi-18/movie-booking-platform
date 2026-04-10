# 🎬 Bruno API Guide

> Start app: `./mvnw spring-boot:run` → http://localhost:8080

---

## STEP 1 — Create Movies

**POST** `http://localhost:8080/api/v1/admin/movies/create`
```json
{
  "title": "Dune: Part Three",
  "language": "English",
  "genre": "Sci-Fi",
  "durationMinutes": 165,
  "rating": "PG-13"
}
```

**POST** `http://localhost:8080/api/v1/admin/movies/create`
```json
{
  "title": "Pathaan 2",
  "language": "Hindi",
  "genre": "Action",
  "durationMinutes": 150,
  "rating": "UA"
}
```

**POST** `http://localhost:8080/api/v1/admin/movies/create`
```json
{
  "title": "RRR 2",
  "language": "Telugu",
  "genre": "Action",
  "durationMinutes": 180,
  "rating": "UA"
}
```

---

## STEP 2 — List All Movies

**GET** `http://localhost:8080/api/v1/admin/movies/findAll`

> Returns all movies with their IDs. Note the `id` values — you need them when creating shows.

---

## STEP 3 — Create Theatres

**POST** `http://localhost:8080/api/v1/admin/theatres/create`
```json
{
  "name": "PVR Orion Mall",
  "city": "Bangalore",
  "address": "Rajajinagar, Bangalore",
  "totalScreens": 8,
  "partnerCode": "PVR-BLR-001"
}
```

**POST** `http://localhost:8080/api/v1/admin/theatres/create`
```json
{
  "name": "INOX Garuda Mall",
  "city": "Bangalore",
  "address": "Magrath Road, Bangalore",
  "totalScreens": 5,
  "partnerCode": "INOX-BLR-001"
}
```

**POST** `http://localhost:8080/api/v1/admin/theatres/create`
```json
{
  "name": "PVR Phoenix",
  "city": "Mumbai",
  "address": "Lower Parel, Mumbai",
  "totalScreens": 10,
  "partnerCode": "PVR-MUM-001"
}
```

---

## STEP 4 — List All Theatres

**GET** `http://localhost:8080/api/v1/admin/theatres/findAll`

> Returns all theatres with their IDs. Note the `id` values for creating shows.

---

## STEP 5 — Create Shows (with seat inventory)

> Use movieId and theatreId from the list responses above.

### Morning show — Dune @ PVR Bangalore (NO afternoon discount)
**POST** `http://localhost:8080/api/v1/admin/theatres/shows`
```json
{
  "movieId": 1,
  "theatreId": 1,
  "showDate": "2026-04-10",
  "startTime": "10:00",
  "endTime": "12:45",
  "basePrice": 250,
  "seatConfig": { "REGULAR": 18, "PREMIUM": 9, "VIP": 3 }
}
```

### Afternoon show — Dune @ PVR Bangalore (20% afternoon discount kicks in)
**POST** `http://localhost:8080/api/v1/admin/theatres/shows`
```json
{
  "movieId": 1,
  "theatreId": 1,
  "showDate": "2026-04-10",
  "startTime": "14:00",
  "endTime": "16:45",
  "basePrice": 300,
  "seatConfig": { "REGULAR": 18, "PREMIUM": 9, "VIP": 3 }
}
```

### Evening show — Pathaan @ PVR Bangalore
**POST** `http://localhost:8080/api/v1/admin/theatres/shows`
```json
{
  "movieId": 2,
  "theatreId": 1,
  "showDate": "2026-04-10",
  "startTime": "18:00",
  "endTime": "20:30",
  "basePrice": 280,
  "seatConfig": { "REGULAR": 18, "PREMIUM": 9, "VIP": 3 }
}
```

### Afternoon show — Dune @ INOX Bangalore
**POST** `http://localhost:8080/api/v1/admin/theatres/shows`
```json
{
  "movieId": 1,
  "theatreId": 2,
  "showDate": "2026-04-10",
  "startTime": "13:00",
  "endTime": "15:45",
  "basePrice": 350,
  "seatConfig": { "REGULAR": 12, "PREMIUM": 6, "VIP": 2 }
}
```

### Afternoon show — Pathaan @ PVR Mumbai
**POST** `http://localhost:8080/api/v1/admin/theatres/shows`
```json
{
  "movieId": 2,
  "theatreId": 3,
  "showDate": "2026-04-10",
  "startTime": "15:00",
  "endTime": "17:30",
  "basePrice": 350,
  "seatConfig": { "REGULAR": 24, "PREMIUM": 12, "VIP": 4 }
}
```

---

## STEP 6 — Browse Shows (READ SCENARIO)

**GET** `http://localhost:8080/api/v1/shows/search?city=Bangalore&date=2026-04-10`

**GET** `http://localhost:8080/api/v1/shows/search?city=Bangalore&movieName=Dune&date=2026-04-10`

**GET** `http://localhost:8080/api/v1/shows/search?city=Mumbai&date=2026-04-10`

> `movieName` supports partial matching — e.g. `movieName=Du` will match "Dune: Part Three".

> Response includes `showId`, `basePrice`, `availableSeats`, `afternoonShow` flag for each show.

---

## STEP 7 — View Seat Map (pick seatIds for booking)

**GET** `http://localhost:8080/api/v1/shows/seats/1`

**GET** `http://localhost:8080/api/v1/shows/seats/2`

> Each seat has `seatId`, `seatNumber`, `seatType` (VIP/PREMIUM/REGULAR), `status` (AVAILABLE/BOOKED).
> Pick `seatId` values where status = AVAILABLE — you need them for the next step.

---

## STEP 8 — Book Tickets (WRITE SCENARIO)

> You only send `showId`, `customerName`, `customerEmail`, and `seatIds`.
> The API automatically calculates the price based on:
> - Seat type multiplier (REGULAR ×1.0, PREMIUM ×1.5, VIP ×2.0)
> - 50% off every 3rd ticket
> - 20% off for afternoon shows (12:00–17:00)

### Book 2 seats for morning show (no discount expected)
**POST** `http://localhost:8080/api/v1/bookings/create`
```json
{
  "showId": 1,
  "customerName": "Jyoti",
  "customerEmail": "jyoti@example.com",
  "seatIds": [1, 2]
}
```

### Book 3 seats for afternoon show (both discounts apply)
**POST** `http://localhost:8080/api/v1/bookings/create`
```json
{
  "showId": 2,
  "customerName": "Rahul",
  "customerEmail": "rahul@example.com",
  "seatIds": [31, 32, 33]
}
```

> Response automatically includes:
> - `totalAmount` — base price before discounts
> - `discountAmount` — total discount applied
> - `finalAmount` — amount after discounts
> - `priceBreakdown` — per-ticket calculation
> - `appliedOffers` — which discount rules were triggered

---

## STEP 9 — Get Booking Details

**GET** `http://localhost:8080/api/v1/bookings/getBookingById/1`

**GET** `http://localhost:8080/api/v1/bookings/getBookingById/2`

> Returns full booking with price breakdown and applied offers.

---

## STEP 10 — Cancel Booking

**POST** `http://localhost:8080/api/v1/bookings/cancel/1`

> Seats are released back to AVAILABLE, `availableSeats` count is restored.

---

## API Summary

| # | Method | URL | Purpose |
|---|--------|-----|---------|
| 1 | POST | `/api/v1/admin/movies/create` | Add movie |
| 2 | GET | `/api/v1/admin/movies/findAllMovies` | List all movies |
| 3 | POST | `/api/v1/admin/theatres/create` | Add theatre |
| 4 | GET | `/api/v1/admin/theatres/findAll` | List all theatres |
| 5 | POST | `/api/v1/admin/theatres/shows` | Create show + seats |
| 6 | **GET** | **`/api/v1/shows/search?city=X&movieName=Y&date=Z`** | **Browse shows (READ)** |
| 7 | GET | `/api/v1/shows/seats/{showId}` | Seat map |
| 8 | **POST** | **`/api/v1/bookings/create`** | **Book tickets — price auto-calculated (WRITE)** |
| 9 | GET | `/api/v1/bookings/getBookingById/{id}` | Booking details + price breakdown |
| 10 | POST | `/api/v1/bookings/cancel/{id}` | Cancel booking |
