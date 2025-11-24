package de.unimarburg.samplemanagement.service;

import de.unimarburg.samplemanagement.model.Sample;
import de.unimarburg.samplemanagement.model.SampleDelivery;
import de.unimarburg.samplemanagement.model.Subject;
import de.unimarburg.samplemanagement.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SampleService {

    private final SampleRepository sampleRepository;

    @Autowired
    public SampleService(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    public List<Sample> findAll() {
        return sampleRepository.findAll();
    }

    public void save(Sample sample) {
        sampleRepository.save(sample);
    }

    @Transactional
    public void deleteSample(Long sampleId) {
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("Sample not found with id: " + sampleId));

        if (sample.getListOfAnalysis() != null && !sample.getListOfAnalysis().isEmpty()) {
            throw new IllegalStateException("This sample cannot be deleted because it has associated analyses.");
        }

        // Remove the sample from the parent entities' collections
        SampleDelivery delivery = sample.getSampleDelivery();
        if (delivery != null) {
            delivery.getSamples().remove(sample);
        }

        Subject subject = sample.getSubject();
        if (subject != null) {
            subject.getListOfSamples().remove(sample);
        }

        sampleRepository.delete(sample);
    }
}