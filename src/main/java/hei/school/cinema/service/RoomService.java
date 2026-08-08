package hei.school.cinema.service;

import hei.school.cinema.exception.ApiException;
import hei.school.cinema.mapper.RoomMapper;
import hei.school.cinema.model.Room;
import hei.school.cinema.model.Seat;
import hei.school.cinema.repository.RoomRepository;
import hei.school.cinema.repository.SeatRepository;
import hei.school.cinema.repository.model.RoomEntity;
import hei.school.cinema.repository.model.SeatEntity;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

  private final RoomRepository roomRepository;
  private final SeatRepository seatRepository;
  private final RoomMapper roomMapper;

  @Transactional
  public Room create(String number, int rows, int seatsPerRow) {
    String normalizedNumber = requireNotBlank(number, "Room number must not be blank");
    requirePositive(rows, "Room rows must be at least one");
    requirePositive(seatsPerRow, "Room seats-per-row must be at least one");
    if (roomRepository.existsByNumberIgnoreCase(normalizedNumber)) {
      throw ApiException.conflict("Room number already exists: " + normalizedNumber);
    }
    Room room =
        Room.builder()
            .id(UUID.randomUUID())
            .number(normalizedNumber)
            .capacity(rows * seatsPerRow)
            .build();
    RoomEntity entity = roomMapper.toEntity(room);
    entity.setSeats(generateSeats(entity, rows, seatsPerRow));
    return roomMapper.toDomain(roomRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public Room getById(UUID roomId) {
    return roomMapper.toDomain(findEntity(roomId));
  }

  @Transactional(readOnly = true)
  public Page<Room> getAll(int page, int pageSize) {
    return roomRepository
        .findAllByOrderByNumberAsc(PageRequest.of(page, pageSize))
        .map(roomMapper::toDomain);
  }

  @Transactional(readOnly = true)
  public List<Seat> getSeats(UUID roomId) {
    findEntity(roomId);
    return seatRepository.findByRoom_Id(roomId).stream()
        .sorted(
            Comparator.comparingInt((SeatEntity seat) -> seat.getNumber().charAt(0))
                .thenComparingInt(seat -> Integer.parseInt(seat.getNumber().substring(1))))
        .map(roomMapper::toDomain)
        .toList();
  }

  private RoomEntity findEntity(UUID roomId) {
    return roomRepository
        .findById(roomId)
        .orElseThrow(() -> ApiException.notFound("Room not found: " + roomId));
  }

  private Set<SeatEntity> generateSeats(RoomEntity room, int rows, int seatsPerRow) {
    Set<SeatEntity> seats = new HashSet<>();
    for (int row = 0; row < rows; row++) {
      char rowLetter = (char) ('A' + row);
      for (int seat = 1; seat <= seatsPerRow; seat++) {
        seats.add(
            SeatEntity.builder()
                .id(UUID.randomUUID())
                .number("" + rowLetter + seat)
                .room(room)
                .build());
      }
    }
    return seats;
  }

  private String requireNotBlank(String value, String message) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.isEmpty()) {
      throw ApiException.badRequest(message);
    }
    return trimmed;
  }

  private void requirePositive(int value, String message) {
    if (value < 1) {
      throw ApiException.badRequest(message);
    }
  }
}
