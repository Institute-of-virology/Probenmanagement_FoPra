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
import de.unimarburg.samplemanagement.model.Analysis;
import de.unimarburg.samplemanagement.model.AnalysisType;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.repository.AnalysisRepository;
import de.unimarburg.samplemanagement.repository.AnalysisTypeRepository;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.service.StudyService;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

@Route("/AddSampleAnalysisToStudy")
public class AddAnalysisToStudy extends HorizontalLayout {

    private final AnalysisTypeRepository analysisTypeRepository;
    private final AnalysisRepository analysisRepository;
    private final StudyService studyService;
    private final ClientStateService clientStateService;

    private Study study;
    private Grid<AnalysisType> analysisTypeGrid;

    // Reuse fields for create/edit
    private TextField analysisName;
    private TextArea analysisDescription;
    private TextField analysisUnit;
    private Button addAnalysisButton;
    private Button updateButton;
    private Button cancelButton;

    // Keep track of current edit
    private AnalysisType editingAnalysis = null;

    @Autowired
    public AddAnalysisToStudy(ClientStateService clientStateService,
                              AnalysisTypeRepository analysisTypeRepository,
                              AnalysisRepository analysisRepository,
                              StudyService studyService) {
        this.clientStateService = clientStateService;
        this.analysisTypeRepository = analysisTypeRepository;
        this.analysisRepository = analysisRepository;
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
        // Create/Edit AnalysisType
        // ------------------------------
        VerticalLayout addAnalysisLayout = new VerticalLayout();
        addAnalysisLayout.setPadding(true);
        addAnalysisLayout.setSpacing(true);
        addAnalysisLayout.setWidth("500px");

        // Fields
        analysisName = new TextField("Analysis Name");
        analysisName.setWidthFull();
        analysisName.setPlaceholder("Enter analysis name");

        analysisDescription = new TextArea("Analysis Description");
        analysisDescription.setWidthFull();
        analysisDescription.setPlaceholder("Enter analysis description...");
        analysisDescription.setHeight("150px");

        analysisUnit = new TextField("Analysis Unit");
        analysisUnit.setWidthFull();
        analysisUnit.setPlaceholder("e.g. mg/L, cells/mL");

        // Buttons
        addAnalysisButton = new Button("Create new Analysis Type", e -> createAnalysisType());
        updateButton = new Button("Update", e -> updateAnalysisType());
        cancelButton = new Button("Cancel", e -> resetForm());

        // Initially show only create button
        HorizontalLayout buttonLayout = new HorizontalLayout(addAnalysisButton, updateButton, cancelButton);
        updateButton.setVisible(false);
        cancelButton.setVisible(false);

        addAnalysisLayout.add(analysisName, analysisDescription, analysisUnit, buttonLayout);
        body.add(addAnalysisLayout);
        body.add(new Text("--------------------------------------------------------------------------------------------------------------------------"));

        // ------------------------------
// AnalysisType Grid
// ------------------------------
        analysisTypeGrid = new Grid<>();
        analysisTypeGrid.setItems(analysisTypeRepository.findAll());
        analysisTypeGrid.addColumn(AnalysisType::getAnalysisName).setHeader("Analysis Name");
        analysisTypeGrid.addColumn(AnalysisType::getAnalysisDescription).setHeader("Analysis Description");
        analysisTypeGrid.addColumn(AnalysisType::getAnalysisUnit).setHeader("Analysis Unit");

// Actions column (all buttons together)
        analysisTypeGrid.addComponentColumn(analysisType -> {
            HorizontalLayout actions = new HorizontalLayout();

            // Add Analysis button
            Button addButton = new Button("Add Analysis");
            addButton.addClickListener(e -> {
                study.getAnalysisTypes().add(analysisType);
                studyService.save(study);

                study = studyService.getStudyById(study.getId());
                analysisTypeGrid.getDataProvider().refreshItem(analysisType);

                Notification.show("Analysis Type added successfully");
            });
            if (study.getAnalysisTypes().contains(analysisType)) {
                addButton.setEnabled(false);
                addButton.setText("already added");
            }

            // Remove from study button
            Button removeButton = new Button(new Icon(VaadinIcon.TRASH));
            removeButton.addClickListener(event -> {
                Dialog confirmDialog = new Dialog();
                confirmDialog.add(new Text("Remove '" + analysisType.getAnalysisName() + "' from this study?"));

                Button confirm = new Button("Remove", e -> {
                    study.getAnalysisTypes().remove(analysisType);
                    studyService.save(study);

                    study = studyService.getStudyById(study.getId());
                    analysisTypeGrid.getDataProvider().refreshAll();

                    Notification.show("Analysis removed from study");
                    confirmDialog.close();
                });

                Button cancel = new Button("Cancel", e -> confirmDialog.close());
                confirmDialog.add(new HorizontalLayout(confirm, cancel));
                confirmDialog.open();
            });

            // Edit button
            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addClickListener(event -> {
                editingAnalysis = analysisType;
                analysisName.setValue(analysisType.getAnalysisName());
                analysisDescription.setValue(analysisType.getAnalysisDescription() != null ? analysisType.getAnalysisDescription() : "");
                analysisUnit.setValue(analysisType.getAnalysisUnit() != null ? analysisType.getAnalysisUnit() : "");

                addAnalysisButton.setVisible(false);
                updateButton.setVisible(true);
                cancelButton.setVisible(true);
            });

            // Delete from DB button
            Button deleteButton = new Button(new Icon(VaadinIcon.CLOSE_CIRCLE));
            deleteButton.addClickListener(event -> {
                Dialog confirmDialog = new Dialog();
                confirmDialog.add(new Text("Delete analysis '" + analysisType.getAnalysisName() + "' from database?"));

                Button confirm = new Button("Delete", e -> {
                    long attachedAnalysisCount = analysisRepository.countByAnalysisType(analysisType);
                    if (attachedAnalysisCount > 0) {
                        Notification.show("Cannot delete. This Analysis Type is used in " + attachedAnalysisCount + " analyses.");
                        return;
                    }
                    long attachedStudyCount = studyService.countByAnalysisTypesContaining(analysisType);
                    if (attachedStudyCount > 0) {
                        Notification.show("Cannot delete. This Analysis Type is used in " + attachedStudyCount + " studies.");
                    }

                    analysisTypeRepository.delete(analysisType);

                    study.getAnalysisTypes().remove(analysisType);
                    studyService.save(study);

                    analysisTypeRepository.delete(analysisType);

                    study = studyService.getStudyById(study.getId());
                    analysisTypeGrid.setItems(analysisTypeRepository.findAll());

                    Notification.show("Analysis Type deleted permanently");
                    confirmDialog.close();
                });

                Button cancel = new Button("Cancel", e -> confirmDialog.close());
                confirmDialog.add(new HorizontalLayout(confirm, cancel));
                confirmDialog.open();
            });

            actions.add(addButton, removeButton, editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setAutoWidth(false).setFlexGrow(2);

        body.add(analysisTypeGrid);
        return body;
    }

    // ------------------------------
    // Methods
    // ------------------------------

    private void createAnalysisType() {
        if (analysisName.isEmpty() || analysisTypeRepository.existsByAnalysisName(analysisName.getValue())) {
            Notification.show("Name of Analysis invalid/duplicate");
            return;
        }

        AnalysisType analysisType = new AnalysisType();
        analysisType.setAnalysisName(analysisName.getValue());
        analysisType.setAnalysisDescription(analysisDescription.getValue());
        analysisType.setAnalysisUnit(analysisUnit.getValue());
        analysisType = analysisTypeRepository.save(analysisType);

        study.getAnalysisTypes().add(analysisType);
        studyService.save(study);

        study = studyService.getStudyById(study.getId());
        analysisTypeGrid.setItems(study.getAnalysisTypes());

        resetForm();
        Notification.show("Analysis Type added successfully");
    }

    private void updateAnalysisType() {
        if (editingAnalysis == null) return;

        if (analysisName.isEmpty()) {
            Notification.show("Name cannot be empty");
            return;
        }

        editingAnalysis.setAnalysisName(analysisName.getValue());
        editingAnalysis.setAnalysisDescription(analysisDescription.getValue());
        editingAnalysis.setAnalysisUnit(analysisUnit.getValue());

        analysisTypeRepository.save(editingAnalysis);

        study = studyService.getStudyById(study.getId());
        analysisTypeGrid.setItems(analysisTypeRepository.findAll());

        resetForm();
        Notification.show("Analysis Type updated successfully");
    }

    private void resetForm() {
        analysisName.clear();
        analysisDescription.clear();
        analysisUnit.clear();
        editingAnalysis = null;

        addAnalysisButton.setVisible(true);
        updateButton.setVisible(false);
        cancelButton.setVisible(false);
    }
}
