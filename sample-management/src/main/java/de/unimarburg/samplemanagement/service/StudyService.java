package de.unimarburg.samplemanagement.service;

import de.unimarburg.samplemanagement.model.AnalysisType;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.repository.AnalysisTypeRepository;
import de.unimarburg.samplemanagement.repository.StudyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudyService {

    private final StudyRepository studyRepository;
    private final AnalysisTypeRepository analysisTypeRepository;

    public StudyService(StudyRepository studyRepository, AnalysisTypeRepository analysisTypeRepository) {
        this.studyRepository = studyRepository;
        this.analysisTypeRepository = analysisTypeRepository;
    }

    public List<Study> findAll() {
        return studyRepository.findAll();
    }

    public void delete(Study study) {
        studyRepository.delete(study);
    }

    public void save(Study study) {
        if (study == null) {
            throw new IllegalArgumentException("Study cannot be null");
        }
        studyRepository.save(study);
    }

    public List<Study> findByNameAndBirthDate(String nameFilter, String dateFilter) {
        boolean hasName = nameFilter != null && !nameFilter.isEmpty();
        boolean hasDate = dateFilter != null && !dateFilter.isEmpty();

        if (!hasName && hasDate) {
            return studyRepository.searchDate(dateFilter);
        } else if (hasName && !hasDate) {
            return studyRepository.searchName(nameFilter);
        } else {
            return studyRepository.findAll();
        }
    }

    public Study getStudyById(Long id) {
        return studyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Study not found with id: " + id));
    }

    // --------------------------
    // AnalysisType management
    // --------------------------

    @Transactional
    public void addAnalysisToStudy(Long studyId, Long analysisTypeId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new EntityNotFoundException("Study not found"));
        AnalysisType analysisType = analysisTypeRepository.findById(analysisTypeId)
                .orElseThrow(() -> new EntityNotFoundException("AnalysisType not found"));

        study.getAnalysisTypes().add(analysisType);
        studyRepository.save(study); // ensures join table is updated
    }

    @Transactional
    public void removeAnalysisFromStudy(Long studyId, Long analysisTypeId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new EntityNotFoundException("Study not found"));
        AnalysisType analysisType = analysisTypeRepository.findById(analysisTypeId)
                .orElseThrow(() -> new EntityNotFoundException("AnalysisType not found"));

        study.getAnalysisTypes().remove(analysisType);
        studyRepository.save(study); // ensures join table is updated
    }
}
