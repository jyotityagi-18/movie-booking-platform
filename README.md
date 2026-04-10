🎬 Movie Booking Platform
A Spring Boot REST API for booking movie tickets — supports theatre onboarding, show management, seat selection, and automatic price calculation with discount rules.
Getting Started
```bash
./mvnw spring-boot:run
```
App runs at `http://localhost:8080`
---
API Reference
Movies
Method	Endpoint	Description
POST	`/api/v1/admin/movies/create`	Add a movie
GET	`/api/v1/admin/movies/findAllMovies`	List all movies
Theatres
Method	Endpoint	Description
POST	`/api/v1/admin/theatres/create`	Add a theatre
GET	`/api/v1/admin/theatres/findAll`	List all theatres
POST	`/api/v1/admin/theatres/shows`	Create a show with seat inventory
Shows
Method	Endpoint	Description
GET	`/api/v1/shows/search?city=X&movieName=Y&date=Z`	Search shows by city, movie, date
GET	`/api/v1/shows/seats/{showId}`	View seat map for a show
Bookings
Method	Endpoint	Description
POST	`/api/v1/bookings/create`	Book tickets (price auto-calculated)
GET	`/api/v1/bookings/getBookingById/{id}`	Get booking details
POST	`/api/v1/bookings/cancel/{id}`	Cancel a booking
---
Pricing Rules
Prices are calculated automatically at booking time — no manual input needed.
Rule	Details
Seat type	REGULAR ×1.0 · PREMIUM ×1.5 · VIP ×2.0
Every 3rd ticket	50% off
Afternoon shows (12:00–17:00)	20% off
The booking response includes `totalAmount`, `discountAmount`, `finalAmount`, `priceBreakdown`, and `appliedOffers`.
-
