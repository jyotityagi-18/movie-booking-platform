===================================================
MOVIE BOOKING PLATFORM - SPRING BOOT APPLICATION
====================================================

OVERVIEW
--------
This is a Movie Booking Platform backend built using:
- Java 17
- Spring Boot
- Spring Data JPA
- H2 In-Memory Database

The application allows:
- Theatre partners to onboard theatres
- Customers to browse movies by city
- Customers to book tickets with discounts applied


ARCHITECTURE
------------
Controller Layer -> Service Layer -> Repository Layer -> Database
|
DTOs

Key design principles:
- Entities are persistence models only
- Controllers return DTOs, not entities
- Business logic in Service layer
- JPA object relationships used
- Transactions handled at service level


DOMAIN ENTITIES
---------------
- TheatrePartner
- Theatre
- Screen
- Movie
- Show
- Booking

All entities:
- Use proper JPA mappings
- Override equals() and hashCode()
- Prevent JSON recursion using @JsonIgnore
- Are DTO-ready


REST APIs
---------

1) Browse movies by city
------------------------
CMD:
curl http://localhost:8080/api/movies/city/Bangalore

Response:
[
{
"id": 1,
"movieName": "Inception",
"language": "English",
"genre": "Sci-Fi"
}
]


2) Book tickets
---------------
CMD:
curl -X POST "http://localhost:8080/api/bookings?showId=1&email=test@gmail.com" ^
-H "Content-Type: application/json" ^
-d "[\"A1\",\"A2\",\"A3\"]"

Response:
{
"bookingId": 10,
"totalAmount": 720.0
}


PRICING & DISCOUNTS
------------------
- Base price = show.basePrice * number of seats
- Group discount:
- If seats >= 3, flat discount applied
- Afternoon discount:
- Shows between 12:00 PM and 4:00 PM get 20% off
DTO STRATEGY
------------
- Entities are NOT returned from controllers
- DTOs are used for API responses

Example DTOs:
- MovieDTO
- BookingResponseDTO


TECH STACK
----------
- Java 17
- Spring Boot
- Spring Data JPA
- H2 Database
- Maven

HOW TO RUN (CMD)
----------------

1) Clean and build project
CMD:
mvn clean install

2) Run application
CMD:
mvn spring-boot:run
