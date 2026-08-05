package hei.school.cinema.repository;

import hei.school.cinema.repository.model.SeatEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, UUID> {

  List<SeatEntity> findByRoom_IdOrderByNumberAsc(UUID roomId);
}
