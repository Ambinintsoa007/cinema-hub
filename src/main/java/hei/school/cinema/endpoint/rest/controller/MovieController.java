package hei.school.cinema.endpoint.rest.controller;

import hei.school.cinema.endpoint.rest.dto.CreateMovieRequest;
import hei.school.cinema.endpoint.rest.dto.Genre;
import hei.school.cinema.endpoint.rest.dto.MoviePageResponse;
import hei.school.cinema.endpoint.rest.dto.MovieResponse;
import hei.school.cinema.endpoint.rest.dto.UpdateMovieRequest;
import hei.school.cinema.mapper.MovieMapper;
import hei.school.cinema.model.Movie;
import hei.school.cinema.service.MovieService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

  private final MovieService movieService;
  private final MovieMapper movieMapper;

  @Override
  public ResponseEntity<MovieResponse> createMovie(CreateMovieRequest createMovieRequest) {
    Movie created =
            movieService.create(
                    createMovieRequest.getTitle(),
                    createMovieRequest.getDescription(),
                    createMovieRequest.getDuration(),
                    movieMapper.toDomainGenres(createMovieRequest.getGenres()));

    return ResponseEntity.status(201).body(movieMapper.toDto(created));
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<MovieResponse> getMovieById(@PathVariable UUID movieId) {
    return ResponseEntity.ok(movieMapper.toDto(movieService.getById(movieId)));
  }

  @GetMapping
  public ResponseEntity<MoviePageResponse> getMovies(
          @RequestParam(defaultValue = "0") Integer page,
          @RequestParam(defaultValue = "20") Integer pageSize,
          @RequestParam(required = false) String title,
          @RequestParam(required = false) Genre genre) {

    Page<Movie> movies =
            movieService.getAll(
                    title,
                    genre == null ? null : movieMapper.toDomainGenre(genre),
                    page,
                    pageSize);

    List<MovieResponse> data = movies.stream().map(movieMapper::toDto).toList();

    return ResponseEntity.ok(
            new MoviePageResponse(
                    data,
                    page,
                    pageSize,
                    movies.getTotalElements(),
                    movies.getTotalPages()));
  }

  @Override
  public ResponseEntity<MovieResponse> updateMovie(
          @PathVariable UUID movieId,
          @RequestBody UpdateMovieRequest updateMovieRequest) {

    Movie updated =
            movieService.update(
                    movieId,
                    updateMovieRequest.getTitle(),
                    updateMovieRequest.getDescription(),
                    updateMovieRequest.getDuration(),
                    movieMapper.toDomainGenres(updateMovieRequest.getGenres()));

    return ResponseEntity.ok(movieMapper.toDto(updated));
  }
}