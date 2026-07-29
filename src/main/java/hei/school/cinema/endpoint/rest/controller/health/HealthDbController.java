package hei.school.cinema.endpoint.rest.controller.health;

import static hei.school.cinema.endpoint.rest.controller.health.PingController.KO;
import static hei.school.cinema.endpoint.rest.controller.health.PingController.OK;

import hei.school.cinema.PojaGenerated;
import hei.school.cinema.repository.DummyRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@PojaGenerated
@RestController
@AllArgsConstructor
public class HealthDbController {

  DummyRepository dummyRepository;

  @GetMapping("/health/db")
  public ResponseEntity<String> dummyTable_should_not_be_empty() {
    return dummyRepository.findAll().isEmpty() ? KO : OK;
  }
}
