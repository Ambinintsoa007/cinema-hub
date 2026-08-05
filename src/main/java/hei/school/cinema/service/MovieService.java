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

    updateGenres(entity, genres);

    MovieEntity saved = movieRepository.save(entity);
    return movieMapper.toDomain(saved);
  }

  private MovieEntity findEntity(UUID movieId) {
    return movieRepository
        .findById(movieId)
        .orElseThrow(() -> ApiException.notFound("Movie not found: " + movieId));
  }

  private String requireNotBlank(String value, String message) {
    String trimmedValue = value == null ? "" : value.trim();

    if (trimmedValue.isEmpty()) {
      throw ApiException.badRequest(message);
    }

    return trimmedValue;
  }

  private void requireGenres(Set<Genre> genres) {
    if (genres == null || genres.isEmpty()) {
      throw ApiException.badRequest("Movie must have at least one genre");
    }
  }

  private Duration requireValidDuration(String rawDuration) {
    if (rawDuration == null || rawDuration.isBlank()) {
      throw ApiException.badRequest("Movie duration must not be blank");
    }

    Duration parsedDuration;

    try {
      parsedDuration = Duration.parse(rawDuration);
    } catch (DateTimeParseException e) {
      throw ApiException.badRequest("Invalid movie duration format: " + rawDuration);
    }

    if (parsedDuration.getNano() != 0) {
      throw ApiException.badRequest("Movie duration must be a whole number of seconds");
    }

    if (parsedDuration.toSeconds() < 1) {
      throw ApiException.badRequest("Movie duration must be at least one second");
    }

    return parsedDuration;
  }

  private void updateGenres(MovieEntity movie, Set<Genre> genres) {
    Set<GenreEntity> requestedGenres =
        genres.stream().map(movieMapper::toEntityGenre).collect(Collectors.toSet());

    movie
        .getGenres()
        .removeIf(movieGenre -> !requestedGenres.contains(movieGenre.getId().getGenre()));

    Set<GenreEntity> existingGenres =
        movie.getGenres().stream()
            .map(movieGenre -> movieGenre.getId().getGenre())
            .collect(Collectors.toSet());
    requestedGenres.stream()
        .filter(genre -> !existingGenres.contains(genre))
        .map(
            genre ->
                MovieGenreEntity.builder()
                    .id(new MovieGenreEntity.MovieGenreId(movie.getId(), genre))
                    .movie(movie)
                    .build())
        .forEach(movie.getGenres()::add);
  }
}
