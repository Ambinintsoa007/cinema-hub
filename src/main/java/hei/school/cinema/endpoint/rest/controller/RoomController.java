package hei.school.cinema.endpoint.rest.controller;

import hei.school.cinema.endpoint.rest.dto.CreateRoomRequest;
import hei.school.cinema.endpoint.rest.dto.RoomPageResponse;
import hei.school.cinema.endpoint.rest.dto.RoomResponse;
import hei.school.cinema.endpoint.rest.dto.SeatResponse;
import hei.school.cinema.mapper.RoomMapper;
import hei.school.cinema.model.Room;
import hei.school.cinema.service.RoomService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;
  private final RoomMapper roomMapper;

  @PostMapping
  public ResponseEntity<RoomResponse> createRoom(@RequestBody CreateRoomRequest createRoomRequest) {
    Room created =
        roomService.create(
            createRoomRequest.getNumber(),
            createRoomRequest.getRows(),
            createRoomRequest.getSeatsPerRow());
    return ResponseEntity.status(201).body(roomMapper.toDto(created));
  }

  @GetMapping("/{roomId}")
  public ResponseEntity<RoomResponse> getRoomById(@PathVariable UUID roomId) {
    return ResponseEntity.ok(roomMapper.toDto(roomService.getById(roomId)));
  }

  @GetMapping
  public ResponseEntity<RoomPageResponse> getRooms(
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "20") Integer pageSize) {

    Page<Room> rooms = roomService.getAll(page, pageSize);
    List<RoomResponse> data = rooms.stream().map(roomMapper::toDto).toList();

    return ResponseEntity.ok(
        new RoomPageResponse(
            data, page, pageSize, rooms.getTotalElements(), rooms.getTotalPages()));
  }

  @GetMapping("/{roomId}/seats")
  public ResponseEntity<List<SeatResponse>> getRoomSeats(@PathVariable UUID roomId) {
    List<SeatResponse> seats =
        roomService.getSeats(roomId).stream().map(roomMapper::toDto).toList();
    return ResponseEntity.ok(seats);
  }
}
