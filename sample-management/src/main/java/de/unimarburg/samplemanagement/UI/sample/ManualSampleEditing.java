package de.unimarburg.samplemanagement.UI.sample;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.LocalDateToDateConverter;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.model.Sample;
import de.unimarburg.samplemanagement.model.SampleDelivery;
import de.unimarburg.samplemanagement.model.Subject;
import de.unimarburg.samplemanagement.repository.SampleRepository;
import de.unimarburg.samplemanagement.repository.SubjectRepository;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.utils.DoubleToLongConverter;
import de.unimarburg.samplemanagement.utils.GENERAL_UTIL;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;


import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Route("/ManualSampleEditing")
public class ManualSampleEditing extends HorizontalLayout {
    ClientStateService clientStateService;
    SampleRepository sampleRepository;
    SubjectRepository subjectRepository;
    @Autowired
    public ManualSampleEditing(ClientStateService clientStateService, SampleRepository sampleRepository, SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
        this.clientStateService = clientStateService;
        this.sampleRepository = sampleRepository;
        add(SIDEBAR_FACTORY.getSidebar(clientStateService.getClientState().getSelectedStudy()));
        if (clientStateService.getClientState().getSelectedStudy() == null) {
            add("Please select a study");
            return;
        }
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.add(getContent());//todo implement manual sample editing


        add(verticalLayout);
    }

    private boolean isSampleValid(Sample selectedSample) {
        if (selectedSample == null) {
            return true;
        }
        if (selectedSample.getSample_barcode() == null || selectedSample.getSample_barcode().isBlank()) {
            return false;
        }
        if (selectedSample.getSample_type() == null || selectedSample.getSample_type().isBlank()) {
            return false;
        }
        if (selectedSample.getSample_amount() == null || selectedSample.getSample_amount().isBlank()) {
            return false;
        }
        if (selectedSample.getDateOfShipment() == null) {
            return false;
        }
        return true;
    }

    private VerticalLayout getContent() {
        VerticalLayout body = new VerticalLayout();
        Grid<Sample> sampleGrid = new Grid<>();
        List<Sample> samples = sampleRepository.getSampleByStudyIdOrderByIdAsc(clientStateService.getClientState().getSelectedStudy().getId());
        sampleGrid.setItems(samples);


        // Define columns
        Grid.Column<Sample> barcodeColumn = sampleGrid.addColumn(Sample::getSample_barcode).setHeader("Sample Barcode").setSortable(true);
        Grid.Column<Sample> typeColumn = sampleGrid.addColumn(Sample::getSample_type).setHeader("Sample Type").setSortable(true);
        Grid.Column<Sample> amountColumn = sampleGrid.addColumn(Sample::getSample_amount).setHeader("Sample Amount (in μl)").setSortable(true);
        Grid.Column<Sample> shipmentDateColumn = sampleGrid
                .addColumn(Sample::getDateOfShipment)
                .setHeader("Date of Shipment")
                .setSortable(true)
                .setRenderer(GENERAL_UTIL.renderDate());
        Grid.Column<Sample> validatedAtColumn = sampleGrid
                .addColumn(sample -> {
                    if (sample.isValidated() && sample.getValidatedAt() != null) {
                        return new SimpleDateFormat("dd.MM.yyyy").format(sample.getValidatedAt());
                    } else {
                        return "Not validated";
                    }
                })
                .setHeader("Validated At")
                .setSortable(true);

        Grid.Column<Sample> sampleDelivery = sampleGrid.addColumn(sample -> {
            SampleDelivery sampleDelivery1 = sample.getSampleDelivery();
            if (sampleDelivery1 == null) {
                return null;
            }
            return sample.getSampleDelivery().getRunningNumber();
        }).setHeader("Sample Delivery").setSortable(true);
        Grid.Column<Sample> coordinatesColumn = sampleGrid.addColumn(Sample::getCoordinates).setHeader("Coordinates").setSortable(true);

        // Create the editor and its binder
        Editor<Sample> editor = sampleGrid.getEditor();
        Binder<Sample> binder = new Binder<>(Sample.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        // Define editor components for each column
        TextField barcodeField = new TextField();
        binder.bind(barcodeField, Sample::getSample_barcode, Sample::setSample_barcode);
        barcodeColumn.setEditorComponent(barcodeField);

        TextField typeField = new TextField();
        binder.bind(typeField, Sample::getSample_type, Sample::setSample_type);
        typeColumn.setEditorComponent(typeField);

        TextField amountField = new TextField();
        binder.bind(amountField, Sample::getSample_amount, Sample::setSample_amount);
        amountColumn.setEditorComponent(amountField);

        // Editor for shipment date
        DatePicker shipmentDateField = new DatePicker();
        binder.forField(shipmentDateField)
                .withConverter(new LocalDateToDateConverter())
                .bind(Sample::getDateOfShipment, Sample::setDateOfShipment);
        shipmentDateColumn.setEditorComponent(shipmentDateField);

        NumberField sampleDeliveryField = new NumberField();
        sampleDeliveryField.setMin(0);
        sampleDeliveryField.setStep(1);
        binder.forField(sampleDeliveryField)
                .withConverter(new DoubleToLongConverter())
                .bind(sample -> {
                    if (sample.getSampleDelivery() == null) {
                        return null;
                    }
                    return (long) sample.getSampleDelivery().getRunningNumber();
                }, (sample, runningNumber) -> {
                    if (runningNumber == null || runningNumber<0 || runningNumber>=clientStateService.getClientState().getSelectedStudy().getSampleDeliveryList().size()){
                        Notification.show("please specify a valid sample delivery running number (between 0 and "+(clientStateService.getClientState().getSelectedStudy().getSampleDeliveryList().size()-1)+")");
                        editor.cancel();
                        return;
                    }
                    SampleDelivery sampleDeliveryForRunningNumber = clientStateService.getClientState().getSelectedStudy().getSampleDeliveryList().get(runningNumber.intValue());
                    sample.setSampleDelivery(sampleDeliveryForRunningNumber);
                    sampleDeliveryForRunningNumber.getSamples().add(sample);
                });
        sampleDelivery.setEditorComponent(sampleDeliveryField);

        TextField coordinatesField = new TextField();
        binder.bind(coordinatesField, Sample::getCoordinates, Sample::setCoordinates);
        coordinatesColumn.setEditorComponent(coordinatesField);

        //buttons
        Button saveButton = new Button("Save", event -> {
            Sample editedSample = new Sample();
            if (!binder.writeBeanIfValid(editedSample)){
                Notification.show("Please make a valid sample");
                return;
            }
            if (!isSampleValid(editedSample)){
                Notification.show("Please make a valid sample");
                return;
            }
            editor.save();
        });
        saveButton.addClickShortcut(Key.ENTER);
        saveButton.setVisible(false);
        Button discardButton = new Button("Discard");
        discardButton.addClickListener(e-> {
            //if sample was new, remove it from the list
            if (editor.getItem().getId()==null){
                samples.remove(editor.getItem());
                sampleGrid.getDataProvider().refreshAll();
            }

            editor.cancel();
            saveButton.setVisible(false);
            discardButton.setVisible(false);
        });
        discardButton.addClickShortcut(Key.ESCAPE);
        discardButton.setVisible(false);

        // Add cancel listener to discard changes
        editor.addCancelListener(event -> {
            binder.readBean(null);
        });
        // Add save listener
        editor.addSaveListener(event -> {
            Sample editedSample = event.getItem();
            if (editedSample == null) {
                return;
            }
            if (editedSample.getSubject() == null) {
                SampleDelivery delivery = editedSample.getSampleDelivery();
                if (delivery != null) {
                    Optional<Subject> subjectOpt = delivery.getSamples().stream()
                            .filter(s -> s.getSubject() != null)
                            .map(Sample::getSubject)
                            .findFirst();

                    if (subjectOpt.isPresent()) {
                        editedSample.setSubject(subjectOpt.get());
                    } else {
                        // No other sample in the delivery has a subject, so create a new one.
                        long alias;
                        do {
                            alias = (long) (10000 + new Random().nextInt(90000));
                        } while (subjectRepository.getSubjectByAliasAndStudy(alias, editedSample.getStudy()).isPresent());

                        Subject newSubject = new Subject(alias, editedSample.getStudy());
                        newSubject = subjectRepository.save(newSubject);
                        editedSample.setSubject(newSubject);
                    }
                } else {
                    Notification.show("Please select a sample delivery");
                    editor.editItem(editedSample);
                    return;
                }
            }
            try {
                sampleRepository.save(editedSample);
            } catch (Exception e){
                Notification.show("Error saving item, please check if all necessary fields are filled in correctly");
                editor.editItem(editedSample); // Reopen the editor
                return;
            }
            saveButton.setVisible(false);
            discardButton.setVisible(false);
        });
        // Allow editing on double-click
        sampleGrid.addItemDoubleClickListener(event -> {
            //save currently edited item
            try {
                editor.save();
            } catch (Exception e) {
                Notification.show("Error saving item, changes are discarded");
                if (editor.getItem().getId()==null){
                    samples.remove(editor.getItem());
                    sampleGrid.getDataProvider().refreshAll();
                }
                editor.cancel();
            }
            //edit new item
            editor.editItem(event.getItem());

            saveButton.setVisible(true);
            discardButton.setVisible(true);
        });

        Button addButton = new Button("Add Sample", event -> {
            if (sampleGrid.getEditor().isOpen()){
                Notification.show("Please finish editing the current sample first");
                return;
            }
            Sample newSample = new Sample();
            newSample.setStudy(clientStateService.getClientState().getSelectedStudy());
            samples.add(0,newSample); // Add the new sample to the list
            sampleGrid.getDataProvider().refreshAll(); // Refresh the grid

            // save the old item
            try {
                editor.save();
            } catch (Exception e) {
                Notification.show("Error saving item, changes are discarded");
                if (editor.getItem().getId()==null){
                    samples.remove(editor.getItem());
                    sampleGrid.getDataProvider().refreshAll();
                }
                editor.cancel();
            }
            //edit new item
            editor.editItem(newSample);

            saveButton.setVisible(true);
            discardButton.setVisible(true);
        });


        body.add(new HorizontalLayout(addButton, saveButton, discardButton));
        body.add(sampleGrid);
        return body;
    }

}
