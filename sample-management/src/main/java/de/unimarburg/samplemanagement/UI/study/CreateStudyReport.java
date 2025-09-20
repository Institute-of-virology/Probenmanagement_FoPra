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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import de.unimarburg.samplemanagement.model.*;
import de.unimarburg.samplemanagement.repository.AddressStoreRepository;
import de.unimarburg.samplemanagement.repository.ReportAuthorRepository;
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
    private Study study;
    private Map<AnalysisType, Boolean> analysisCheckboxMap = new HashMap<>();
    private Map<SampleDelivery, Boolean> sampleDeliveriesCheckboxMap = new HashMap<>();
    Grid<Sample> sampleGrid;
    private ClientStateService clientStateService;
    private List<ReportAuthor> reportAuthors = new ArrayList<>();
    private Button printPdfButton;

    private Anchor downloadLink;

    @Autowired
    public CreateStudyReport(ClientStateService clientStateService, AddressStoreRepository addressStoreRepository, ReportAuthorRepository reportAuthorRepository) {
        this.clientStateService = clientStateService;
        this.reportAuthorRepository = reportAuthorRepository;
        this.addressStoreRepository = addressStoreRepository;
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
        List<Sample> samples = study.getListOfSamples();

        sampleGrid = new Grid<>();
        sampleGrid.setItems(samples); // Don't forget to set items

        sampleGrid.addColumn(Sample::getSample_barcode).setHeader("Sample Barcode");

        // Deduplicate AnalysisTypes by name to avoid duplicate headers
        List<AnalysisType> uniqueAnalysisTypes = study.getAnalysisTypes().stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                AnalysisType::getAnalysisName, // use name as deduplication key
                                at -> at,
                                (a, b) -> a // keep the first if duplicate
                        ),
                        map -> new java.util.ArrayList<>(map.values())
                ));

        // Add columns for each unique analysis
        if (uniqueAnalysisTypes.isEmpty()) {
            body.add("No Analyses available for Study: " + study.getStudyName());
            return body;
        }

        for (AnalysisType analysisType : uniqueAnalysisTypes) {
            sampleGrid.addColumn(sample -> {
                Object result = GENERAL_UTIL.getAnalysisForSample(sample, analysisType.getId());
                // Convert to string and handle null or blank
                String display = (result == null || result.toString().isBlank()) ? "-" : result.toString();
                return display;
            }).setHeader(analysisType.getAnalysisName());
        }

        body.add(sampleGrid);

        body.add("Select Analysis for report:");

        // Add horizontal layout underneath the grid
        HorizontalLayout analysisSelection = new HorizontalLayout();
        for (AnalysisType analysisType : uniqueAnalysisTypes) {
            Checkbox checkbox = new Checkbox();
            Div labelDiv = new Div(analysisType.getAnalysisName());

            // Initialize checkbox value and store it in the map
            analysisCheckboxMap.put(analysisType, checkbox.getValue());

            checkbox.addValueChangeListener(event -> {
                // Update the checkbox value in the map
                analysisCheckboxMap.put(analysisType, event.getValue());
            });
            analysisSelection.add(checkbox, labelDiv);
        }
        body.add(analysisSelection);

        body.add("Select deliveries for report:");
        HorizontalLayout sampleDeliverySelection = new HorizontalLayout();
        List<SampleDelivery> sampleDeliveries = study.getSampleDeliveryList();
        sampleDeliveries.sort(Comparator.comparing(SampleDelivery::getRunningNumber));
        for (SampleDelivery sampleDelivery : sampleDeliveries) {
            Checkbox checkbox = new Checkbox();
            Div labelRunningNumber = new Div(FORMAT_UTILS.getOrdinal(sampleDelivery.getRunningNumber()) + " delivery");
            Div labelDate = new Div(new SimpleDateFormat("dd.MM.yyyy").format(sampleDelivery.getDeliveryDate()));
            VerticalLayout labelLayout = new VerticalLayout(checkbox, labelRunningNumber, labelDate);

            // Initialize checkbox value and store it in the map
            sampleDeliveriesCheckboxMap.put(sampleDelivery, checkbox.getValue());

            checkbox.addValueChangeListener(event -> {
                // Update the checkbox value in the map
                sampleDeliveriesCheckboxMap.put(sampleDelivery, event.getValue());
                updateSampleGrid(sampleDeliveries.stream().filter(sampleDeliveriesCheckboxMap::get).toList());
            });
            sampleDeliverySelection.add(labelLayout);
        }
        body.add(sampleDeliverySelection);

        reportAuthors = reportAuthorRepository.findAll();

        printPdfButton = new Button("Create Report");
        printPdfButton.addClickListener(event -> {
            try {
                // Generate the PDF file
                String dest = "/tmp/study_report.pdf";
                generatePdf(dest);

                // Create a StreamResource for downloading the generated PDF
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

                // Add the download link to the UI
                if (downloadLink == null) {
                    downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", "study_report.pdf");
                    Button downloadButton = new Button("Download PDF");
                    downloadLink.add(downloadButton);
                    body.add(downloadLink);
                } else {
                    downloadLink.setHref(resource);  // just update the resource
                }
            } catch (Exception e) {
                Notification.show("Error generating PDF: " + e.getMessage());
                e.printStackTrace();
            }
        });
        body.add(printPdfButton);

        return body;
    }


    private void updateSampleGrid(List<SampleDelivery> sampleDeliveries) {
        List<Sample> samples = new ArrayList<>();
        for (SampleDelivery sampleDelivery : sampleDeliveries) {
            samples.addAll(sampleDelivery.getSamples());
        }
        sampleGrid.setItems(samples);
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

        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("calibri.ttf")) {
            if (fontStream == null) {
                throw new FileNotFoundException("calibri.ttf resource not found");
            }
            byte[] fontBytes = IOUtils.toByteArray(fontStream);
            PdfFont calibriFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, true);

            // Use the same font for bold if you don't have a separate bold font file
            PdfFont calibriBoldFont = calibriFont;

            // Load the logo image from resource stream safely
            try (InputStream logoStream = getClass().getClassLoader().getResourceAsStream("uni-logo.png")) {
                if (logoStream == null) {
                    throw new FileNotFoundException("uni-logo.png resource not found");
                }
                byte[] logoBytes = IOUtils.toByteArray(logoStream);
                ImageData logoImageData = ImageDataFactory.create(logoBytes);
                Image logoImage = new Image(logoImageData)
                        .scaleToFit(100, 100)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);

                // Set the header handler
                // Create a placeholder object
                PdfFormXObject placeholder = new PdfFormXObject(new Rectangle(50, 12));

                HeaderFooterHandler handler = new HeaderFooterHandler(pdfDoc, logoImage, calibriFont);
                pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, handler);

                // Set the default font for the document
                document.setFont(calibriFont);
                document.setFontSize(11);

                // Add spacing after logo
                document.add(new Paragraph("\n"));

                // Add the header
                Paragraph header = new Paragraph("Philipps-Universität Marburg -- Institut für Virologie -- Immunmonitoringlabor")
                        .setFont(calibriBoldFont)
                        .setFontSize(11)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER);
                document.add(header);

                // Add a horizontal line separator
                document.add(new LineSeparator(new SolidLine()));

                // Create the three-column table for sender details and fixed address
                float[] columnWidths = {2, 3, 5};
                Table senderTable = new Table(columnWidths);
                senderTable.setWidth(UnitValue.createPercentValue(100));

                // Add sender details and fixed address
                for (int i = 0; i < reportAuthors.size(); i++) {
                    ReportAuthor reportAuthor = reportAuthors.get(i);
                    senderTable.addCell(new Cell().add(new Paragraph(reportAuthor.getName()).setFont(calibriBoldFont).setFontSize(11)).setBold().setBorder(null));
                    senderTable.addCell(new Cell().add(new Paragraph(reportAuthor.getTitle()).setFont(calibriBoldFont).setFontSize(11)).setBorder(null));

                    if (i == 0) {
                        senderTable.addCell(new Cell(reportAuthors.size(), 1).add(new Paragraph(addressStoreRepository.getOwnAddress())
                                        .setFont(calibriFont)
                                        .setFontSize(11))
                                .setBorder(null));
                    }
                }

                document.add(senderTable);

                // Add a horizontal line separator
                document.add(new LineSeparator(new SolidLine()));

                // Add recipient addresses
                Paragraph recipient = new Paragraph("Recipient Address:\n" + study.getSponsor())
                        .setFont(calibriFont)
                        .setFontSize(11);
                document.add(recipient);

                // Spacing
                document.add(new Paragraph("\n"));

                // Add "Date of Report Generation: " followed by the current date and time
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                Paragraph dateOfReport = new Paragraph("Date of Report Generation: " + now.format(formatter))
                        .setFont(calibriFont)
                        .setFontSize(11)
                        .setBold();
                document.add(dateOfReport);

                // Add spacing after recipient address
                document.add(new Paragraph("\n"));

                // Add "Report on the results of the serological analysis" in bold
                Paragraph reportTitle = new Paragraph("Report on the results of the serological analysis")
                        .setFont(calibriBoldFont)
                        .setFontSize(11)
                        .setBold();
                document.add(reportTitle);

                // Add spacing after report title
                document.add(new Paragraph("\n"));

                // Add "Analytical methods: " followed by AnalysisTypeName for selected checkboxes
                StringBuilder methods = new StringBuilder("Analytical methods: ");
                List<AnalysisType> selectedAnalysisTypes = analysisCheckboxMap.keySet().stream()
                        .filter(analysisCheckboxMap::get)
                        .toList();
                for (AnalysisType analysisType : selectedAnalysisTypes) {
                    methods.append(analysisType.getAnalysisName()).append(", ");
                }
                if (methods.length() > 0) {
                    methods.setLength(methods.length() - 2); // Remove the last comma and space
                }
                Paragraph analyticalMethods = new Paragraph(methods.toString())
                        .setFont(calibriFont)
                        .setFontSize(11);
                document.add(analyticalMethods);

                // Add spacing after analytical methods
                document.add(new Paragraph("\n"));

                // Add "Study: " followed by StudyName and studyDate
                Paragraph studyDetails = new Paragraph("Study: " + study.getStudyName() + ", " + study.getStartDate().toString() + "-" + study.getEndDate().toString())
                        .setFont(calibriFont)
                        .setFontSize(11);
                document.add(studyDetails);


                // Add fields for details to the study (handwritten by user)
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("Method validation: ").setBold());
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("Quality control: ").setBold());
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("Remarks: ").setBold());
                document.add(new Paragraph("\n"));


                // Add spacing after study details
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("The results can be found on the following page(s).").setBold());
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("The test report may not be reproduced without the written consent of the laboratory.").setBold());

                // Add a page break here to start the table on a new page
                document.add(new AreaBreak());


                // Create the table with appropriate number of columns, including the numbering column
                float[] tableColumnWidths = new float[selectedAnalysisTypes.size() + 2]; // +2 for numbering and sample ID
                tableColumnWidths[0] = 1; // For numbering column
                tableColumnWidths[1] = 2; // For Sample ID column, make it wider
                for (int i = 2; i < tableColumnWidths.length; i++) {
                    tableColumnWidths[i] = 1; // Make each analysis column narrower
                }
                Table table = new Table(tableColumnWidths);
                table.setWidth(UnitValue.createPercentValue(100)); // Set the table width to 100% of the page

                // Add table headers
                table.addHeaderCell(new Cell().add(new Paragraph("No.")).setFont(calibriFont).setFontSize(11));
                table.addHeaderCell(new Cell().add(new Paragraph("Sample ID")).setFont(calibriFont).setFontSize(11));
                for (AnalysisType analysisType : selectedAnalysisTypes) {
                    table.addHeaderCell(new Cell().add(new Paragraph(analysisType.getAnalysisName())).setFont(calibriFont).setFontSize(11));
                }

                // Add sample data to the table
                List<Sample> samples = study.getListOfSamples();
                for (int i = 0; i < samples.size(); i++) {
                    Sample sample = samples.get(i);
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))).setFont(calibriFont).setFontSize(11)); // Add numbering
                    table.addCell(new Cell().add(new Paragraph(sample.getSample_barcode())).setFont(calibriFont).setFontSize(11));
                    for (AnalysisType analysisType : selectedAnalysisTypes) {
                        Object result = GENERAL_UTIL.getAnalysisForSample(sample, analysisType.getId());
                        // Convert to string and handle null or blank
                        String display = (result == null || result.toString().isBlank()) ? "-" : result.toString();
                        table.addCell(new Cell().add(new Paragraph(display)));
                    }
                }

                // Add the table normally to follow the flow of the document
                document.add(new Paragraph("Results: ").setBold());
                document.add(table);
                document.add(new Paragraph("\n"));

                // Add "Textbaustein " followed by analysisName and description
                for (AnalysisType analysisType : selectedAnalysisTypes) {
                    Paragraph textbaustein = new Paragraph(analysisType.getAnalysisName() + ": " + analysisType.getAnalysisDescription())
                            .setFont(calibriFont)
                            .setBold()
                            .setFontSize(11);
                    document.add(textbaustein);
                    document.add(new Paragraph("\n")); // Add line break after each analysis type
                }


                // Add extra spacing before the final section
                document.add(new Paragraph("\n\n\n"));

                // Add the final section with "Technical validation" and "Final validation" in a table
                float[] validationColumnWidths = {1, 1};
                Table validationTable = new Table(validationColumnWidths);
                validationTable.setWidth(UnitValue.createPercentValue(100));
                validationTable.setBorder(null);

                // Add validation headers
                validationTable.addCell(new Cell().add(new Paragraph("Technical validation" + "\n\n\n")
                                .setFont(calibriBoldFont)
                                .setFontSize(11)
                                .setBold())
                        .setBorder(null)
                        .setTextAlignment(TextAlignment.LEFT));

                validationTable.addCell(new Cell().add(new Paragraph("Final validation" + "\n\n\n")
                                .setFont(calibriBoldFont)
                                .setFontSize(11)
                                .setBold())
                        .setBorder(null)
                        .setTextAlignment(TextAlignment.RIGHT));

                // Add signature lines
                validationTable.addCell(new Cell().add(new Paragraph("Datum, Unterschrift")
                                .setFont(calibriFont)
                                .setFontSize(11))
                        .setBorder(null)
                        .setTextAlignment(TextAlignment.LEFT));

                validationTable.addCell(new Cell().add(new Paragraph("Datum, Unterschrift")
                                .setFont(calibriFont)
                                .setFontSize(11))
                        .setBorder(null)
                        .setTextAlignment(TextAlignment.RIGHT));

                document.add(validationTable);

//                placeholder.getResources();
//                PdfCanvas canvas = new PdfCanvas(placeholder, pdfDoc);
//                canvas.getResources(); // ensures resources map is initialized
//                canvas.beginText()
//                        .setFontAndSize(calibriFont, 10)
//                        .moveText(0,0)
//                        .showText(String.valueOf(pdfDoc.getNumberOfPages()))
//                        .endText();
//                canvas.release();
                handler.writeTotal(pdfDoc); // then write total page count into placeholder

                document.close();
                pdfDoc.close();
            }
        }
    }
}


