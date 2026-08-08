package hei.school.cinema.endpoint.rest.controller;

import hei.school.cinema.gen.api.MoviesApi;
import hei.school.cinema.gen.model.CreateMovieRequest;
import hei.school.cinema.gen.model.Genre;
import hei.school.cinema.gen.model.MoviePageResponse;
import hei.school.cinema.gen.model.MovieResponse;
import hei.school.cinema.gen.model.UpdateMovieRequest;
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
@RequiredArgsConstructor
public class MovieController implements MoviesApi {

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

  @Override
  public ResponseEntity<MovieResponse> getMovieById(UUID movieId) {
    return ResponseEntity.ok(movieMapper.toDto(movieService.getById(movieId)));
  }

  @Override
  public ResponseEntity<MoviePageResponse> getMovies(
      Integer page, Integer pageSize, String title, Genre genre) {
    Page<Movie> movies =
        movieService.getAll(
            title, genre == null ? null : movieMapper.toDomainGenre(genre), page, pageSize);
    List<MovieResponse> data = movies.stream().map(movieMapper::toDto).toList();
    MoviePageResponse response =
        new MoviePageResponse(
            data, page, pageSize, movies.getTotalElements(), movies.getTotalPages());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<MovieResponse> updateMovie(
      UUID movieId, UpdateMovieRequest updateMovieRequest) {
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
