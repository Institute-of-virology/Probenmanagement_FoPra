package de.unimarburg.samplemanagement.UI.study;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import de.unimarburg.samplemanagement.model.Study;
import org.springframework.stereotype.Component;


@Component
public class StudyForm extends FormLayout {
    BeanValidationBinder<Study> binder = new BeanValidationBinder<>(Study.class);
    TextField studyName = new TextField("Study Name");
    DatePicker startDate = new DatePicker("Start Date");
    TextField expectedNumberOfSubjects = new TextField("Expected Number Of Subjects");
    TextField expectedNumberOfSampleDeliveries = new TextField("Expected Number Of Sample Deliveries");
    TextArea sender1 = new TextArea("Sender1");
    TextArea sender2 = new TextArea("Sender2");
    TextArea sender3 = new TextArea("Sender3");
    TextArea sponsor = new TextArea("Sponsor");
    TextField remark = new TextField("Remarks");
    DatePicker endDate = new DatePicker("End Date");

    Button save = new Button("Save");
    Button delete = new Button("Delete");
    Button close = new Button("Cancel");

    public StudyForm() {
        DatePicker.DatePickerI18n singleFormatI18n = new DatePicker.DatePickerI18n();
        singleFormatI18n.setDateFormat("yyyy/MM/dd");
        startDate.setI18n(singleFormatI18n);
        endDate.setI18n(singleFormatI18n);
        binder.bindInstanceFields(this);
        // Add a label to show the date format pattern
        add(studyName,
                startDate,endDate, expectedNumberOfSubjects, expectedNumberOfSampleDeliveries,sender1,sender2,sender3,sponsor,remark,
                createButtonsLayout());
    }
    public void setStudy(Study study) {
        binder.setBean(study);
    }

    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        close.addClickShortcut(Key.ESCAPE);
        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));

        return new HorizontalLayout(save, delete, close);
    }

    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, binder.getBean()));
        }

    }

    // Events
    public static abstract class StudyFormEvent extends ComponentEvent<StudyForm> {
        private final Study study;

        protected StudyFormEvent(StudyForm source, Study study) {
            super(source, false);
            this.study = study;
        }

        public Study getStudy() {
            return study;
        }
    }

    public static class SaveEvent extends StudyFormEvent {
        SaveEvent(StudyForm source, Study study) {
            super(source, study);
        }
    }

    public static class DeleteEvent extends StudyFormEvent {
        DeleteEvent(StudyForm source, Study study) {
            super(source, study);
        }

    }

    public static class CloseEvent extends StudyFormEvent {
        CloseEvent(StudyForm source) {
            super(source, null);
        }
    }

    public void addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        addListener(DeleteEvent.class, listener);
    }

    public void addSaveListener(ComponentEventListener<SaveEvent> listener) {
        addListener(SaveEvent.class, listener);
    }
    public void addCloseListener(ComponentEventListener<CloseEvent> listener) {
        addListener(CloseEvent.class, listener);
    }
}