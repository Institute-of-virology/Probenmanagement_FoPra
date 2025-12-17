package de.unimarburg.samplemanagement.service;

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
import de.unimarburg.samplemanagement.model.*;
import de.unimarburg.samplemanagement.repository.AddressStoreRepository;
import de.unimarburg.samplemanagement.repository.ReportAuthorRepository;
import de.unimarburg.samplemanagement.utils.GENERAL_UTIL;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class PdfReportService {

    private final AddressStoreRepository addressStoreRepository;
    private final ReportAuthorRepository reportAuthorRepository;

    public PdfReportService(AddressStoreRepository addressStoreRepository, ReportAuthorRepository reportAuthorRepository) {
        this.addressStoreRepository = addressStoreRepository;
        this.reportAuthorRepository = reportAuthorRepository;
    }

    public class HeaderFooterHandler implements IEventHandler {
        private final Image logo;
        private final PdfFormXObject placeholder;
        private final PdfFont font;
        private final int fontSize = 10;

        public HeaderFooterHandler(PdfDocument pdfDoc, Image logo, PdfFont font) {
            this.logo = logo;
            this.font = font;
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

            logo.setFixedPosition(
                    (pageSize.getWidth() - logo.getImageScaledWidth()) / 2,
                    pageSize.getTop() - logo.getImageScaledHeight() - 10
            );
            canvas.add(logo);

            Paragraph p = new Paragraph()
                    .setFont(font)
                    .setFontSize(fontSize)
                    .add("Page " + pageNumber + " of ")
                    .add(new Image(placeholder));

            canvas.showTextAligned(p,
                    pageSize.getRight() - 60,
                    pageSize.getTop() - 20,
                    TextAlignment.RIGHT);

            canvas.close();
        }

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

    public ByteArrayInputStream generatePdf(Study study, String selectedSender, List<AnalysisType> selectedAnalysisTypes, List<SampleDelivery> selectedDeliveries) throws IOException, URISyntaxException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
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
                        .setMargins(0, 0, 5, 0);
                document.add(header);

                document.add(new LineSeparator(new SolidLine()));

                float[] columnWidths = {3, 3, 4};
                Table senderTable = new Table(columnWidths);
                senderTable.setWidth(UnitValue.createPercentValue(100));
                List<ReportAuthor> reportAuthors = reportAuthorRepository.findAll();
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

                Paragraph recipient = new Paragraph("Recipient Address:\n" + selectedSender)
                        .setFont(calibriFont)
                        .setFontSize(10)
                        .setMargins(10, 0, 5, 0);
                document.add(recipient);

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                Paragraph dateOfReport = new Paragraph("Date of Report Generation: " + now.format(formatter))
                        .setFont(calibriFont)
                        .setFontSize(10)
                        .setBold()
                        .setMargins(10, 0, 5, 0);
                document.add(dateOfReport);

                Paragraph reportTitle = new Paragraph("Report on the results of the serological analysis")
                        .setFont(calibriBoldFont)
                        .setFontSize(10)
                        .setBold()
                        .setMargins(10, 0, 5, 0);
                document.add(reportTitle);

                StringBuilder methods = new StringBuilder("Analytical methods: ");
                for (AnalysisType analysisType : selectedAnalysisTypes) {
                    methods.append(analysisType.getAnalysisName()).append(" (").append(analysisType.getAnalysisUnit()).append("), ");
                }
                if (methods.length() > 0) {
                    methods.setLength(methods.length() - 2);
                }
                Paragraph analyticalMethods = new Paragraph(methods.toString())
                        .setFont(calibriFont)
                        .setFontSize(10)
                        .setMargins(10, 0, 5, 0);
                document.add(analyticalMethods);

                Paragraph studyDetails = new Paragraph()
                        .add("Study: ")
                        .add(new Text(study.getStudyName()).setBold())
                        .add(", " + (study.getStartDate() != null ? study.getStartDate().toString() : "N/A") + "-" + (study.getEndDate() != null ? study.getEndDate().toString() : "N/A"))
                        .setFont(calibriFont)
                        .setFontSize(10)
                        .setMargins(10, 0, 5, 0);
                document.add(studyDetails);

                Paragraph deliveriesTitle = new Paragraph("Sample Deliveries:")
                        .setFont(calibriBoldFont)
                        .setFontSize(10)
                        .setBold()
                        .setMargins(10, 0, 5, 0);
                document.add(deliveriesTitle);

                List<SampleDelivery> mutableDeliveries = new java.util.ArrayList<>(selectedDeliveries);
                mutableDeliveries.sort(Comparator.comparing(SampleDelivery::getRunningNumber));

                if (mutableDeliveries.isEmpty()) {
                    document.add(new Paragraph("No specific deliveries selected for this report.").setFont(calibriFont).setFontSize(10).setMargins(0,0,5,0));
                } else {
                    com.itextpdf.layout.element.List deliveryList = new com.itextpdf.layout.element.List()
                            .setSymbolIndent(12)
                            .setListSymbol("\u2022")
                            .setFont(calibriFont)
                            .setFontSize(10)
                            .setMarginLeft(20);
                    for (SampleDelivery delivery : mutableDeliveries) {
                        String deliveryInfo = String.format("%s delivery: Received on %s, %d samples.",
                            GENERAL_UTIL.toOrdinal(delivery.getRunningNumber()),
                                delivery.getDeliveryDate() != null ? new SimpleDateFormat("yyyy/MM/dd").format(delivery.getDeliveryDate()) : "N/A",
                                delivery.getSamples() != null ? delivery.getSamples().size() : 0);
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
                    table.addHeaderCell(new Cell().add(new Paragraph(analysisType.getAnalysisName() + " (" + analysisType.getAnalysisUnit() + ")")).setFont(calibriFont).setFontSize(10));
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
                    Paragraph textbaustein = new Paragraph(analysisType.getAnalysisName() + " (" + analysisType.getAnalysisUnit() + "): " + analysisType.getAnalysisDescription())
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
        return new ByteArrayInputStream(baos.toByteArray());
    }
}