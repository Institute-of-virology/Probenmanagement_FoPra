package de.unimarburg.samplemanagement.UI.study;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import de.unimarburg.samplemanagement.model.*;
import de.unimarburg.samplemanagement.repository.AddressStoreRepository;
import de.unimarburg.samplemanagement.repository.ReportAuthorRepository;
import de.unimarburg.samplemanagement.repository.StudyRepository;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.utils.FORMAT_UTILS;
import de.unimarburg.samplemanagement.utils.GENERAL_UTIL;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Route("/CreateReport")
public class CreateStudyReport extends HorizontalLayout {

    private final AddressStoreRepository addressStoreRepository;
    private final ReportAuthorRepository reportAuthorRepository;
    private final StudyRepository studyRepository;
    private Study study;
    private Map<AnalysisType, Boolean> analysisCheckboxMap = new HashMap<>();
    private Map<SampleDelivery, Boolean> sampleDeliveriesCheckboxMap = new HashMap<>();
    Grid<Sample> sampleGrid;
    private ClientStateService clientStateService;
    private List<ReportAuthor> reportAuthors = new ArrayList<>();
    private Button printPdfButton;

    private Anchor downloadLink;

    @Autowired
    public CreateStudyReport(ClientStateService clientStateService, AddressStoreRepository addressStoreRepository, ReportAuthorRepository reportAuthorRepository, StudyRepository studyRepository) {
        this.clientStateService = clientStateService;
        this.reportAuthorRepository = reportAuthorRepository;
        this.addressStoreRepository = addressStoreRepository;
        this.studyRepository = studyRepository;
        setSizeFull();
        add(SIDEBAR_FACTORY.getSidebar(clientStateService.getClientState().getSelectedStudy()));
        if (clientStateService == null || clientStateService.getClientState().getSelectedStudy() == null) {
            add("Please select a Study");
            return;
        }
        this.study = clientStateService.getClientState().getSelectedStudy();
        if (study == null) {
            add("Please select a Study");
            return;
        }
        add(loadContent());
    }

    private VerticalLayout loadContent() {
        VerticalLayout body = new VerticalLayout();
        body.setSizeFull();

        Tab contentSelectionTab = new Tab("Content Selection");
        Tab reportDetailsTab = new Tab("Report Details");
        Tab generateReportTab = new Tab("Generate Report");

        Tabs tabs = new Tabs(contentSelectionTab, reportDetailsTab, generateReportTab);

        Div contentSelectionPage = new Div();
        contentSelectionPage.setSizeFull();
        Div reportDetailsPage = new Div();
        reportDetailsPage.setSizeFull();
        reportDetailsPage.setVisible(false);
        Div generateReportPage = new Div();
        generateReportPage.setSizeFull();
        generateReportPage.setVisible(false);

        // Content for Content Selection Tab
        VerticalLayout contentSelectionLayout = new VerticalLayout();
        contentSelectionLayout.setSizeFull();
        List<Sample> samples = study.getListOfSamples();
        sampleGrid = new Grid<>();
        sampleGrid.setSizeFull();
        sampleGrid.setItems(samples);
        sampleGrid.addColumn(Sample::getSample_barcode).setHeader("Sample Barcode");

        List<AnalysisType> uniqueAnalysisTypes = study.getAnalysisTypes().stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                AnalysisType::getAnalysisName,
                                at -> at,
                                (a, b) -> a
                        ),
                        map -> new java.util.ArrayList<>(map.values())
                ));

        if (uniqueAnalysisTypes.isEmpty()) {
            body.add("No Analyses available for Study: " + study.getStudyName());
            return body;
        }

        for (AnalysisType analysisType : uniqueAnalysisTypes) {
            sampleGrid.addColumn(sample -> {
                Object result = GENERAL_UTIL.getAnalysisForSample(sample, analysisType.getId());
                String display = (result == null || result.toString().isBlank()) ? "-" : result.toString();
                return display;
            }).setHeader(analysisType.getAnalysisName());
        }

        Accordion accordion = new Accordion();

        // Analysis Selection Accordion Panel
        VerticalLayout analysisSelectionLayout = new VerticalLayout();
        for (AnalysisType analysisType : uniqueAnalysisTypes) {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(true);
            Div labelDiv = new Div(analysisType.getAnalysisName());
            analysisCheckboxMap.put(analysisType, checkbox.getValue());
            checkbox.addValueChangeListener(event -> {
                analysisCheckboxMap.put(analysisType, event.getValue());
                updateGridItems();
            });
            analysisSelectionLayout.add(new HorizontalLayout(checkbox, labelDiv));
        }
        accordion.add("Select Analysis for report", analysisSelectionLayout);

        // Delivery Selection Accordion Panel
        VerticalLayout deliverySelectionLayout = new VerticalLayout();
        List<SampleDelivery> sampleDeliveries = study.getSampleDeliveryList();
        sampleDeliveries.sort(Comparator.comparing(SampleDelivery::getRunningNumber));
        for (SampleDelivery sampleDelivery : sampleDeliveries) {
            Checkbox checkbox = new Checkbox(true);
            Div labelRunningNumber = new Div(FORMAT_UTILS.getOrdinal(sampleDelivery.getRunningNumber()) + " delivery");
            Div labelDate = new Div(new SimpleDateFormat("dd.MM.yyyy").format(sampleDelivery.getDeliveryDate()));
            sampleDeliveriesCheckboxMap.put(sampleDelivery, checkbox.getValue());
            checkbox.addValueChangeListener(event -> {
                sampleDeliveriesCheckboxMap.put(sampleDelivery, event.getValue());
                updateGridItems();
            });
            deliverySelectionLayout.add(new HorizontalLayout(checkbox, labelRunningNumber, labelDate));
        }
        accordion.add("Select deliveries for report", deliverySelectionLayout);

        contentSelectionLayout.add(sampleGrid, accordion);
        contentSelectionLayout.expand(sampleGrid);
        contentSelectionPage.add(contentSelectionLayout);

        // Content for Report Details Tab
        VerticalLayout reportDetailsLayout = new VerticalLayout();
        TextArea methodValidationArea = new TextArea("Method Validation");
        methodValidationArea.setValue(study.getMethodValidation() != null ? study.getMethodValidation() : "");
        methodValidationArea.setReadOnly(true);
        methodValidationArea.setWidthFull();

        TextArea qualityControlArea = new TextArea("Quality Control");
        qualityControlArea.setValue(study.getQualityControl() != null ? study.getQualityControl() : "");
        qualityControlArea.setReadOnly(true);
        qualityControlArea.setWidthFull();

        TextArea remarksArea = new TextArea("Remarks");
        remarksArea.setValue(study.getRemarks() != null ? study.getRemarks() : "");
        remarksArea.setReadOnly(true);
        remarksArea.setWidthFull();

        Button saveButton = new Button("Save");
        saveButton.setVisible(false);

        Button editButton = new Button("Edit", event -> {
            methodValidationArea.setReadOnly(false);
            qualityControlArea.setReadOnly(false);
            remarksArea.setReadOnly(false);
            saveButton.setVisible(true);
        });

        saveButton.addClickListener(event -> {
            study.setMethodValidation(methodValidationArea.getValue());
            study.setQualityControl(qualityControlArea.getValue());
            study.setRemarks(remarksArea.getValue());
            studyRepository.save(study);

            methodValidationArea.setReadOnly(true);
            qualityControlArea.setReadOnly(true);
            remarksArea.setReadOnly(true);
            saveButton.setVisible(false);
            Notification.show("Report details saved.");
        });
        reportDetailsLayout.add(new HorizontalLayout(editButton, saveButton));
        reportDetailsLayout.add(methodValidationArea, qualityControlArea, remarksArea);
        reportDetailsPage.add(reportDetailsLayout);

        // Content for Generate Report Tab
        VerticalLayout generateReportLayout = new VerticalLayout();
        printPdfButton = new Button("Create Report");
        printPdfButton.addClickListener(event -> {
            try {
                String dest = "/tmp/study_report.pdf";
                generatePdf(dest);

                Path path = Paths.get(dest);
                if (!Files.exists(path)) {
                    Notification.show("PDF file was not created. Please try again.");
                    return;
                }

                StreamResource resource = new StreamResource("study_report.pdf", () -> {
                    try {
                        return new ByteArrayInputStream(Files.readAllBytes(path));
                    } catch (IOException e) {
                        Notification.show("Error reading PDF: " + e.getMessage());
                        return null;
                    }
                });

                if (downloadLink == null) {
                    downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", "study_report.pdf");
                    Button downloadButton = new Button("Download PDF");
                    downloadLink.add(downloadButton);
                    generateReportLayout.add(downloadLink);
                } else {
                    downloadLink.setHref(resource);
                }
            } catch (Exception e) {
                Notification.show("Error generating PDF: " + e.getMessage());
                e.printStackTrace();
            }
        });
        generateReportLayout.add(printPdfButton);
        generateReportPage.add(generateReportLayout);

        tabs.addSelectedChangeListener(event -> {
            contentSelectionPage.setVisible(false);
            reportDetailsPage.setVisible(false);
            generateReportPage.setVisible(false);

            if (tabs.getSelectedTab() == contentSelectionTab) {
                contentSelectionPage.setVisible(true);
            } else if (tabs.getSelectedTab() == reportDetailsTab) {
                reportDetailsPage.setVisible(true);
            } else if (tabs.getSelectedTab() == generateReportTab) {
                generateReportPage.setVisible(true);
            }
        });

        body.add(tabs, contentSelectionPage, reportDetailsPage, generateReportPage);
        body.expand(contentSelectionPage);

        return body;
    }


    private void updateGridItems() {
        List<SampleDelivery> selectedDeliveries = study.getSampleDeliveryList().stream()
                .filter(delivery -> sampleDeliveriesCheckboxMap.getOrDefault(delivery, false))
                .toList();

        List<Sample> samplesToFilter = selectedDeliveries.stream()
                .flatMap(delivery -> delivery.getSamples().stream())
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        List<AnalysisType> selectedAnalysisTypes = analysisCheckboxMap.keySet().stream()
                .filter(analysisType -> analysisCheckboxMap.getOrDefault(analysisType, true))
                .toList();

        long totalAnalyses = analysisCheckboxMap.keySet().size();
        if (selectedAnalysisTypes.size() < totalAnalyses) {
            if (!selectedAnalysisTypes.isEmpty()) {
                samplesToFilter = samplesToFilter.stream()
                        .filter(sample -> selectedAnalysisTypes.stream()
                                .anyMatch(analysisType -> {
                                    Object result = GENERAL_UTIL.getAnalysisForSample(sample, analysisType.getId());
                                    return result != null && !result.toString().isBlank();
                                }))
                        .collect(java.util.stream.Collectors.toList());
            } else {
                samplesToFilter.clear();
            }
        }
        sampleGrid.setItems(samplesToFilter);
    }

    public class HeaderFooterHandler implements IEventHandler {
        private final Image logo;
        private final PdfFormXObject placeholder;
        private final PdfFont font;
        private final int fontSize = 10;

        public HeaderFooterHandler(PdfDocument pdfDoc, Image logo, PdfFont font) {
            this.logo = logo;
            this.font = font;
            // Reserve space for total page number placeholder
            this.placeholder = new PdfFormXObject(new Rectangle(0, 0, 20, 10));
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdfDoc.getPageNumber(page);
            Rectangle pageSize = page.getPageSize();

            Canvas canvas = new Canvas(new PdfCanvas(page, true), pdfDoc, pageSize);

            // --- Add logo at top center ---
            logo.setFixedPosition(
                    (pageSize.getWidth() - logo.getImageScaledWidth()) / 2,
                    pageSize.getTop() - logo.getImageScaledHeight() - 10
            );
            canvas.add(logo);

            // --- Page numbering: "Page X of Y" ---
            Paragraph p = new Paragraph()
                    .setFont(font)
                    .setFontSize(fontSize)
                    .add("Page " + pageNumber + " of ")
                    .add(new Image(placeholder)); // Placeholder for total pages

            canvas.showTextAligned(p,
                    pageSize.getRight() - 60,  // right margin
                    pageSize.getTop() - 20,   // from top
                    TextAlignment.RIGHT);

            canvas.close();
        }

        // Call this after closing the document to fill in total pages
        public void writeTotal(PdfDocument pdfDoc) {
            Canvas canvas = new Canvas(placeholder, pdfDoc);
            canvas.showTextAligned(
                    new Paragraph(String.valueOf(pdfDoc.getNumberOfPages()))
                            .setFont(font)
                            .setFontSize(fontSize),
                    0, -3, TextAlignment.LEFT);
            canvas.close();
        }
    }

    private void generatePdf(String dest) throws IOException, URISyntaxException {
        PdfWriter writer = new PdfWriter(dest);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4);
        Document document = new Document(pdfDoc);
        document.setMargins(75, 20, 36, 20);

        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("calibri.ttf")) {
            if (fontStream == null) {
                throw new FileNotFoundException("calibri.ttf resource not found");
            }
            byte[] fontBytes = IOUtils.toByteArray(fontStream);
            PdfFont calibriFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, true);
            PdfFont calibriBoldFont = calibriFont;

            try (InputStream logoStream = getClass().getClassLoader().getResourceAsStream("uni-logo.png")) {
                if (logoStream == null) {
                    throw new FileNotFoundException("uni-logo.png resource not found");
                }
                byte[] logoBytes = IOUtils.toByteArray(logoStream);
                ImageData logoImageData = ImageDataFactory.create(logoBytes);
                Image logoImage = new Image(logoImageData)
                        .scaleToFit(150, 150)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);

                HeaderFooterHandler handler = new HeaderFooterHandler(pdfDoc, logoImage, calibriFont);
                pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, handler);

                document.setFont(calibriFont);
                document.setFontSize(10);

                Paragraph header = new Paragraph("Philipps-Universität Marburg -- Institut für Virologie -- Immunmonitoringlabor")
                        .setFont(calibriBoldFont)
                        .setFontSize(10)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMargins(0, 0, 5, 0); // add some bottom margin
                document.add(header);

                document.add(new LineSeparator(new SolidLine()));

                float[] columnWidths = {3, 3, 4};
                Table senderTable = new Table(columnWidths);
                senderTable.setWidth(UnitValue.createPercentValue(100));
                this.reportAuthors = reportAuthorRepository.findAll();
                for (int i = 0; i < reportAuthors.size(); i++) {
                    ReportAuthor reportAuthor = reportAuthors.get(i);
                    String authorInfo = reportAuthor.getName() + ", " + reportAuthor.getTitle();
                    senderTable.addCell(new Cell(1, 2).add(new Paragraph(authorInfo).setFont(calibriBoldFont).setFontSize(10)).setBold().setBorder(null));

                    if (i == 0) {
                        senderTable.addCell(new Cell(reportAuthors.size(), 1).add(new Paragraph(addressStoreRepository.getOwnAddress())
                                        .setFont(calibriFont)
                                        .setFontSize(10))
                                .setBorder(null));
                    }
                }
                document.add(senderTable);

                document.add(new LineSeparator(new SolidLine()));

                Paragraph recipient = new Paragraph("Recipient Address:\n" + study.getSponsor())
                        .setFont(calibriFont)
                        .setFontSize(10)
                        .setMargins(10, 0, 5, 0); // add top margin
                document.add(recipient);

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                Paragraph dateOfReport = new Paragraph("Date of Report Generation: " + now.format(formatter))
                        .setFont(calibriFont)
                        .setFontSize(10)
                        .setBold()
                        .setMargins(10, 0, 5, 0); // add top margin
                document.add(dateOfReport);

                Paragraph reportTitle = new Paragraph("Report on the results of the serological analysis")
                        .setFont(calibriBoldFont)
                        .setFontSize(10)
                        .setBold()
                        .setMargins(10, 0, 5, 0); // add top margin
                document.add(reportTitle);

                StringBuilder methods = new StringBuilder("Analytical methods: ");
                List<AnalysisType> selectedAnalysisTypes = analysisCheckboxMap.keySet().stream()
                        .filter(analysisCheckboxMap::get)
                        .toList();
                for (AnalysisType analysisType : selectedAnalysisTypes) {
                    methods.append(analysisType.getAnalysisName()).append(", ");
                }
                if (methods.length() > 0) {
                    methods.setLength(methods.length() - 2);
                }
                Paragraph analyticalMethods = new Paragraph(methods.toString())
                        .setFont(calibriFont)
                        .setFontSize(10)
                        .setMargins(10, 0, 5, 0); // add top margin
                document.add(analyticalMethods);

                Paragraph studyDetails = new Paragraph()
                        .add("Study: ")
                        .add(new Text(study.getStudyName()).setBold())
                        .add(", " + study.getStartDate().toString() + "-" + study.getEndDate().toString())
                        .setFont(calibriFont)
                        .setFontSize(10)
                        .setMargins(10, 0, 5, 0); // add top margin
                document.add(studyDetails);

                Paragraph deliveriesTitle = new Paragraph("Sample Deliveries:")
                        .setFont(calibriBoldFont)
                        .setFontSize(10)
                        .setBold()
                        .setMargins(10, 0, 5, 0); // add top margin
                document.add(deliveriesTitle);

                List<SampleDelivery> selectedDeliveries = sampleDeliveriesCheckboxMap.keySet().stream()
                        .filter(sampleDeliveriesCheckboxMap::get)
                        .sorted(Comparator.comparing(SampleDelivery::getRunningNumber))
                        .toList();

                if (selectedDeliveries.isEmpty()) {
                    document.add(new Paragraph("No specific deliveries selected for this report.").setFont(calibriFont).setFontSize(10).setMargins(0,0,5,0));
                } else {
                    com.itextpdf.layout.element.List deliveryList = new com.itextpdf.layout.element.List()
                            .setSymbolIndent(12)
                            .setListSymbol("\u2022")
                            .setFont(calibriFont)
                            .setFontSize(10)
                            .setMarginLeft(20);
                    for (SampleDelivery delivery : selectedDeliveries) {
                        String deliveryInfo = String.format("%s delivery: Received on %s, %d samples.",
                                FORMAT_UTILS.getOrdinal(delivery.getRunningNumber()),
                                new SimpleDateFormat("dd.MM.yyyy").format(delivery.getDeliveryDate()),
                                delivery.getSamples().size());
                        deliveryList.add(new ListItem(deliveryInfo));
                    }
                    document.add(deliveryList);
                }

                if (study.getMethodValidation() != null && !study.getMethodValidation().isEmpty()) {
                    document.add(new Paragraph("Method validation:").setBold().setMargins(10, 0, 0, 0));
                    document.add(new Paragraph(study.getMethodValidation()).setMargins(0, 0, 5, 0));
                }
                if (study.getQualityControl() != null && !study.getQualityControl().isEmpty()) {
                    document.add(new Paragraph("Quality control:").setBold().setMargins(10, 0, 0, 0));
                    document.add(new Paragraph(study.getQualityControl()).setMargins(0, 0, 5, 0));
                }
                if (study.getRemarks() != null && !study.getRemarks().isEmpty()) {
                    document.add(new Paragraph("Remarks:").setBold().setMargins(10, 0, 0, 0));
                    document.add(new Paragraph(study.getRemarks()).setMargins(0, 0, 5, 0));
                }

                document.add(new Paragraph("The results can be found on the following page(s).").setBold().setMargins(10, 0, 0, 0));
                document.add(new Paragraph("The test report may not be reproduced without the written consent of the laboratory.").setBold().setMargins(5, 0, 10, 0));

                document.add(new AreaBreak());

                float[] tableColumnWidths = new float[selectedAnalysisTypes.size() + 2];
                tableColumnWidths[0] = 1;
                tableColumnWidths[1] = 2;
                for (int i = 2; i < tableColumnWidths.length; i++) {
                    tableColumnWidths[i] = 1;
                }
                Table table = new Table(tableColumnWidths);
                table.setWidth(UnitValue.createPercentValue(100));

                table.addHeaderCell(new Cell().add(new Paragraph("No.")).setFont(calibriFont).setFontSize(10));
                table.addHeaderCell(new Cell().add(new Paragraph("Sample ID")).setFont(calibriFont).setFontSize(10));
                for (AnalysisType analysisType : selectedAnalysisTypes) {
                    table.addHeaderCell(new Cell().add(new Paragraph(analysisType.getAnalysisName())).setFont(calibriFont).setFontSize(10));
                }

                List<Sample> samples = study.getListOfSamples();
                for (int i = 0; i < samples.size(); i++) {
                    Sample sample = samples.get(i);
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))).setFont(calibriFont).setFontSize(10));
                    table.addCell(new Cell().add(new Paragraph(sample.getSample_barcode())).setFont(calibriFont).setFontSize(10));
                    for (AnalysisType analysisType : selectedAnalysisTypes) {
                        Object result = GENERAL_UTIL.getAnalysisForSample(sample, analysisType.getId());
                        String display = (result == null || result.toString().isBlank()) ? "-" : result.toString();
                        table.addCell(new Cell().add(new Paragraph(display)));
                    }
                }

                document.add(new Paragraph("Results: ").setBold().setMargins(0, 0, 5, 0));
                document.add(table);

                for (AnalysisType analysisType : selectedAnalysisTypes) {
                    Paragraph textbaustein = new Paragraph(analysisType.getAnalysisName() + ": " + analysisType.getAnalysisDescription())
                            .setFont(calibriFont)
                            .setBold()
                            .setFontSize(10)
                            .setMargins(10, 0, 10, 0);
                    document.add(textbaustein);
                }

                document.add(new Paragraph("\n\n"));

                float[] validationColumnWidths = {1, 1};
                Table validationTable = new Table(validationColumnWidths);
                validationTable.setWidth(UnitValue.createPercentValue(100));
                validationTable.setBorder(null);

                validationTable.addCell(new Cell().add(new Paragraph("Technical validation" + "\n\n\n")
                                .setFont(calibriBoldFont)
                                .setFontSize(10)
                                .setBold())
                        .setMargins(10, 0, 0, 0)
                        .setBorder(null)
                        .setTextAlignment(TextAlignment.LEFT));

                validationTable.addCell(new Cell().add(new Paragraph("Final validation" + "\n\n\n")
                                .setFont(calibriBoldFont)
                                .setFontSize(10)
                                .setBold())
                        .setBorder(null)
                        .setTextAlignment(TextAlignment.RIGHT));

                validationTable.addCell(new Cell().add(new Paragraph("Datum, Unterschrift")
                                .setFont(calibriFont)
                                .setFontSize(10))
                        .setBorder(null)
                        .setTextAlignment(TextAlignment.LEFT));

                validationTable.addCell(new Cell().add(new Paragraph("Datum, Unterschrift")
                                .setFont(calibriFont)
                                .setFontSize(10))
                        .setBorder(null)
                        .setTextAlignment(TextAlignment.RIGHT));

                document.add(validationTable);

                handler.writeTotal(pdfDoc);

                document.close();
                pdfDoc.close();
            }
        }
    }
}


