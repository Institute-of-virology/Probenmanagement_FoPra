package de.unimarburg.samplemanagement.service;

import de.unimarburg.samplemanagement.model.AnalysisType;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.repository.AnalysisTypeRepository;
import de.unimarburg.samplemanagement.repository.StudyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StudyServiceIntegrationTest {

    @Autowired
    private StudyService studyService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private AnalysisTypeRepository analysisTypeRepository;

    @Test
    public void testAddAndRemoveAnalysisFromStudy() {
        // given
        Study study = new Study();
        study.setStudyName("Test Study");
        study = studyRepository.save(study);

        AnalysisType analysisType = new AnalysisType();
        analysisType.setAnalysisName("Test Analysis");
        analysisType = analysisTypeRepository.save(analysisType);

        // when
        studyService.addAnalysisToStudy(study.getId(), analysisType.getId());

        // then
        Study updatedStudy = studyRepository.findById(study.getId()).get();
        assertThat(updatedStudy.getAnalysisTypes()).hasSize(1);
        assertThat(updatedStudy.getAnalysisTypes()).contains(analysisType);

        // when
        studyService.removeAnalysisFromStudy(study.getId(), analysisType.getId());

        // then
        updatedStudy = studyRepository.findById(study.getId()).get();
        assertThat(updatedStudy.getAnalysisTypes()).isEmpty();
    }

    @Test
    public void testFindByNameAndBirthDate() {
        // given
        Study study1 = new Study();
        study1.setStudyName("Study One");
        study1.setStartDate(new java.util.Date(120, 0, 1)); // 2020-01-01
        studyRepository.save(study1);

        Study study2 = new Study();
        study2.setStudyName("Study Two");
        study2.setStartDate(new java.util.Date(121, 1, 2)); // 2021-02-02
        studyRepository.save(study2);

        // when
        List<Study> foundByName = studyService.findByNameAndBirthDate("One", "");
        List<Study> foundByDate = studyService.findByNameAndBirthDate("", "2021");
        List<Study> foundAll = studyService.findByNameAndBirthDate("", "");

        // then
        assertThat(foundByName).hasSize(1);
        assertThat(foundByName.get(0).getStudyName()).isEqualTo("Study One");

        assertThat(foundByDate).hasSize(1);
        assertThat(foundByDate.get(0).getStudyName()).isEqualTo("Study Two");
        
        assertThat(foundAll).hasSize(2);
    }
}
