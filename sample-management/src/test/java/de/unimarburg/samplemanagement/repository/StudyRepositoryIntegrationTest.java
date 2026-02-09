package de.unimarburg.samplemanagement.repository;

import de.unimarburg.samplemanagement.model.Study;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class StudyRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudyRepository studyRepository;

    @Test
    public void whenFindByName_thenReturnStudy() {
        // given
        Study study = new Study();
        study.setStudyName("Test Study");
        entityManager.persist(study);
        entityManager.flush();

        // when
        Study found = studyRepository.findByStudyName(study.getStudyName());

        // then
        assertThat(found.getStudyName())
                .isEqualTo(study.getStudyName());
    }
}
