package de.unimarburg.samplemanagement.UI.sample;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.model.Sample;
import de.unimarburg.samplemanagement.model.SampleDelivery;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.service.SampleService;
import de.unimarburg.samplemanagement.utils.GENERAL_UTIL;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Route("/VerifySampleDelivery")
public class VerifySampleDelivery extends HorizontalLayout {

    private final ClientStateService clientStateService;
    private final SampleService sampleService;
    private Study study;
    private SampleDelivery sampleDelivery;
    private Grid<Sample> sampleGrid;
    private List<Sample> unvalidatedSamples;
    private List<Sample> validatedSamples;
    private Tabs sampleTabs;
    private Tab unvalidatedTab;
    private Tab validatedTab;
    private VerticalLayout content;
    private Span allSamplesValidatedText;

    @Autowired
    public VerifySampleDelivery(ClientStateService clientStateService, SampleService sampleService) {
        this.clientStateService = clientStateService;
        this.sampleService = sampleService;
        this.study = clientStateService.getClientState().getSelectedStudy();
        this.sampleDelivery = clientStateService.getClientState().getSelectedSampleDelivery();
        add(SIDEBAR_FACTORY.getSidebar(clientStateService.getClientState().getSelectedStudy()));
        if (study == null) {
            add("No study selected");
            return;
        }

        if (sampleDelivery == null) {
            List<SampleDelivery> deliveries = study.getSampleDeliveryList();
            if (deliveries == null || deliveries.isEmpty()) {
                add("No sample deliveries available for this study.");
                return;
            }
            sampleDelivery = deliveries.get(deliveries.size() - 1);
        }

        initContent();
        add(content);
    }

    

    private void initContent() {
        content = new VerticalLayout();

        // SampleDelivery selection
        content.add(new Text("Verify Sample Delivery:"));
        Select<SampleDelivery> deliveryFilter = new Select<>();
        deliveryFilter.setLabel("Select Delivery to verify");
        deliveryFilter.setItems(study.getSampleDeliveryList());
        deliveryFilter.setEmptySelectionAllowed(true);
        deliveryFilter.setRenderer(new TextRenderer<>(sd -> GENERAL_UTIL.toOrdinal(sd.getRunningNumber()) + " delivery"));
        deliveryFilter.setValue(sampleDelivery);
        deliveryFilter.addValueChangeListener(e -> {
            sampleDelivery = e.getValue();
            clientStateService.getClientState().setSelectedSampleDelivery(sampleDelivery);
            updateSampleLists();
            updateGrid();
        });

        initTabs();
        initGrid();

        allSamplesValidatedText = new Span("All samples from this delivery have been validated and can be seen in the validated tab.");
        allSamplesValidatedText.setVisible(false);

        // Verification textfield
        TextField verificationField = new TextField();
        verificationField.setLabel("Verification");
        verificationField.setPlaceholder("Enter verification here");
        verificationField.addKeyPressListener(Key.ENTER, e -> {
            String verification = verificationField.getValue();
            if (sampleGrid.getSelectedItems().isEmpty()) {
                Notification.show("No sample selected for verification.");
                return;
            }
            String barcode = sampleGrid.getSelectedItems().iterator().next().getSample_barcode();
            if (verification.equals(barcode)) {
                verificationSuccess();
                verificationField.clear();
            } else {
                Notification.show("Verification failed, mismatch between barcode and verification");
            }
        });

        content.add(deliveryFilter, sampleTabs, verificationField, allSamplesValidatedText, sampleGrid);
        updateSampleLists();
        updateGrid();
    }

    private void initTabs() {
        unvalidatedTab = new Tab("Unvalidated");
        validatedTab = new Tab("Validated");
        sampleTabs = new Tabs(unvalidatedTab, validatedTab);
        sampleTabs.addSelectedChangeListener(event -> updateGrid());
    }

    private void initGrid() {
        sampleGrid = new Grid<>();
        sampleGrid.addColumn(Sample::getCoordinates).setHeader("Coordinates");
        sampleGrid.addColumn(Sample::getSample_barcode).setHeader("Barcode");
        sampleGrid.addColumn(Sample::getSample_type).setHeader("Type");
    }

    private void updateSampleLists() {
        List<Sample> allSamples = sampleDelivery != null ? sampleDelivery.getSamples() : study.getListOfSamples();
        unvalidatedSamples = allSamples.stream().filter(s -> !s.isValidated()).sorted(Comparator.comparing(Sample::getCoordinates)).collect(Collectors.toList());
        validatedSamples = allSamples.stream().filter(Sample::isValidated).sorted(Comparator.comparing(Sample::getCoordinates)).collect(Collectors.toList());
    }

    private void updateGrid() {
        if (sampleTabs.getSelectedTab() == unvalidatedTab) {
            sampleGrid.setItems(unvalidatedSamples);
            allSamplesValidatedText.setVisible(unvalidatedSamples.isEmpty());
            if (!unvalidatedSamples.isEmpty()) {
                sampleGrid.select(unvalidatedSamples.get(0));
            }
        } else {
            sampleGrid.setItems(validatedSamples);
            allSamplesValidatedText.setVisible(false);
        }
        sampleGrid.getDataProvider().refreshAll();
    }

    private void verificationSuccess() {
        Sample selectedSample = sampleGrid.getSelectedItems().iterator().next();
        selectedSample.setValidated(true);
        selectedSample.setValidatedAt(new Date()); // current timestamp
        sampleService.save(selectedSample);

        updateSampleLists();
        updateGrid();

        Notification.show("Verification successful");

        if (unvalidatedSamples.isEmpty()) {
            Notification.show("All samples verified");
        }
    }
}
