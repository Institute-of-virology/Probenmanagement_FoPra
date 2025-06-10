package de.unimarburg.samplemanagement.security;

import de.unimarburg.samplemanagement.model.User;
import de.unimarburg.samplemanagement.repository.UserRepository;
import de.unimarburg.samplemanagement.service.SendGridMagicLinkEmailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.authentication.ott.RedirectOneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class MagicLinkOneTimeTokenGenerationSuccessHandler implements OneTimeTokenGenerationSuccessHandler {

    private final OneTimeTokenGenerationSuccessHandler redirectHandler = new RedirectOneTimeTokenGenerationSuccessHandler("/");
    private final UserRepository userRepository;   // Inject this
    private final SendGridMagicLinkEmailService emailService;       // Your email sender service

    public MagicLinkOneTimeTokenGenerationSuccessHandler(UserRepository userRepository, SendGridMagicLinkEmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (username == null || username.isEmpty()) {
            throw new ServletException("Username parameter is missing");
        }

        // Lookup user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ServletException("User not found for username: " + username));

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            throw new ServletException("Email not found for username: " + username);
        }

        String magicLink = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString())
                .replacePath(request.getContextPath() + "/login/ott")
                .queryParam("token", oneTimeToken.getTokenValue())
                .toUriString();

        try {
            emailService.sendMagicLink(email, magicLink);
        } catch (Exception e) {
            throw new ServletException("Failed to send magic link email", e);
        }

        response.getWriter().write("Magic link sent to " + email);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}

