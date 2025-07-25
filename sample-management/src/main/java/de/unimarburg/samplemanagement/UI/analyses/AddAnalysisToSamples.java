package de.unimarburg.samplemanagement.UI.analyses;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.model.*;
import de.unimarburg.samplemanagement.repository.SampleRepository;
import de.unimarburg.samplemanagement.repository.StudyRepository;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.utils.DISPLAY_UTILS;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route("/AddAnalysisToSample")
public class AddAnalysisToSamples extends HorizontalLayout {
    private final SampleRepository sampleRepository;
    private final StudyRepository studyRepository;
    Grid<Sample> sampleGrid = new Grid<>();
    ClientStateService clientStateService;
    Study study;


    @Autowired
    public AddAnalysisToSamples(ClientStateService clientStateService, StudyRepository studyRepository, SampleRepository sampleRepository) {
        this.clientStateService = clientStateService;
        this.sampleRepository = sampleRepository;
        this.studyRepository = studyRepository;
        add(SIDEBAR_FACTORY.getSidebar(clientStateService.getClientState().getSelectedStudy()));
        study = clientStateService.getClientState().getSelectedStudy();
        if (clientStateService.getClientState().getSelectedStudy() == null) {
            add("Please select a Study");
            return;
        }
        add(loadContent());
    }

    private void setButtonAddMode(Button button) {
        button.setText("Add");
        //set colour
        button.getStyle().set("background-color", "green");
    }

    private void setButtonRemoveMode(Button button) {
        button.setText("Remove");
        //set colour
        button.getStyle().set("background-color", "red");
    }

    private void refreshSampleGrid() {
        study = studyRepository.findById(study.getId()).orElseThrow();
        sampleGrid.setItems(study.getListOfSamples());
    }

    private VerticalLayout loadContent() {
        VerticalLayout body = new VerticalLayout();
        sampleGrid.setItems(study.getListOfSamples());

        //sample info
        sampleGrid.addColumn(Sample::getSample_barcode).setHeader("Sample Barcode");
        sampleGrid.addColumn(Sample::getSample_type).setHeader("Sample Type");
        sampleGrid.addColumn(Sample::getSample_amount).setHeader("Sample Amount");

        // get unique analysis types to avoid duplicates in the grid header
        List<AnalysisType> uniqueAnalysisTypes = study.getAnalysisTypes().stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(AnalysisType::getId, at -> at, (a, b) -> a),
                        map -> new ArrayList<>(map.values())
                ));

        //add checkbox for each analysis type
        for (AnalysisType analysisType : uniqueAnalysisTypes) {
            sampleGrid.addComponentColumn(sample -> {
                Button button = new Button("");
                if (sample.getListOfAnalysis().stream().anyMatch(a -> a.getAnalysisType().getId().equals(analysisType.getId()))) {
                    setButtonAddMode(button);
                } else {
                    setButtonRemoveMode(button);
                }

                // Set button mode on creation
                boolean analysisExists = sample.getListOfAnalysis().stream()
                        .anyMatch(a -> a.getAnalysisType().getId().equals(analysisType.getId()) &&
                                a.getSample().getId().equals(sample.getId()));
                if (analysisExists) {
                    setButtonRemoveMode(button);
                } else {
                    setButtonAddMode(button);
                }

                button.addClickListener(e -> {
                    if ("Add".equals(button.getText())) {
                        sample.getListOfAnalysis().add(new Analysis(analysisType, sample));
                        sampleRepository.save(sample);
                        setButtonRemoveMode(button);
                    } else if ("Remove".equals(button.getText())) {
                        sample.getListOfAnalysis().removeIf(a ->
                                a.getAnalysisType().getId().equals(analysisType.getId())
                        );
                        sampleRepository.save(sample);

                        refreshSampleGrid();
                        setButtonAddMode(button);
                    } else {
                        throw new RuntimeException("Unexpected button text: " + button.getText());
                    }
                });

                return button;
            }).setHeader(analysisType.getAnalysisName());
        }
        body.add(sampleGrid);

        //dropdown filter for deliveries
        HorizontalLayout filterLayout = new HorizontalLayout();
        Select<SampleDelivery> deliveryFilter = new Select<>();
        deliveryFilter.setLabel("Filter by Delivery");
        deliveryFilter.setItems(study.getSampleDeliveryList());
        deliveryFilter.setEmptySelectionAllowed(true);
        deliveryFilter.setRenderer(new TextRenderer<>(sampleDelivery -> String.valueOf(sampleDelivery.getRunningNumber())));
        deliveryFilter.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                sampleGrid.setItems(e.getValue().getSamples());
            } else {
                sampleGrid.setItems(study.getListOfSamples());
            }
        });
        filterLayout.add(deliveryFilter);

        //add-all buttons
        List<Button> add_all_buttons = new ArrayList<>();
        for (AnalysisType analysisType : uniqueAnalysisTypes) {
            Button button = new Button("Add all " + analysisType.getAnalysisName());
            button.addClickListener(e -> {
                for (Sample sample : study.getListOfSamples()) {
                    if (deliveryFilter.getValue() == null || deliveryFilter.getValue().getSamples().contains(sample)) {
                        if (sample.getListOfAnalysis().stream().noneMatch(a -> a.getAnalysisType().getId().equals(analysisType.getId()))) {
                            sample.getListOfAnalysis().add(new Analysis(analysisType, sample));
                            sampleRepository.save(sample);
                        }
                    }
                }
                sampleGrid.getDataProvider().refreshAll();
            });
            add_all_buttons.add(button);
        }
        filterLayout.add(DISPLAY_UTILS.getBoxAlignment(add_all_buttons.toArray(new Button[0])));


        body.add(filterLayout);

        return body;
    }
}
