package de.unimarburg.samplemanagement.service;

import de.unimarburg.samplemanagement.model.*;
import de.unimarburg.samplemanagement.repository.AddressStoreRepository;
import de.unimarburg.samplemanagement.repository.ReportAuthorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

    @Mock
    private AddressStoreRepository addressStoreRepository;

    @Mock
    private ReportAuthorRepository reportAuthorRepository;

    @InjectMocks
    private PdfReportService pdfReportService;

    @Test
    void testGeneratePdf() throws IOException, URISyntaxException {
        // given
        Study study = new Study();
        study.setStudyName("Test Study");
        study.setStartDate(new Date());
        study.setEndDate(new Date());
        study.setMethodValidation("Test Validation");
        study.setQualityControl("Test QC");
        study.setRemarks("Test Remarks");
        study.setListOfSubjects(new ArrayList<>());
        study.setSampleDeliveryList(new ArrayList<>());

        List<AnalysisType> selectedAnalysisTypes = new ArrayList<>();
        AnalysisType analysisType = new AnalysisType();
        analysisType.setAnalysisName("Test Analysis");
        analysisType.setAnalysisUnit("U/ml");
        analysisType.setAnalysisDescription("Description");
        selectedAnalysisTypes.add(analysisType);

        List<SampleDelivery> selectedDeliveries = new ArrayList<>();
        SampleDelivery delivery = new SampleDelivery();
        delivery.setStudy(study);
        delivery.setDeliveryDate(new Date());
        delivery.setSamples(new ArrayList<>());
        selectedDeliveries.add(delivery);
        study.getSampleDeliveryList().add(delivery);

        ReportAuthor author = new ReportAuthor();
        author.setName("Dr. Test");
        author.setTitle("Tester");
        List<ReportAuthor> authors = new ArrayList<>();
        authors.add(author);
        when(reportAuthorRepository.findAll()).thenReturn(authors);
        when(addressStoreRepository.getOwnAddress()).thenReturn("Test Address");

        // when
        ByteArrayInputStream pdfStream = pdfReportService.generatePdf(study, "Test Sender", selectedAnalysisTypes, selectedDeliveries);

        // then
        assertThat(pdfStream).isNotNull();
        assertThat(pdfStream.available()).isGreaterThan(0);
    }
}
