package de.unimarburg.samplemanagement.utils;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import de.unimarburg.samplemanagement.UI.Main;
import de.unimarburg.samplemanagement.UI.analyses.UserCreationView;
import de.unimarburg.samplemanagement.UI.general_info.EditAddresses;
import de.unimarburg.samplemanagement.UI.sample.SampleView;
import de.unimarburg.samplemanagement.UI.study.StudiesView;
import de.unimarburg.samplemanagement.model.Study;
import de.unimarburg.samplemanagement.model.User;
import de.unimarburg.samplemanagement.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.vaadin.flow.server.VaadinService;

import java.util.List;
import java.util.Optional;

public class SIDEBAR_FACTORY {

    private static UserRepository userRepository;

    // Setter or inject UserRepository for fetching username from email
    public static void setUserRepository(UserRepository repo) {
        userRepository = repo;
    }

    public static VerticalLayout getSidebar(Study study) {
        SideNav genNav = new SideNav();
        genNav.setLabel("General");
        SideNavItem home = new SideNavItem("Home", Main.class, VaadinIcon.HOME.create());
        SideNavItem studies = new SideNavItem("Studies", StudiesView.class, VaadinIcon.BOOK.create());
        SideNavItem samples = new SideNavItem("Samples", SampleView.class, VaadinIcon.BARCODE.create());
        SideNavItem editAddress = new SideNavItem("Base-data", EditAddresses.class, VaadinIcon.MAILBOX.create());
        genNav.addItem(home, studies, samples, editAddress);

        // Add "Add User" for admins only
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            SideNavItem addUser = new SideNavItem("Add User", UserCreationView.class, VaadinIcon.USER.create());
            genNav.addItem(addUser);
        }

        SideNav sideNavToUse = genNav;
        if (study != null) {
            SideNav studyNav = new SideNav();
            studyNav.setLabel("Study: " + study.getStudyName());
            studyNav.addItem(home, studies);
            List<oshi.util.tuples.Pair<String, Class>> studyActions = ACTION_LISTS.getStudySpecificActions();
            for (oshi.util.tuples.Pair<String, Class> entry : studyActions) {
                SideNavItem button = new SideNavItem(entry.getA(), entry.getB(), VaadinIcon.ARROW_RIGHT.create());
                studyNav.addItem(button);
            }
            sideNavToUse = studyNav;
        }

        VerticalLayout sidebarWrapper = new VerticalLayout();
        sidebarWrapper.setSizeFull();
        sidebarWrapper.setPadding(false);
        sidebarWrapper.setSpacing(false);
        sidebarWrapper.getStyle().set("display", "flex");
        sidebarWrapper.getStyle().set("flex-direction", "column");
        sidebarWrapper.getStyle().set("max-width", "20%");
        sidebarWrapper.getStyle().set("width", "100%");
        sidebarWrapper.getStyle().set("position", "sticky");
        sidebarWrapper.getStyle().set("top", "0");
        sidebarWrapper.getStyle().set("height", "100vh"); // full viewport height

        sidebarWrapper.add(sideNavToUse);
        sidebarWrapper.expand(sideNavToUse);

        Div footer = new Div();
        footer.getStyle().set("padding", "10px");
        footer.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        footer.getStyle().set("display", "flex");
        footer.getStyle().set("justify-content", "space-between");
        footer.getStyle().set("align-items", "center");

        // Get username from principal (via email) from DB
        String usernameToShow = "Guest";
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String email = auth.getName();
            if (userRepository != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                usernameToShow = userOpt.map(User::getUsername).orElse(email);
            } else {
                // fallback: show email if repo not injected
                usernameToShow = email;
            }
        }

        Text usernameText = new Text("Logged in as: " + usernameToShow);

        Button logoutButton = new Button("Logout", e -> {
            VaadinService.getCurrentRequest().getWrappedSession().invalidate();
            sidebarWrapper.getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
        });

        footer.add(usernameText, logoutButton);
        sidebarWrapper.add(footer);
        sidebarWrapper.setAlignItems(Alignment.STRETCH);

        return sidebarWrapper;
    }
}
