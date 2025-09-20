package de.unimarburg.samplemanagement.utils;

import de.unimarburg.samplemanagement.model.*;
import de.unimarburg.samplemanagement.repository.*;
import de.unimarburg.samplemanagement.service.ClientStateService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Component
public class ExcelParser {

    private SampleDeliveryRepository sampleDeliveryRepository;
    private SubjectRepository subjectRepository;
    private StudyRepository studyRepository;
    private ClientStateService clientStateService;
    private AnalysisRepository analysisRepository;

    private AnalysisType selectedAnalysisType = null;
    @Autowired
    public ExcelParser(SampleDeliveryRepository sampleDeliveryRepository, SubjectRepository subjectRepository, StudyRepository studyRepository, ClientStateService clientStateService,AnalysisRepository analysisRepository) {
        this.analysisRepository = analysisRepository;
        this.sampleDeliveryRepository = sampleDeliveryRepository;
        this.subjectRepository = subjectRepository;
        this.studyRepository = studyRepository;
        this.clientStateService = clientStateService;
    }

    public enum cellType {
        STRING, NUMERIC, DATE, BOOLEAN
    }

    public void readExcelFile(FileInputStream inputStream) throws IOException {

        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = sheet.iterator();

        String studyName;
        Date deliveryDate;
        String boxNumber;

        // Skip first 3 rows (we want row 3 which is index 2)
        for (int i = 0; i < 2 && iterator.hasNext(); i++) {
            iterator.next();
        }

        // Reading Study Name from Row 4, Column D (index 3)
        if (iterator.hasNext()) {
            Row currentRow = iterator.next();
            studyName = (String) getCellValue(currentRow.getCell(3), cellType.STRING);

            Row deliveryDateRow = sheet.getRow(5);
            deliveryDate = (Date) getCellValue(deliveryDateRow.getCell(3), cellType.DATE);
            System.out.println("Delivery date is : " + deliveryDate);

            Row boxNumberRow = sheet.getRow(7);
            try {
                boxNumber = (String) getCellValue(boxNumberRow.getCell(3), cellType.STRING);
            } catch (IOException e) {
                Double boxNumberDouble = (Double) getCellValue(boxNumberRow.getCell(3), cellType.NUMERIC);
                boxNumber = String.valueOf(boxNumberDouble).split("\\.")[0];
            }

        } else {
            throw new IOException("Empty Excel File");
        }

        Study study = studyRepository.findByStudyName(studyName);
        Study selectedStudy = clientStateService.getClientState().getSelectedStudy();
        if (selectedStudy != null && !selectedStudy.getStudyName().equals(studyName)) {
            throw new IOException("Selected study does not match the study in the file");
        }

        if (study == null) {
            throw new IOException("Study not found");
        }

        if(sampleDeliveryRepository.findByStudyAndBoxNumber(study, boxNumber).isPresent()){
            throw new IOException("This delivery has already been processed.");
        }

        // Use default alias as Long
        Long defaultAlias = (long) (10000 + new Random().nextInt(90000));
        Subject defaultSubject = subjectRepository.getSubjectByAliasAndStudy(defaultAlias, study)
                .orElseGet(() -> {
                    Subject newSubject = new Subject();
                    newSubject.setAlias(defaultAlias);
                    newSubject.setStudy(study);
                    return subjectRepository.save(newSubject);
                });

        // Create a new SampleDelivery
        SampleDelivery sampleDelivery = new SampleDelivery();
        sampleDelivery.setDeliveryDate(new Date());
        sampleDelivery.setStudy(study);
        sampleDelivery.setBoxNumber(boxNumber);

        // Skip rows until sample data starts at row 11 (index 10)
        for (int i = 0; i < 6 && iterator.hasNext(); i++) {
            iterator.next();
        }

        while (iterator.hasNext()) {
            Row currentRow = iterator.next();

            // Skip null or empty rows
            if (currentRow == null || currentRow.getCell(0) == null) {
                continue;
            }

            Sample sample = new Sample();

            // Coordinates (Column B = index 1)
            String coordinates = (String) getCellValue(currentRow.getCell(1), cellType.STRING);
            sample.setCoordinates(coordinates);

            sample.setSubject(defaultSubject);
            sample.setDateOfShipment(deliveryDate);
            sample.setStudy(study);

            // Barcode (Column C = index 2)
            String barcode;
            try {
                barcode = (String) getCellValue(currentRow.getCell(2), cellType.STRING);
            } catch (IOException e) {
                Double barcodeDouble = (Double) getCellValue(currentRow.getCell(2), cellType.NUMERIC);
                barcode = String.valueOf(barcodeDouble).split("\\.")[0];
            }

            // Skip rows with empty barcode
            if (barcode == null || barcode.trim().isEmpty()) {
                System.out.println("Skipping row " + currentRow.getRowNum() + ": Empty barcode.");
                continue;
            }

            // Skip if barcode already exists for this study
            String finalBarcode = barcode;
            boolean duplicateExists = study.getListOfSamples().stream()
                    .anyMatch(s -> finalBarcode.equals(s.getSample_barcode()));
            if (duplicateExists) {
                System.out.println("Skipping duplicate barcode: " + barcode + " for study: " + study.getStudyName());
                continue;
            }

            sample.setSample_barcode(barcode);

            // Amount (Column D = index 3)
            String amount = String.valueOf(getCellValue(currentRow.getCell(3), cellType.NUMERIC));
            sample.setSample_amount(amount);

            // Sample type (Column E = index 4)
            String sampleType = (String) getCellValue(currentRow.getCell(4), cellType.STRING);
            sample.setSample_type(sampleType);

            sampleDelivery.addSample(sample);
        }
        workbook.close();
        inputStream.close();

        sampleDeliveryRepository.save(sampleDelivery);
    }

    private Subject getSubject(long alias, Study study) {
        Optional<Subject> subject = subjectRepository.getSubjectByAliasAndStudy(alias, study);
        if (subject.isPresent()) {
            return subject.get();
        }
        Subject subjectNew = new Subject();
        subjectNew.setAlias(alias);
        subjectNew.setStudy(study);
        subjectNew = subjectRepository.save(subjectNew);
        return subjectNew;
    }

    public static Object getCellValue(Cell cell, cellType expectedType) throws IOException {
        System.out.println("in getcell method");
        if (cell == null) {
            System.out.println("in null condition");
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                if (expectedType == cellType.STRING) {
                    System.out.println("in string condition");
                    return cell.getStringCellValue();
                }
                break;
            case NUMERIC:
                if (expectedType == cellType.DATE && DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else if (expectedType == cellType.NUMERIC) {
                    System.out.println("case numeric");
                    return cell.getNumericCellValue();
                }
                break;
            case BOOLEAN:
                if (expectedType == cellType.BOOLEAN) {
                    System.out.println("case boolean");
                    return cell.getBooleanCellValue();
                }
                break;
            case BLANK:
                System.out.println("case Blank");
                return "";
            default:
                System.out.println("case exception");
                throw new IOException("Unexpected cell type: " + cell.getCellType());
        }
        // If cell type does not match expected type, throw an exception
        throw new IOException("Expected cell type: " + expectedType + ", but got: " + cell.getCellType());
    }

    private static long getNumericValue(double value) throws IOException {
        if (value == (long) value) {
            return (long) value;
        }
        throw new IOException("Expected int value, given Double");
    }

    public void readArbeitslist(FileInputStream inputStream) throws IOException {
        System.out.println("I am in readfile");

        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = sheet.iterator();

        String studyName;
        Date deliveryDate;

        // Skip to Row 4 (index 3) for Study Name
        if (iterator.hasNext()) {
            // skip 3 rows
            for (int i = 0; i < 3 && iterator.hasNext(); i++) iterator.next();

            Row studyNameRow = sheet.getRow(3); // Row 4 (index 3)
            studyName = (String) getCellValue(studyNameRow.getCell(3), cellType.STRING); // Column D (index 3)
            System.out.println("Study name is : " + studyName);

            Row deliveryDateRow = sheet.getRow(5);
            deliveryDate = (Date) getCellValue(deliveryDateRow.getCell(3), cellType.DATE);
            System.out.println("Delivery date is : " + deliveryDate);
        } else {
            throw new IOException("Empty Excel File");
        }

        Study study = studyRepository.findByStudyName(studyName);
        Study selectedStudy = clientStateService.getClientState().getSelectedStudy();

        System.out.println("selected study is: " + selectedStudy.getStudyName());
        if (selectedStudy != null && !selectedStudy.getStudyName().equals(studyName)) {
            throw new IOException("Selected study does not match the study in the file");
        }

        // 🔴 Assuming assay/analysis name is no longer provided in the new sheet
        // If it *is*, please let me know where it appears so I can re-add this:
        // Row assay = sheet.getRow(3); // <- previously used row 4
        // String analysename = (String) getCellValue(assay.getCell(5), cellType.STRING);

        // TEMPORARY placeholder: using first available analysisType
        selectedAnalysisType = study.getAnalysisTypes().stream().findFirst().orElseThrow(() ->
                new IOException("No analysis types found for the study"));

        // Sample data starts from Row 11 (index 10)
        int startRow = 10;

        Map<String, String> map = new HashMap<>();
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                String barcode = "";
                try {
                    System.out.println("in barcode");
                    barcode = (String) getCellValue(row.getCell(2), cellType.STRING); // Column C (index 1)
                } catch (IOException e) {
                    System.out.println("in catch");
                    Double barcodeDouble = (Double) getCellValue(row.getCell(2), cellType.NUMERIC);
                    barcode = String.valueOf(barcodeDouble).split("\\.")[0];
                }

                // Assuming Analysis Result is in Column E (index 4)
                System.out.println("before map");
                map.put(barcode, String.valueOf(row.getCell(4))); // update from cell(6) to cell(4)
            }
        }

        map.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));

        List<Analysis> relevantAnalyses = study.getListOfSamples().stream()
                .flatMap(sample -> sample.getListOfAnalysis().stream())
                .filter(analysis -> analysis.getAnalysisType().getId().equals(selectedAnalysisType.getId()))
                .toList();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String barcode = entry.getKey();
            Analysis analysis = findCorrectAnalysis(relevantAnalyses, barcode);
            analysis.setAnalysisResult(entry.getValue());
            // Set delivery date to the corresponding sample
            if (analysis.getSample() != null) {
                analysis.getSample().setDateOfShipment(deliveryDate);
            }
            analysisRepository.save(analysis);
        }

        workbook.close();
        inputStream.close();
    }

    public Analysis findCorrectAnalysis(List<Analysis> list, String barcode){
        return list.stream()
                .filter(analysis -> barcode.equals(analysis.getSample().getSample_barcode()))
                .findFirst().get();

    }
}
