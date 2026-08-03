package hei.school.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import hei.school.cinema.exception.ApiException;
import hei.school.cinema.mapper.MovieMapper;
import hei.school.cinema.model.Genre;
import hei.school.cinema.model.Movie;
import hei.school.cinema.repository.MovieRepository;
import hei.school.cinema.repository.model.GenreEntity;
import hei.school.cinema.repository.model.MovieEntity;
import hei.school.cinema.repository.model.MovieGenreEntity;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

  @Mock private MovieRepository movieRepository;

  private MovieService movieService;

  private static final UUID MOVIE_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");

  @BeforeEach
  void setUp() {
    movieService = new MovieService(movieRepository, new MovieMapper());
  }

  private static final Set<Genre> ACTION_SCI_FI = Set.of(Genre.ACTION, Genre.SCI_FI);

  @Test
  void create_should_return_created_movie() {
    when(movieRepository.existsByTitleIgnoreCase("Inception")).thenReturn(false);
    when(movieRepository.save(any(MovieEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Movie created =
        movieService.create("Inception", "A dream within a dream", "PT2H15M", ACTION_SCI_FI);

    assertEquals("Inception", created.getTitle());
    assertEquals(java.time.Duration.ofHours(2).plusMinutes(15), created.getDuration());
    assertThat(created.getGenres()).containsExactlyInAnyOrder(Genre.ACTION, Genre.SCI_FI);
  }

  @Test
  void create_should_trim_title_and_description() {
    when(movieRepository.existsByTitleIgnoreCase("Inception")).thenReturn(false);
    when(movieRepository.save(any(MovieEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Movie created = movieService.create("  Inception  ", "  A dream  ", "PT2H15M", ACTION_SCI_FI);

    assertEquals("Inception", created.getTitle());
    assertEquals("A dream", created.getDescription());
  }

  @Test
  void create_should_reject_blank_title() {
    ApiException exception =
        assertThrows(
            ApiException.class, () -> movieService.create("   ", "desc", "PT1H", ACTION_SCI_FI));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_null_title() {
    ApiException exception =
        assertThrows(
            ApiException.class, () -> movieService.create(null, "desc", "PT1H", ACTION_SCI_FI));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_blank_description() {
    ApiException exception =
        assertThrows(
            ApiException.class, () -> movieService.create("Movie", "  ", "PT1H", ACTION_SCI_FI));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_empty_genres() {
    ApiException exception =
        assertThrows(
            ApiException.class, () -> movieService.create("Movie", "desc", "PT1H", Set.of()));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_duplicate_title_case_insensitive() {
    when(movieRepository.existsByTitleIgnoreCase("inception")).thenReturn(true);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> movieService.create("inception", "desc", "PT1H", Set.of(Genre.DRAMA)));

    assertEquals(409, exception.getStatus().value());
  }

  @Test
  void create_should_reject_invalid_duration_format() {
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> movieService.create("Movie", "desc", "not-a-duration", Set.of(Genre.DRAMA)));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_zero_duration() {
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> movieService.create("Movie", "desc", "PT0S", Set.of(Genre.DRAMA)));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_reject_duration_below_one_second() {
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> movieService.create("Movie", "desc", "PT0.5S", Set.of(Genre.DRAMA)));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void create_should_accept_duration_of_exactly_one_second() {
    when(movieRepository.existsByTitleIgnoreCase("Movie")).thenReturn(false);
    when(movieRepository.save(any(MovieEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Movie created = movieService.create("Movie", "desc", "PT1S", Set.of(Genre.DRAMA));

    assertEquals(java.time.Duration.ofSeconds(1), created.getDuration());
  }

  @Test
  void getById_should_return_movie_when_found() {
    when(movieRepository.findById(MOVIE_ID))
        .thenReturn(Optional.of(entityOf(MOVIE_ID, "Inception", 8100L)));

    Movie movie = movieService.getById(MOVIE_ID);

    assertEquals(MOVIE_ID, movie.getId());
    assertEquals("Inception", movie.getTitle());
    assertThat(movie.getGenres()).containsExactlyInAnyOrder(Genre.ACTION, Genre.SCI_FI);
  }

  @Test
  void getById_should_return_404_when_not_found() {
    when(movieRepository.findById(MOVIE_ID)).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class, () -> movieService.getById(MOVIE_ID));

    assertEquals(404, exception.getStatus().value());
  }

  @Test
  void update_should_modify_movie() {
    MovieEntity existing = entityOf(MOVIE_ID, "Inception", 8100L);
    when(movieRepository.findById(MOVIE_ID)).thenReturn(Optional.of(existing));
    when(movieRepository.save(any(MovieEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Movie updated =
        movieService.update(
            MOVIE_ID, "Inception 2", "Sequel", "PT2H30M", Set.of(Genre.ACTION, Genre.THRILLER));

    assertEquals("Inception 2", updated.getTitle());
    assertEquals(java.time.Duration.ofHours(2).plusMinutes(30), updated.getDuration());
    assertThat(updated.getGenres()).containsExactlyInAnyOrder(Genre.ACTION, Genre.THRILLER);
  }

  @Test
  void update_should_keep_same_title_without_conflict() {
    MovieEntity existing = entityOf(MOVIE_ID, "Inception", 8100L);
    when(movieRepository.findById(MOVIE_ID)).thenReturn(Optional.of(existing));
    when(movieRepository.save(any(MovieEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Movie updated =
        movieService.update(MOVIE_ID, "inception", "Desc", "PT2H15M", Set.of(Genre.ACTION));

    assertEquals("inception", updated.getTitle());
  }

  @Test
  void update_should_reject_duplicate_title_case_insensitive() {
    when(movieRepository.findById(MOVIE_ID))
        .thenReturn(Optional.of(entityOf(MOVIE_ID, "Inception", 8100L)));
    when(movieRepository.existsByTitleIgnoreCaseAndIdNot("Titanic", MOVIE_ID)).thenReturn(true);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> movieService.update(MOVIE_ID, "Titanic", "Desc", "PT2H15M", Set.of(Genre.DRAMA)));

    assertEquals(409, exception.getStatus().value());
  }

  @Test
  void update_should_reject_duration_change_when_movie_has_projections() {
    MovieEntity existing = entityOf(MOVIE_ID, "Inception", 8100L);
    when(movieRepository.findById(MOVIE_ID)).thenReturn(Optional.of(existing));
    when(movieRepository.existsProjectionForMovie(MOVIE_ID)).thenReturn(true);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> movieService.update(MOVIE_ID, "Inception", "Desc", "PT3H", Set.of(Genre.ACTION)));

    assertEquals(409, exception.getStatus().value());
  }

  @Test
  void update_should_allow_duration_change_without_projections() {
    MovieEntity existing = entityOf(MOVIE_ID, "Inception", 8100L);
    when(movieRepository.findById(MOVIE_ID)).thenReturn(Optional.of(existing));
    when(movieRepository.save(any(MovieEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Movie updated =
        movieService.update(MOVIE_ID, "Inception", "Desc", "PT3H", Set.of(Genre.ACTION));

    assertEquals(java.time.Duration.ofHours(3), updated.getDuration());
  }

  @Test
  void update_should_reject_blank_title() {
    when(movieRepository.findById(MOVIE_ID))
        .thenReturn(Optional.of(entityOf(MOVIE_ID, "Inception", 8100L)));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> movieService.update(MOVIE_ID, "  ", "Desc", "PT2H", Set.of(Genre.ACTION)));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void update_should_reject_duration_below_one_second() {
    when(movieRepository.findById(MOVIE_ID))
        .thenReturn(Optional.of(entityOf(MOVIE_ID, "Inception", 8100L)));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                movieService.update(MOVIE_ID, "Inception", "Desc", "PT0.5S", Set.of(Genre.ACTION)));

    assertEquals(400, exception.getStatus().value());
  }

  @Test
  void update_should_return_404_when_movie_not_found() {
    when(movieRepository.findById(MOVIE_ID)).thenReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> movieService.update(MOVIE_ID, "Inception", "Desc", "PT2H", Set.of(Genre.ACTION)));

    assertEquals(404, exception.getStatus().value());
  }

  private MovieEntity entityOf(UUID id, String title, long durationSeconds) {
    MovieEntity entity =
        MovieEntity.builder()
            .id(id)
            .title(title)
            .description("desc")
            .durationSeconds(durationSeconds)
            .build();
    entity.setGenres(
        new java.util.HashSet<>(
            Set.of(
                MovieGenreEntity.builder()
                    .id(new MovieGenreEntity.MovieGenreId(id, GenreEntity.ACTION))
                    .movie(entity)
                    .build(),
                MovieGenreEntity.builder()
                    .id(new MovieGenreEntity.MovieGenreId(id, GenreEntity.SCI_FI))
                    .movie(entity)
                    .build())));
    return entity;
  }
}
