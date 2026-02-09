package de.unimarburg.samplemanagement.repository;

import de.unimarburg.samplemanagement.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AnalysisRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Test
    public void whenSaveAnalysis_thenCanBeFound() {
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

        AnalysisType analysisType = new AnalysisType();
        analysisType.setAnalysisName("Test Analysis");
        entityManager.persist(analysisType);

        Analysis analysis = new Analysis();
        analysis.setSample(sample);
        analysis.setAnalysisType(analysisType);
        analysis.setAnalysisResult("Positive");
        entityManager.persist(analysis);
        entityManager.flush();

        // when
        Optional<Analysis> found = analysisRepository.findById(analysis.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAnalysisResult()).isEqualTo("Positive");
        assertThat(found.get().getSample().getSample_barcode()).isEqualTo("S1");
        assertThat(found.get().getAnalysisType().getAnalysisName()).isEqualTo("Test Analysis");
    }
}
