package de.unimarburg.samplemanagement.repository;

import de.unimarburg.samplemanagement.model.SampleDelivery;
import de.unimarburg.samplemanagement.model.Study;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SampleDeliveryRepository extends JpaRepository<SampleDelivery, Long> {
    Optional<SampleDelivery> findByStudyAndBoxNumber(Study study, String boxNumber);

}
