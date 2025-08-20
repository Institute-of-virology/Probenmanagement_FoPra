package de.unimarburg.samplemanagement.UI.analyses;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.model.AnalysisType;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.repository.AnalysisTypeRepository;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.service.StudyService;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

@Route("/AddSampleAnalysisToStudy")
public class AddAnalysisToStudy extends HorizontalLayout {

    private final AnalysisTypeRepository analysisTypeRepository;
    private final StudyService studyService;
    private final ClientStateService clientStateService;

    private Study study;
    private Grid<AnalysisType> analysisTypeGrid;

    @Autowired
    public AddAnalysisToStudy(ClientStateService clientStateService,
                              AnalysisTypeRepository analysisTypeRepository,
                              StudyService studyService) {
        this.clientStateService = clientStateService;
        this.analysisTypeRepository = analysisTypeRepository;
        this.studyService = studyService;

        this.study = clientStateService.getClientState().getSelectedStudy();

        add(SIDEBAR_FACTORY.getSidebar(study));

        if (study == null) {
            add("Please select a Study");
            return;
        }

        add(loadContent());
    }

    private VerticalLayout loadContent() {
        VerticalLayout body = new VerticalLayout();

// ------------------------------
// Create new AnalysisType
// ------------------------------
        VerticalLayout addAnalysisLayout = new VerticalLayout();
        addAnalysisLayout.setPadding(true);
        addAnalysisLayout.setSpacing(true);
        addAnalysisLayout.setWidth("500px");

// Bigger Analysis Name field
        TextField analysisName = new TextField("Analysis Name");
        analysisName.setWidthFull();
        analysisName.setPlaceholder("Enter analysis name");

// Multi-line Analysis Description
        TextArea analysisDescription = new TextArea("Analysis Description");
        analysisDescription.setWidthFull();
        analysisDescription.setPlaceholder("Enter analysis description...");
        analysisDescription.setHeight("150px");

// Add button
        Button addAnalysisButton = new Button("Create new Analysis Type");
        addAnalysisButton.addClickListener(buttonClickEvent -> {
            //check validity
            if (analysisName.isEmpty() || analysisTypeRepository.existsByAnalysisName(analysisName.getValue())) {
                Notification.show("Name of Analysis invalid/duplicate");
                return;
            }

            AnalysisType analysisType = new AnalysisType();
            analysisType.setAnalysisName(analysisName.getValue());
            analysisType.setAnalysisDescription(analysisDescription.getValue());
            analysisType = analysisTypeRepository.save(analysisType);
            study.getAnalysisTypes().add(analysisType);
            studyService.save(study);

            // Refresh grid
            study = studyService.getStudyById(study.getId());
            analysisTypeGrid.setItems(study.getAnalysisTypes());

            // Clear fields
            analysisName.clear();
            analysisDescription.clear();

            Notification.show("Analysis Type added successfully");
        });

        addAnalysisLayout.add(analysisName, analysisDescription, addAnalysisButton);
        body.add(addAnalysisLayout);
        body.add(new Text("--------------------------------------------------------------------------------------------------------------------------"));


        // ------------------------------
        // AnalysisType Grid
        // ------------------------------
        analysisTypeGrid = new Grid<>();
        analysisTypeGrid.setItems(analysisTypeRepository.findAll());
        analysisTypeGrid.addColumn(AnalysisType::getAnalysisName).setHeader("Analysis Name");
        analysisTypeGrid.addColumn(AnalysisType::getAnalysisDescription).setHeader("Analysis Description");

        // Add Analysis button
        analysisTypeGrid.addComponentColumn(analysisType -> {
            Button button = new Button("Add Analysis");
            button.addClickListener(buttonClickEvent -> {
                study.getAnalysisTypes().add(analysisType);
                studyService.save(study);

                // refresh only this row
                study = studyService.getStudyById(study.getId());
                analysisTypeGrid.getDataProvider().refreshItem(analysisType);

                Notification.show("Analysis Type added successfully");
            });

            // disable if already linked
            if (study.getAnalysisTypes().contains(analysisType)) {
                button.setEnabled(false);
                button.setText("already added");
            }
            return button;
        }).setHeader("Add Analysis");

        // Delete button with confirmation dialog
        analysisTypeGrid.addComponentColumn(analysisType -> {
            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addClickListener(event -> {
                Dialog confirmDialog = new Dialog();
                confirmDialog.add(new Text("Are you sure you want to remove analysis '"
                        + analysisType.getAnalysisName() + "' from this study?"));

                Button confirm = new Button("Remove", e -> {
                    // remove from study only (not from global repo)
                    study.getAnalysisTypes().remove(analysisType);
                    studyService.save(study);

                    // refresh study and grid
                    study = studyService.getStudyById(study.getId());
                    analysisTypeGrid.getDataProvider().refreshAll();

                    Notification.show("Analysis removed from study");
                    confirmDialog.close();
                });

                Button cancel = new Button("Cancel", e -> confirmDialog.close());

                HorizontalLayout dialogButtons = new HorizontalLayout(confirm, cancel);
                confirmDialog.add(dialogButtons);
                confirmDialog.open();
            });
            return deleteButton;
        }).setHeader("Remove from Study");

        body.add(analysisTypeGrid);

        return body;
    }
}
