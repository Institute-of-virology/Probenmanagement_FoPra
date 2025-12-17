package de.unimarburg.samplemanagement.UI.analyses;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.model.AnalysisType;
import de.unimarburg.samplemanagement.model.Sample;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.utils.GENERAL_UTIL;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Route("/ViewSampleAnalysis")
public class AnalysisResultView extends HorizontalLayout {
    private ClientStateService clientStateService;
    private Grid<Sample> sampleGrid;
    private List<Sample> samples;

    @Autowired
    public AnalysisResultView(ClientStateService clientStateService) {
        this.clientStateService = clientStateService;
        add(SIDEBAR_FACTORY.getSidebar(clientStateService.getClientState().getSelectedStudy()));
        add(loadData());
    }

    private VerticalLayout loadData() {
        VerticalLayout body = new VerticalLayout();

        Study study = clientStateService.getClientState().getSelectedStudy();

        if (study == null) {
            add("Please select a Study");
            return body;
        }

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search by Sample Barcode");
        searchField.setWidth("300px");

        Button searchButton = new Button("Search");

        HorizontalLayout searchLayout = new HorizontalLayout(searchField, searchButton);

        samples = study.getListOfSamples();
        sampleGrid = new Grid<>();
        sampleGrid.setItems(samples);

        searchButton.addClickListener(event -> {
            String searchTerm = searchField.getValue();
            if (searchTerm == null || searchTerm.isBlank()) {
                return;
            }

            Optional<Sample> foundSample = samples.stream()
                    .filter(sample -> sample.getSample_barcode().equalsIgnoreCase(searchTerm))
                    .findFirst();

            if (foundSample.isPresent()) {
                Sample sample = foundSample.get();
                // Scroll to and select the item
                int index = samples.indexOf(sample);
                if (index != -1) {
                    sampleGrid.scrollToIndex(index);
                }
                sampleGrid.select(sample);

                // Deselect after 3 seconds
                UI ui = UI.getCurrent();
                CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
                    ui.access(() -> {
                        sampleGrid.deselect(sample);
                    });
                });
            } else {
                Notification.show("No sample found with barcode: " + searchTerm);
            }
        });

        sampleGrid.addColumn(Sample::getSample_barcode).setHeader("Sample Barcode");

        // Deduplicate AnalysisTypes by name
        List<AnalysisType> uniqueAnalysisTypes = study.getAnalysisTypes().stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                AnalysisType::getAnalysisName,
                                at -> at,
                                (a, b) -> a
                        ),
                        map -> new java.util.ArrayList<>(map.values())
                ));

        for (AnalysisType analysisType : uniqueAnalysisTypes) {
            String header = analysisType.getAnalysisName();
            if (analysisType.getAnalysisUnit() != null && !analysisType.getAnalysisUnit().isEmpty()) {
                header += " (" + analysisType.getAnalysisUnit() + ")";
            }
            sampleGrid.addColumn(sample -> {
                Object result = GENERAL_UTIL.getAnalysisForSample(sample, analysisType.getId());
                String display = (result == null || result.toString().isBlank()) ? "-" : result.toString();
                return display;
            }).setHeader(header);
        }

        body.add(searchLayout, sampleGrid);
        return body;
    }
}