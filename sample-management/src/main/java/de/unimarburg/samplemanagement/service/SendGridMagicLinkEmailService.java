package de.unimarburg.samplemanagement.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridMagicLinkEmailService {

    private final SendGrid sendGridClient;

    @Value("${app.magiclink.sender-email:adhikarb@students.uni-marburg.de}")
    private String senderEmail;

    public SendGridMagicLinkEmailService(@Value("${sendgrid.api.key}") String sendGridApiKey) {
        this.sendGridClient = new SendGrid(sendGridApiKey);
    }

    public void sendMagicLink(String toEmail, String magicLink) throws IOException {
        Email from = new Email(senderEmail);
        String subject = "Your Magic Login Link";
        Email to = new Email(toEmail);
        Content content = new Content("text/html",
                "<p>Click the link below to login:</p>" +
                        "<p><a href=\"" + magicLink + "\">" + magicLink + "</a></p>" +
                        "<br><p>If you didn't request this, please ignore this email.</p>");

        Mail mail = new Mail(from, subject, to, content);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGridClient.api(request);

            if (response.getStatusCode() >= 400) {
                throw new IOException("Failed to send email. Response code: " + response.getStatusCode());
            }
        } catch (IOException ex) {
            throw ex;
        }
    }
}
