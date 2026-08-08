package hei.school.cinema.repository;

import hei.school.cinema.repository.model.RoomEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

  boolean existsByNumberIgnoreCase(String number);

  Page<RoomEntity> findAllByOrderByNumberAsc(Pageable pageable);
}
