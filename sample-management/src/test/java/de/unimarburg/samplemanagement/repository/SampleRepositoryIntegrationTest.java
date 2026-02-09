package de.unimarburg.samplemanagement.repository;

import de.unimarburg.samplemanagement.model.Sample;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.model.SampleDelivery;
import de.unimarburg.samplemanagement.model.Subject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class SampleRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SampleRepository sampleRepository;

    @Test
    public void whenSaveSample_thenCanBeFound() {
        // given
        Study study = new Study();
        study.setStudyName("Test Study");
        entityManager.persist(study);

        SampleDelivery delivery = new SampleDelivery();
        delivery.setStudy(study);
        entityManager.persist(delivery);
        
        Subject subject = new Subject();
        subject.setStudy(study);
        subject.setAlias(123L);
        entityManager.persist(subject);

        Sample sample = new Sample();
        sample.setSample_barcode("S1");
        sample.setStudy(study);
        sample.setSampleDelivery(delivery);
        sample.setSubject(subject);
        entityManager.persist(sample);
        entityManager.flush();

        // when
        Optional<Sample> found = sampleRepository.findById(sample.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSample_barcode()).isEqualTo("S1");
        assertThat(found.get().getStudy().getStudyName()).isEqualTo("Test Study");
        assertThat(found.get().getSampleDelivery().getId()).isEqualTo(delivery.getId());
        assertThat(found.get().getSubject().getAlias()).isEqualTo(123L);
    }

    @Test
    public void whenUpdateSample_thenIsUpdated() {
        // given
        Study study = new Study();
        study.setStudyName("Test Study");
        entityManager.persist(study);

        SampleDelivery delivery = new SampleDelivery();
        delivery.setStudy(study);
        entityManager.persist(delivery);
        
        Subject subject = new Subject();
        subject.setStudy(study);
        subject.setAlias(123L);
        entityManager.persist(subject);

        Sample sample = new Sample();
        sample.setSample_barcode("S1");
        sample.setStudy(study);
        sample.setSampleDelivery(delivery);
        sample.setSubject(subject);
        entityManager.persist(sample);
        entityManager.flush();

        // when
        Sample savedSample = sampleRepository.findById(sample.getId()).get();
        savedSample.setSample_barcode("S2");
        sampleRepository.save(savedSample);

        // then
        Sample updatedSample = sampleRepository.findById(sample.getId()).get();
        assertThat(updatedSample.getSample_barcode()).isEqualTo("S2");
    }
}
