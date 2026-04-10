package com.booking.movie_booking_platform.service;

import com.booking.movie_booking_platform.dto.BookingRequest;
import com.booking.movie_booking_platform.dto.BookingResponse;
import com.booking.movie_booking_platform.dto.TicketPriceBreakdown;
import com.booking.movie_booking_platform.entity.*;
import com.booking.movie_booking_platform.exception.InsufficientSeatsException;
import com.booking.movie_booking_platform.exception.InvalidRequestException;
import com.booking.movie_booking_platform.exception.ResourceNotFoundException;
import com.booking.movie_booking_platform.repository.BookingRepository;
import com.booking.movie_booking_platform.repository.SeatRepository;
import com.booking.movie_booking_platform.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public BookingResponse bookTickets(BookingRequest request) {
        validateRequest(request);

        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Show not found with id: " + request.showId()));

        List<Seat> available = seatRepository.findByIdInAndShowIdAndStatus(
                request.seatIds(), show.getId(), SeatStatus.AVAILABLE);

        if (available.size() != request.seatIds().size()) {
            throw new InsufficientSeatsException(
                    "Some selected seats are no longer available. Please refresh and try again.");
        }

        List<TicketPriceBreakdown> breakdown = new ArrayList<>();
        List<String> appliedOffers = new ArrayList<>();
        BigDecimal[] totals = calculatePricing(show, available, breakdown, appliedOffers);

        Booking booking = bookingRepository.save(Booking.builder()
                .show(show)
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .numberOfTickets(available.size())
                .totalAmount(totals[0])
                .discountAmount(totals[1])
                .finalAmount(totals[2])
                .status(BookingStatus.CONFIRMED)
                .build());

        for (Seat seat : available) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBooking(booking);
        }
        seatRepository.saveAll(available);

        show.setAvailableSeats(show.getAvailableSeats() - available.size());
        showRepository.save(show);

        return toResponse(booking, available, breakdown, appliedOffers);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidRequestException("Booking is already cancelled");
        }

        List<Seat> bookedSeats = seatRepository.findByShowId(booking.getShow().getId())
                .stream()
                .filter(s -> s.getBooking() != null && s.getBooking().getId().equals(bookingId))
                .collect(Collectors.toList());

        for (Seat seat : bookedSeats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setBooking(null);
        }
        seatRepository.saveAll(bookedSeats);

        Show show = booking.getShow();
        show.setAvailableSeats(show.getAvailableSeats() + bookedSeats.size());
        showRepository.save(show);

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        return toResponse(booking, bookedSeats, List.of(), List.of());
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + bookingId));

        List<Seat> seats = seatRepository.findByShowId(booking.getShow().getId())
                .stream()
                .filter(s -> s.getBooking() != null && s.getBooking().getId().equals(bookingId))
                .collect(Collectors.toList());

        List<TicketPriceBreakdown> breakdown = new ArrayList<>();
        List<String> appliedOffers = new ArrayList<>();
        if (!seats.isEmpty() && booking.getStatus() == BookingStatus.CONFIRMED) {
            calculatePricing(booking.getShow(), seats, breakdown, appliedOffers);
        }

        return toResponse(booking, seats, breakdown, appliedOffers);
    }

    private BigDecimal[] calculatePricing(Show show, List<Seat> seats,
                                           List<TicketPriceBreakdown> breakdown,
                                           List<String> appliedOffers) {
        boolean isAfternoon = show.isAfternoonShow();
        BigDecimal baseTotal = BigDecimal.ZERO;
        BigDecimal totalThirdDiscount = BigDecimal.ZERO;
        BigDecimal totalAfternoonDiscount = BigDecimal.ZERO;

        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            BigDecimal seatPrice = show.getBasePrice()
                    .multiply(BigDecimal.valueOf(seat.getSeatType().getPriceMultiplier()))
                    .setScale(2, RoundingMode.HALF_UP);
            baseTotal = baseTotal.add(seatPrice);

            BigDecimal thirdDisc = BigDecimal.ZERO;
            BigDecimal afternoonDisc = BigDecimal.ZERO;
            BigDecimal ticketFinal = seatPrice;

            if ((i + 1) % 3 == 0) {
                thirdDisc = ticketFinal.multiply(BigDecimal.valueOf(0.50))
                        .setScale(2, RoundingMode.HALF_UP);
                ticketFinal = ticketFinal.subtract(thirdDisc);
                totalThirdDiscount = totalThirdDiscount.add(thirdDisc);
            }

            if (isAfternoon) {
                afternoonDisc = ticketFinal.multiply(BigDecimal.valueOf(0.20))
                        .setScale(2, RoundingMode.HALF_UP);
                ticketFinal = ticketFinal.subtract(afternoonDisc);
                totalAfternoonDiscount = totalAfternoonDiscount.add(afternoonDisc);
            }

            breakdown.add(new TicketPriceBreakdown(
                    seat.getSeatNumber(), seat.getSeatType().name(),
                    seatPrice, thirdDisc, afternoonDisc, ticketFinal));
        }

        if (totalThirdDiscount.compareTo(BigDecimal.ZERO) > 0)
            appliedOffers.add("50% discount on every 3rd ticket");
        if (totalAfternoonDiscount.compareTo(BigDecimal.ZERO) > 0)
            appliedOffers.add("20% discount for afternoon show");

        BigDecimal totalDiscount = totalThirdDiscount.add(totalAfternoonDiscount);
        return new BigDecimal[]{ baseTotal, totalDiscount, baseTotal.subtract(totalDiscount) };
    }

    private void validateRequest(BookingRequest r) {
        if (r.showId() == null)
            throw new InvalidRequestException("Show ID is required");
        if (r.customerName() == null || r.customerName().isBlank())
            throw new InvalidRequestException("Customer name is required");
        if (r.customerEmail() == null || r.customerEmail().isBlank())
            throw new InvalidRequestException("Customer email is required");
        if (r.seatIds() == null || r.seatIds().isEmpty())
            throw new InvalidRequestException("At least one seat must be selected");
    }

    private BookingResponse toResponse(Booking b, List<Seat> seats,
                                        List<TicketPriceBreakdown> breakdown,
                                        List<String> appliedOffers) {
        return new BookingResponse(
                b.getId(),
                b.getShow().getId(),
                b.getShow().getMovie().getTitle(),
                b.getShow().getTheatre().getName(),
                b.getShow().getShowDate().toString(),
                b.getShow().getStartTime().toString(),
                b.getCustomerName(),
                b.getCustomerEmail(),
                seats.stream().map(Seat::getSeatNumber).collect(Collectors.toList()),
                b.getNumberOfTickets(),
                b.getTotalAmount(),
                b.getDiscountAmount(),
                b.getFinalAmount(),
                breakdown,
                appliedOffers,
                b.getStatus().name(),
                b.getCreatedAt());
    }
}
