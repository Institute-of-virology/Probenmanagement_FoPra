package de.unimarburg.samplemanagement.UI.analyses;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.model.Analysis;
import de.unimarburg.samplemanagement.model.AnalysisType;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.repository.AnalysisRepository;
import de.unimarburg.samplemanagement.repository.SampleRepository;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.utils.DISPLAY_UTILS;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route("/EnterSampleAnalysis")
public class InputAnalysisResult extends HorizontalLayout {
    private final SampleRepository sampleRepository;
    private final AnalysisRepository analysisRepository;
    private ClientStateService clientStateService;
    private Study study;
    private AnalysisType selectedAnalysisType = null;


    @Autowired
    public InputAnalysisResult(ClientStateService clientStateService, SampleRepository sampleRepository, AnalysisRepository analysisRepository) {
        this.clientStateService = clientStateService;
        this.sampleRepository = sampleRepository;
        this.analysisRepository = analysisRepository;
        add(SIDEBAR_FACTORY.getSidebar(clientStateService.getClientState().getSelectedStudy()));
        study = clientStateService.getClientState().getSelectedStudy();
        if (clientStateService.getClientState().getSelectedStudy() == null) {
            add("Please select a Study");
            return;
        }
        add(loadContent());
    }

    private VerticalLayout loadContent() {
        VerticalLayout body = new VerticalLayout();

        // Deduplicate analysis types by name
        List<AnalysisType> uniqueAnalysisTypes = study.getAnalysisTypes().stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(AnalysisType::getId, at ->
                                at, (a, b) -> a),
                        map -> new java.util.ArrayList<>(map.values())
                ));

        List<Button> analysisSelectionButtons = uniqueAnalysisTypes.stream()
                .map(analysisType -> {
                    String buttonText = analysisType.getAnalysisName();
                    if (analysisType.getAnalysisUnit() != null && !analysisType.getAnalysisUnit().isEmpty()) {
                        buttonText += " (" + analysisType.getAnalysisUnit() + ")";
                    }
                    Button button = new Button(buttonText);
                    button.addClickListener(e -> {
                        selectedAnalysisType = analysisType;
                        body.removeAll();
                        body.add(loadAnalysisTypeContent());
                    });
                    return button;
                }).toList();

        body.add(DISPLAY_UTILS.getBoxAlignment(analysisSelectionButtons.toArray(new Button[0])));
        return body;
    }


    private Component loadAnalysisTypeContent() {
        Grid<Analysis> analysisGrid = new Grid<>();
        List<Analysis> relevantAnalyses = study.getListOfSamples().stream()
                .flatMap(sample -> sample.getListOfAnalysis().stream())
                .filter(analysis -> analysis.getAnalysisType().getId().equals(selectedAnalysisType.getId()))
                .toList();

        analysisGrid.setItems(relevantAnalyses);

        analysisGrid.addColumn(analysis -> analysis.getSample().getSample_barcode()).setHeader("Sample Barcode");
        //editable result column
        String header = selectedAnalysisType.getAnalysisName();
        if (selectedAnalysisType.getAnalysisUnit() != null && !selectedAnalysisType.getAnalysisUnit().isEmpty()) {
            header += " (" + selectedAnalysisType.getAnalysisUnit() + ")";
        }
        analysisGrid.addComponentColumn(analysis -> {
            TextField textField = new TextField();
            String analysisResult = analysis.getAnalysisResult();
            if (analysisResult == null) {
                analysisResult = "";
            }
            textField.setValue(analysisResult);
            textField.addValueChangeListener(e -> {
                saveNewAnalysisResult(analysis, e.getValue());
            });
            return textField;
        }).setHeader(header);


        return analysisGrid;
    }


    private void saveNewAnalysisResult(Analysis analysis, String value) {
        analysis.setAnalysisResult(value);
        analysisRepository.save(analysis);
        sampleRepository.save(analysis.getSample());
        Notification.show("Result Saved");
    }

}