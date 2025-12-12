package de.unimarburg.samplemanagement.UI.study;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
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
import de.unimarburg.samplemanagement.repository.StudyRepository;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.service.PdfReportService;
import de.unimarburg.samplemanagement.utils.GENERAL_UTIL;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Route("/CreateReport")
public class CreateStudyReport extends HorizontalLayout {

    private final StudyRepository studyRepository;
    private final PdfReportService pdfReportService;
    private Study study;
    private Map<AnalysisType, Boolean> analysisCheckboxMap = new HashMap<>();
    private Map<SampleDelivery, Boolean> sampleDeliveriesCheckboxMap = new HashMap<>();
    Grid<Sample> sampleGrid;
    private Button printPdfButton;

    private Anchor downloadLink;

    @Autowired
    public CreateStudyReport(ClientStateService clientStateService, StudyRepository studyRepository, PdfReportService pdfReportService) {
        this.studyRepository = studyRepository;
        this.pdfReportService = pdfReportService;
        setSizeFull();
        add(SIDEBAR_FACTORY.getSidebar(clientStateService.getClientState().getSelectedStudy()));
        if (clientStateService.getClientState().getSelectedStudy() == null) {
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
            }).setHeader(analysisType.getAnalysisName() + " (" + analysisType.getAnalysisUnit() + ")");
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
            Div labelRunningNumber = new Div(GENERAL_UTIL.toOrdinal(sampleDelivery.getRunningNumber()) + " delivery");
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
        Button openDialogButton = new Button("Open Report Generation Dialog");
        generateReportLayout.add(openDialogButton);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Generate Report");

        VerticalLayout dialogLayout = new VerticalLayout();
        List<String> senders = new ArrayList<>();
        if (study.getSender1() != null && !study.getSender1().isEmpty()) {
            senders.add(study.getSender1());
        }
        if (study.getSender2() != null && !study.getSender2().isEmpty()) {
            senders.add(study.getSender2());
        }
        if (study.getSender3() != null && !study.getSender3().isEmpty()) {
            senders.add(study.getSender3());
        }

        ComboBox<String> senderComboBox = new ComboBox<>("Select Sender");
        senderComboBox.setItems(senders);

        printPdfButton = new Button("Create Report");
        printPdfButton.setEnabled(false);

        senderComboBox.addValueChangeListener(event -> printPdfButton.setEnabled(event.getValue() != null));

        printPdfButton.addClickListener(event -> {
            try {
                String selectedSender = senderComboBox.getValue();
                if (selectedSender == null) {
                    Notification.show("Please select a sender.");
                    return;
                }

                List<AnalysisType> selectedAnalysisTypes = analysisCheckboxMap.keySet().stream()
                        .filter(analysisCheckboxMap::get)
                        .toList();
                List<SampleDelivery> selectedDeliveries = sampleDeliveriesCheckboxMap.keySet().stream()
                        .filter(sampleDeliveriesCheckboxMap::get)
                        .toList();

                ByteArrayInputStream pdfStream = pdfReportService.generatePdf(study, selectedSender, selectedAnalysisTypes, selectedDeliveries);

                StreamResource resource = new StreamResource("study_report.pdf", () -> pdfStream);

                if (downloadLink == null) {
                    downloadLink = new Anchor(resource, "Download PDF");
                    downloadLink.getElement().setAttribute("download", true);
                    generateReportLayout.add(downloadLink);
                } else {
                    downloadLink.setHref(resource);
                }
                dialog.close();
            } catch (Exception e) {
                Notification.show("Error generating PDF: " + e.getMessage());
                e.printStackTrace();
            }
        });

        dialogLayout.add(senderComboBox, printPdfButton);
        dialog.add(dialogLayout);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton);

        openDialogButton.addClickListener(event -> dialog.open());
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
}