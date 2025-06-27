package de.unimarburg.samplemanagement.security;

import de.unimarburg.samplemanagement.model.User;
import de.unimarburg.samplemanagement.repository.UserRepository;
import de.unimarburg.samplemanagement.service.SendGridMagicLinkEmailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.authentication.ott.RedirectOneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Component
public class MagicLinkOneTimeTokenGenerationSuccessHandler implements OneTimeTokenGenerationSuccessHandler {

    @Value("${app.base-url}")
    private String baseUrl;

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

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ServletException("User not found for username: " + username));

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            throw new ServletException("Email not found for username: " + username);
        }

        String magicLink = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .replacePath(request.getContextPath() + "/login/ott")
                .queryParam("token", oneTimeToken.getTokenValue())
                .toUriString();
        System.out.println("magic link: " + magicLink);
        System.out.println("email: " + email);

        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendMagicLink(email, magicLink);
            } catch (Exception e) {
                e.printStackTrace(); // Log it safely
            }
        });

        // Set content type to HTML
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        // Write a simple HTML confirmation page
        response.getWriter().write(
                "<!DOCTYPE html>" +
                        "<html lang='en'>" +
                        "<head>" +
                        "  <meta charset='UTF-8'>" +
                        "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "  <title>Magic Link Sent</title>" +
                        "  <style>" +
                        "    body { font-family: Arial, sans-serif; background: #f9f9f9; color: #333; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }" +
                        "    .container { background: white; padding: 2em 3em; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); text-align: center; max-width: 400px; }" +
                        "    h1 { color: #4CAF50; }" +
                        "    p { margin-top: 1em; font-size: 1.1em; }" +
                        "  </style>" +
                        "</head>" +
                        "<body>" +
                        "  <div class='container'>" +
                        "    <h1>Check your email!</h1>" +
                        "    <p>A login link has been sent to <strong>" + email + "</strong>.</p>" +
                        "    <p>Please follow the link in the email to log in.</p>" +
                        "    <p>You can now close this window.</p>" +
                        "  </div>" +
                        "</body>" +
                        "</html>"
        );
    }
}

