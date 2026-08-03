package hei.school.cinema.service;

import hei.school.cinema.exception.ApiException;
import hei.school.cinema.mapper.MovieMapper;
import hei.school.cinema.model.Genre;
import hei.school.cinema.model.Movie;
import hei.school.cinema.repository.MovieRepository;
import hei.school.cinema.repository.model.GenreEntity;
import hei.school.cinema.repository.model.MovieEntity;
import hei.school.cinema.repository.model.MovieGenreEntity;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovieService {

  private final MovieRepository movieRepository;
  private final MovieMapper movieMapper;

  @Transactional
  public Movie create(String title, String description, String duration, Set<Genre> genres) {
    String normalizedTitle = requireNotBlank(title, "Movie title must not be blank");
    String normalizedDescription =
        requireNotBlank(description, "Movie description must not be blank");
    requireGenres(genres);
    Duration movieDuration = requireValidDuration(duration);
    if (movieRepository.existsByTitleIgnoreCase(normalizedTitle)) {
      throw ApiException.conflict("Movie title already exists: " + normalizedTitle);
    }
    Movie movie =
        Movie.builder()
            .id(UUID.randomUUID())
            .title(normalizedTitle)
            .description(normalizedDescription)
            .duration(movieDuration)
            .genres(genres)
            .build();
    MovieEntity saved = movieRepository.save(movieMapper.toEntity(movie));
    return movieMapper.toDomain(saved);
  }

  @Transactional(readOnly = true)
  public Movie getById(UUID movieId) {
    return movieMapper.toDomain(findEntity(movieId));
  }

  @Transactional(readOnly = true)
  public Page<Movie> getAll(String title, Genre genre, int page, int pageSize) {
    GenreEntity genreEntity = genre == null ? null : movieMapper.toEntityGenre(genre);
    return movieRepository
        .findAllFiltered(title, genreEntity, PageRequest.of(page, pageSize))
        .map(movieMapper::toDomain);
  }

  @Transactional
  public Movie update(
      UUID movieId, String title, String description, String duration, Set<Genre> genres) {
    MovieEntity entity = findEntity(movieId);
    String normalizedTitle = requireNotBlank(title, "Movie title must not be blank");
    String normalizedDescription =
        requireNotBlank(description, "Movie description must not be blank");
    requireGenres(genres);
    Duration newDuration = requireValidDuration(duration);
    if (!entity.getTitle().equalsIgnoreCase(normalizedTitle)
        && movieRepository.existsByTitleIgnoreCaseAndIdNot(normalizedTitle, movieId)) {
      throw ApiException.conflict("Movie title already exists: " + normalizedTitle);
    }
    if (newDuration.toSeconds() != entity.getDurationSeconds()
        && movieRepository.existsProjectionForMovie(movieId)) {
      throw ApiException.conflict(
          "Movie duration cannot be changed because it already has projections");
    }
    entity.setTitle(normalizedTitle);
    entity.setDescription(normalizedDescription);
    entity.setDurationSeconds(newDuration.toSeconds());
    Set<MovieGenreEntity> newGenres = toGenreEntities(entity, genres);
    entity.getGenres().clear();
    entity.getGenres().addAll(newGenres);
    return movieMapper.toDomain(movieRepository.save(entity));
  }

  private MovieEntity findEntity(UUID movieId) {
    return movieRepository
        .findById(movieId)
        .orElseThrow(() -> ApiException.notFound("Movie not found: " + movieId));
  }

  private String requireNotBlank(String value, String message) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.isEmpty()) {
      throw ApiException.badRequest(message);
    }
    return trimmed;
  }

  private void requireGenres(Set<Genre> genres) {
    if (genres == null || genres.isEmpty()) {
      throw ApiException.badRequest("Movie must have at least one genre");
    }
  }

  private Duration requireValidDuration(String rawDuration) {
    Duration parsed;
    try {
      parsed = Duration.parse(rawDuration);
    } catch (DateTimeParseException e) {
      throw ApiException.badRequest("Invalid movie duration format: " + rawDuration);
    }
    if (parsed.toSeconds() < 1) {
      throw ApiException.badRequest("Movie duration must be at least one second");
    }
    return parsed;
  }

  private Set<MovieGenreEntity> toGenreEntities(MovieEntity movie, Set<Genre> genres) {
    return genres.stream()
        .map(
            genre ->
                MovieGenreEntity.builder()
                    .id(
                        new MovieGenreEntity.MovieGenreId(
                            movie.getId(), movieMapper.toEntityGenre(genre)))
                    .movie(movie)
                    .build())
        .collect(Collectors.toSet());
  }
}
