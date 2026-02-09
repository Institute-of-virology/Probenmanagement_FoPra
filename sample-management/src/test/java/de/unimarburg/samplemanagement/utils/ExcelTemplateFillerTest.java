package de.unimarburg.samplemanagement.utils;

import de.unimarburg.samplemanagement.model.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelTemplateFillerTest {

    private Study study;
    private ArrayList<Sample> samples;
    private Map<String, String> data;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        study = new Study();
        study.setStudyName("Test Study");
        study.setSampleDeliveryList(new ArrayList<>());
        study.setListOfSubjects(new ArrayList<>());

        SampleDelivery delivery1 = new SampleDelivery();
        delivery1.setId(1L);
        delivery1.setStudy(study);
        study.getSampleDeliveryList().add(delivery1);

        Subject subject1 = new Subject();
        subject1.setStudy(study);
        subject1.setListOfSamples(new ArrayList<>());
        study.getListOfSubjects().add(subject1);

        Sample sample1 = new Sample();
        sample1.setId(1L);
        sample1.setSample_barcode("BC1");
        sample1.setCoordinates("A1");
        sample1.setSample_type("Blood");
        sample1.setSample_amount("1ml");
        sample1.setSampleDelivery(delivery1);
        sample1.setSubject(subject1);
        subject1.getListOfSamples().add(sample1);
        
        ArrayList<Sample> sampleList1 = new ArrayList<>();
        sampleList1.add(sample1);
        delivery1.setSamples(sampleList1);

        AnalysisType analysisType1 = new AnalysisType();
        analysisType1.setAnalysisName("Test Assay");
        
        Analysis analysis1 = new Analysis();
        analysis1.setAnalysisType(analysisType1);
        analysis1.setAnalysisResult("Positive");
        
        List<Analysis> analyses1 = new ArrayList<>();
        analyses1.add(analysis1);
        sample1.setListOfAnalysis(analyses1);

        samples = new ArrayList<>();
        samples.add(sample1);

        date = LocalDate.of(2025, 12, 8);

        data = new HashMap<>();
        data.put("operatorName", "Test Operator");
        data.put("calculatorName", "Test Calculator");
        data.put("protocolName", "Test Protocol");
        data.put("freeTextField", "Free Text");
        data.put("nr", "42");
        data.put("assay", "Test Assay");
    }

    private InputStream createDummyTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Test Sheet");
            for (int i = 0; i <= 11; i++) {
                sheet.createRow(i);
                for (int j = 0; j <= 6; j++) {
                    sheet.getRow(i).createCell(j);
                }
            }
            ByteArrayOutputStream templateBaos = new ByteArrayOutputStream();
            workbook.write(templateBaos);
            return new ByteArrayInputStream(templateBaos.toByteArray());
        }
    }

    @Test
    void testFillTemplate() throws IOException {
        InputStream templateInputStream = createDummyTemplate();
        ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();
        ExcelTemplateFiller.fillTemplate(templateInputStream, resultBaos, data, samples, study, date);

        try (XSSFWorkbook resultWorkbook = new XSSFWorkbook(new ByteArrayInputStream(resultBaos.toByteArray()))) {
            Sheet resultSheet = resultWorkbook.getSheetAt(0);

            // Assertions for header
            assertEquals("Test Study", resultSheet.getRow(2).getCell(5).getStringCellValue());
            assertEquals(date, resultSheet.getRow(4).getCell(3).getLocalDateTimeCellValue().toLocalDate());
            assertEquals("Test Operator", resultSheet.getRow(5).getCell(3).getStringCellValue());
            assertEquals("Test Calculator", resultSheet.getRow(5).getCell(5).getStringCellValue());
            assertEquals("Test Protocol", resultSheet.getRow(6).getCell(3).getStringCellValue());
            assertEquals("Free Text", resultSheet.getRow(7).getCell(1).getStringCellValue());
            assertEquals("42", resultSheet.getRow(7).getCell(2).getStringCellValue());
            assertEquals("Test Assay", resultSheet.getRow(3).getCell(5).getStringCellValue());
            assertEquals("Test Assay", resultSheet.getRow(8).getCell(6).getStringCellValue());

            // Assertions for sample data
            assertEquals(1.0, resultSheet.getRow(11).getCell(0).getNumericCellValue());
            assertEquals("A1 (Box 1)", resultSheet.getRow(11).getCell(2).getStringCellValue());
            assertEquals("BC1", resultSheet.getRow(11).getCell(3).getStringCellValue());
            assertEquals("1ml", resultSheet.getRow(11).getCell(4).getStringCellValue());
            assertEquals("Blood", resultSheet.getRow(11).getCell(5).getStringCellValue());
            assertEquals("Positive", resultSheet.getRow(11).getCell(6).getStringCellValue());
        }
    }
}
