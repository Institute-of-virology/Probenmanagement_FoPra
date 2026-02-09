package de.unimarburg.samplemanagement.UI.legal;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.utils.GENERAL_UTIL;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;

@Route("/datenschutzerklaerung")
public class Datenschutzerklaerung extends HorizontalLayout {

    private final String sourcefile = "legal/dataprotection.md";
    private Div viewContent;
    private TextArea editContent;
    private Button editButton;
    private Button saveButton;
    private Button cancelButton;
    private HorizontalLayout buttonLayout;

    private final GENERAL_UTIL generalUtil; // Inject GENERAL_UTIL

    @Autowired
    public Datenschutzerklaerung(GENERAL_UTIL generalUtil) { // Constructor injection
        this.generalUtil = generalUtil;

        add(SIDEBAR_FACTORY.getSidebar(null));
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();

        // View mode components
        viewContent = new Div();
        viewContent.setSizeFull();

        // Edit mode components
        editContent = new TextArea();
        editContent.setSizeFull();
        editContent.setVisible(false);

        editButton = new Button("Edit");
        editButton.addClickListener(e -> switchToEditMode());

        saveButton = new Button("Save");
        saveButton.addClickListener(e -> saveChanges());

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> switchToViewMode());

        buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setVisible(false);

        // Set initial visibility based on role
        boolean isAdmin = GENERAL_UTIL.hasRole("ADMIN"); // Still using static hasRole
        editButton.setVisible(isAdmin);
        buttonLayout.setVisible(false); // Ensure buttons are hidden initially

        contentLayout.add(viewContent, editButton, editContent, buttonLayout); // Changed order
        add(contentLayout);

        loadContent();
    }

    private void loadContent() {
        String contentMD = generalUtil.readLegalFileToString(sourcefile); // Use non-static method
        String contentHTML = GENERAL_UTIL.markdownToHtml(contentMD); // markdownToHtml is still static
        viewContent.removeAll();
        viewContent.add(new Html("<div>" + contentHTML + "</div>"));
        editContent.setValue(contentMD);
    }

    private void switchToEditMode() {
        viewContent.setVisible(false);
        editButton.setVisible(false);
        editContent.setVisible(true);
        buttonLayout.setVisible(true);
    }

    private void switchToViewMode() {
        loadContent(); // Reload to ensure latest content is shown (in case of cancel)
        viewContent.setVisible(true);
        editButton.setVisible(GENERAL_UTIL.hasRole("ADMIN")); // Re-check role on switch
        editContent.setVisible(false);
        buttonLayout.setVisible(false);
    }

    private void saveChanges() {
        try {
            generalUtil.writeLegalFileToString(sourcefile, editContent.getValue()); // Use non-static method
            Notification.show("Content saved successfully!", 3000, Notification.Position.BOTTOM_START);
            switchToViewMode();
        } catch (IOException e) { // URISyntaxException is no longer thrown by writeLegalFileToString
            Notification.show("Error saving content: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START);
            e.printStackTrace();
        }
    }
}
