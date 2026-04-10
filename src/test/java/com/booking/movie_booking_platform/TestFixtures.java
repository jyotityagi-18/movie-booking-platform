package com.booking.movie_booking_platform;

import com.booking.movie_booking_platform.entity.*;
import com.booking.movie_booking_platform.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public final class TestFixtures {

    private TestFixtures() {}

    public static Movie movie(MovieRepository repo,
                               String title, String language, String genre) {
        return repo.save(Movie.builder()
                .title(title).language(language).genre(genre)
                .durationMinutes(150).rating("PG-13").build());
    }

    public static Theatre theatre(TheatreRepository repo,
                                   String name, String city) {
        return repo.save(Theatre.builder()
                .name(name).city(city).address(city + " Address")
                .totalScreens(5).partnerCode(name.replace(" ", "") + "-001").build());
    }

    public static Show show(ShowRepository repo,
                             Movie movie, Theatre theatre,
                             LocalDate date, LocalTime startTime,
                             BigDecimal price, int totalSeats) {
        return repo.save(Show.builder()
                .movie(movie).theatre(theatre)
                .showDate(date).startTime(startTime)
                .endTime(startTime.plusHours(2).plusMinutes(30))
                .basePrice(price)
                .totalSeats(totalSeats).availableSeats(totalSeats)
                .build());
    }

    
    public static void seats(SeatRepository repo, Show show,
                              int regular, int premium, int vip) {
        char row = 'A';
        int col = 1;
        for (int i = 0; i < vip; i++) {
            repo.save(seat(show, row, col++, SeatType.VIP));
            if (col > 10) { col = 1; row++; }
        }
        if (col > 1) { row++; col = 1; }
        for (int i = 0; i < premium; i++) {
            repo.save(seat(show, row, col++, SeatType.PREMIUM));
            if (col > 10) { col = 1; row++; }
        }
        if (col > 1) { row++; col = 1; }
        for (int i = 0; i < regular; i++) {
            repo.save(seat(show, row, col++, SeatType.REGULAR));
            if (col > 10) { col = 1; row++; }
        }
    }

    private static Seat seat(Show show, char row, int col, SeatType type) {
        return Seat.builder()
                .show(show)
                .seatNumber(row + String.valueOf(col))
                .seatRow(String.valueOf(row))
                .seatType(type)
                .status(SeatStatus.AVAILABLE)
                .build();
    }
}

