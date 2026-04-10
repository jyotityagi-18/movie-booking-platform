package com.booking.movie_booking_platform.service;

import com.booking.movie_booking_platform.dto.*;
import com.booking.movie_booking_platform.entity.*;
import com.booking.movie_booking_platform.exception.ResourceNotFoundException;
import com.booking.movie_booking_platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;

    

    @Transactional
    public MovieResponse createMovie(MovieCreateRequest req) {
        Movie movie = movieRepository.save(Movie.builder()
                .title(req.title())
                .language(req.language())
                .genre(req.genre())
                .durationMinutes(req.durationMinutes())
                .rating(req.rating())
                .build());
        return toMovieResponse(movie);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> listMovies() {
        return movieRepository.findAll().stream()
                .map(this::toMovieResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TheatreResponse createTheatre(TheatreCreateRequest req) {
        Theatre theatre = theatreRepository.save(Theatre.builder()
                .name(req.name())
                .city(req.city())
                .address(req.address())
                .totalScreens(req.totalScreens())
                .partnerCode(req.partnerCode())
                .build());
        return toTheatreResponse(theatre);
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> listTheatres() {
        return theatreRepository.findAll().stream()
                .map(this::toTheatreResponse)
                .collect(Collectors.toList());
    }

    

    @Transactional(readOnly = true)
    public List<ShowSearchResponse> searchShows(String city, String movieName, LocalDate date) {
        List<Show> shows;
        if (movieName != null && !movieName.isBlank()) {
            shows = showRepository.findShowsByCityMovieNameAndDate(city, movieName, date);
        } else {
            shows = showRepository.findShowsByCityAndDate(city, date);
        }
        return shows.stream().map(this::toSearchResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsForShow(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show not found with id: " + showId);
        }
        return seatRepository.findByShowId(showId).stream()
                .map(this::toSeatResponse)
                .collect(Collectors.toList());
    }

    

    @Transactional
    public ShowSearchResponse createShow(ShowCreateRequest req) {
        Movie movie = movieRepository.findById(req.movieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + req.movieId()));
        Theatre theatre = theatreRepository.findById(req.theatreId())
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + req.theatreId()));

        int totalSeats = req.seatConfig().values().stream().mapToInt(Integer::intValue).sum();

        Show show = showRepository.save(Show.builder()
                .movie(movie)
                .theatre(theatre)
                .showDate(req.showDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .basePrice(req.basePrice())
                .totalSeats(totalSeats)
                .availableSeats(totalSeats)
                .build());

        generateSeats(show, req.seatConfig());
        return toSearchResponse(show);
    }

    

    private void generateSeats(Show show, Map<String, Integer> seatConfig) {
        char row = 'A';
        for (Map.Entry<String, Integer> entry : seatConfig.entrySet()) {
            SeatType type = SeatType.valueOf(entry.getKey().toUpperCase());
            int count = entry.getValue();
            int col = 1;
            for (int i = 0; i < count; i++) {
                seatRepository.save(Seat.builder()
                        .show(show)
                        .seatNumber(row + String.valueOf(col))
                        .seatRow(String.valueOf(row))
                        .seatType(type)
                        .status(SeatStatus.AVAILABLE)
                        .build());
                col++;
                if (col > 10) { col = 1; row++; }
            }
            row++;
        }
    }

    

    private ShowSearchResponse toSearchResponse(Show s) {
        return new ShowSearchResponse(
                s.getId(),
                s.getMovie().getTitle(),
                s.getMovie().getLanguage(),
                s.getMovie().getGenre(),
                s.getMovie().getRating(),
                s.getTheatre().getName(),
                s.getTheatre().getCity(),
                s.getTheatre().getAddress(),
                s.getShowDate(),
                s.getStartTime(),
                s.getEndTime(),
                s.getBasePrice(),
                s.getTotalSeats(),
                s.getAvailableSeats(),
                s.isAfternoonShow());
    }

    private SeatResponse toSeatResponse(Seat s) {
        return new SeatResponse(
                s.getId(),
                s.getSeatNumber(),
                s.getSeatRow(),
                s.getSeatType().name(),
                s.getStatus().name(),
                s.getSeatType().getPriceMultiplier());
    }

    private MovieResponse toMovieResponse(Movie m) {
        return new MovieResponse(m.getId(), m.getTitle(), m.getLanguage(),
                m.getGenre(), m.getDurationMinutes(), m.getRating());
    }

    private TheatreResponse toTheatreResponse(Theatre t) {
        return new TheatreResponse(t.getId(), t.getName(), t.getCity(),
                t.getAddress(), t.getTotalScreens(), t.getPartnerCode());
    }
}

