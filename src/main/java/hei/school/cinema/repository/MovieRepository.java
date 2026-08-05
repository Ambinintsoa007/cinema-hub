package hei.school.cinema.repository;

import hei.school.cinema.repository.model.GenreEntity;
import hei.school.cinema.repository.model.MovieEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<MovieEntity, UUID> {

  boolean existsByTitleIgnoreCase(String title);

  boolean existsByTitleIgnoreCaseAndIdNot(String title, UUID id);

  @Query(
      """
      SELECT m
      FROM MovieEntity m
      WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', COALESCE(:title, ''), '%'))
        AND (:genre IS NULL
             OR EXISTS (
                 SELECT 1
                 FROM MovieGenreEntity mg
                 WHERE mg.id.movieId = m.id AND mg.id.genre = :genre
             ))
      ORDER BY m.title ASC
      """)
  Page<MovieEntity> findAllFiltered(
      @Param("title") String title, @Param("genre") GenreEntity genre, Pageable pageable);

  @Query(
      value = "SELECT EXISTS (SELECT 1 FROM projections WHERE movie_id = :movieId)",
      nativeQuery = true)
  boolean existsProjectionForMovie(@Param("movieId") UUID movieId);
}
