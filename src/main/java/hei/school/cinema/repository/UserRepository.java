package hei.school.cinema.repository;

import hei.school.cinema.repository.model.UserEntity;
import hei.school.cinema.repository.model.UserRoleEntity;
import hei.school.cinema.repository.model.UserStatusEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

  boolean existsByEmailIgnoreCase(String email);

  Optional<UserEntity> findByEmailIgnoreCase(String email);

  long countByRoleAndStatus(UserRoleEntity role, UserStatusEntity status);

  @Query(
      """
      SELECT u
      FROM UserEntity u
      WHERE (:role IS NULL OR u.role = :role)
        AND (:status IS NULL OR u.status = :status)
      ORDER BY u.lastName ASC, u.firstName ASC
      """)
  Page<UserEntity> findAllFiltered(
      @Param("role") UserRoleEntity role,
      @Param("status") UserStatusEntity status,
      Pageable pageable);
}
