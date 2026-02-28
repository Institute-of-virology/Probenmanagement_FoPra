package de.unimarburg.samplemanagement.UI;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import de.unimarburg.samplemanagement.model.Sample;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.repository.StudyRepository;
import de.unimarburg.samplemanagement.repository.UserRepository;
import de.unimarburg.samplemanagement.service.ClientStateService;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@Route("/")
public class Main extends HorizontalLayout {

    private final ClientStateService clientStateService;
    private final StudyRepository studyRepository;

    @Autowired
    public Main(ClientStateService clientStateService, UserRepository userRepository, StudyRepository studyRepository) {
        this.clientStateService = clientStateService;
        this.studyRepository = studyRepository;
        clientStateService.setUserState(null);

        setSizeFull();

        // Inject UserRepository into SIDEBAR_FACTORY before using it
        SIDEBAR_FACTORY.setUserRepository(userRepository);

        add(SIDEBAR_FACTORY.getSidebar(null));

        StreamResource imageResource = new StreamResource("uni-logo.png",
                () -> getClass().getResourceAsStream("/uni-logo.png"));
        Image logo = new Image(imageResource, "University Logo");
        logo.setWidth("240px");

        VerticalLayout welcomeLayout = new VerticalLayout(
                logo,
                new Text("Welcome to the Sample Management System. "),
                new Text("Select an option from the sidebar to get started :)")
        );
        welcomeLayout.setAlignItems(Alignment.CENTER);
        welcomeLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        welcomeLayout.setSizeFull();

        // VerticalLayout activeStudiesLayout = createActiveStudiesLayout();

        Anchor impressumLink = new Anchor("impressum", "Imprint");
        impressumLink.getStyle().set("font-size", "small");
        Anchor datenLink = new Anchor("datenschutzerklaerung", "Privacy Policy");
        datenLink.getStyle().set("font-size", "small");

        HorizontalLayout footerLayout = new HorizontalLayout(impressumLink, new Text("|"), datenLink);
        footerLayout.setSpacing(true);

        VerticalLayout mainContent = new VerticalLayout(welcomeLayout, /* activeStudiesLayout, */ footerLayout);
        mainContent.setSizeFull();
        mainContent.setHorizontalComponentAlignment(Alignment.CENTER, footerLayout);
        // mainContent.setHorizontalComponentAlignment(Alignment.END, activeStudiesLayout);
        mainContent.setFlexGrow(1, welcomeLayout);
        mainContent.setAlignItems(Alignment.CENTER);
        mainContent.getStyle().set("background", "linear-gradient(to bottom, #f8f9fa, #e9ecef)");

        add(mainContent);
        setFlexGrow(1, mainContent);
    }

    /*
    private VerticalLayout createActiveStudiesLayout() {
        Map<Study, Date> studyValidationDates = new HashMap<>();
        studyRepository.findAll().forEach(study ->
            study.getListOfSamples().stream()
                    .filter(Sample::isValidated)
                    .map(Sample::getValidatedAt)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .ifPresent(maxDate -> studyValidationDates.put(study, maxDate))
        );

        List<Study> activeStudies = studyValidationDates.entrySet().stream()
                .sorted(Map.Entry.<Study, Date>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(2)
                .collect(Collectors.toList());

        if (activeStudies.isEmpty()) {
            return new VerticalLayout(); // Return an empty layout and thus hide the section
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(Alignment.END);
        layout.add(new H3("Recent Validated Studies"));

        for (Study study : activeStudies) {
            Button studyButton = new Button(study.getStudyName());
            studyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            studyButton.addClickListener(e -> {
                clientStateService.getClientState().setSelectedStudy(study);
                UI.getCurrent().navigate("StudyOverview");
            });
            layout.add(studyButton);
        }
        return layout;
    }
    */
}


