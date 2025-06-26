package de.unimarburg.samplemanagement.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridMagicLinkEmailService {

    private final SendGrid sendGridClient;

    @Value("${app.magiclink.sender-email:adhikarb@students.uni-marburg.de}")
    private String senderEmail;

    public SendGridMagicLinkEmailService(@Value("${sendgrid.api.key}") String sendGridApiKey) {
        Client customClient = new CustomSendGridClient();
        this.sendGridClient = new SendGrid(sendGridApiKey, customClient);
        System.out.println("SendGrid API Key: " + sendGridApiKey);
    }

    public void sendMagicLink(String toEmail, String magicLink) throws IOException {
        Email from = new Email(senderEmail);
        System.out.println("Sender Email: " + senderEmail);
        String subject = "Your Secure Login Link from Sample Management System";
        Email to = new Email(toEmail);

        String htmlContent = """
        <html>
        <head>
            <style>
                .container {
                    font-family: Arial, sans-serif;
                    padding: 20px;
                    background-color: #f9f9f9;
                    border-radius: 8px;
                    max-width: 600px;
                    margin: auto;
                    color: #333;
                }
                .button {
                    display: inline-block;
                    padding: 12px 20px;
                    margin-top: 20px;
                    font-size: 16px;
                    color: #fff;
                    background-color: #007BFF;
                    text-decoration: none;
                    border-radius: 5px;
                }
                .footer {
                    margin-top: 30px;
                    font-size: 12px;
                    color: #777;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>Welcome to Sample Management System</h2>
                <p>Hello,</p>
                <p>You recently requested a secure login link to log in.</p>
                <p>Click the button below to securely log into your account:</p>
                <a href="%s" class="button">Log In Now</a>
                <p>If the button doesn't work, copy and paste this link into your browser:</p>
                <p><a href="%s">%s</a></p>
                <p class="footer">
                    If you did not request this email, you can safely ignore it.<br/>
                    &copy; 2025 Sample Management System - University of Marburg
                </p>
            </div>
        </body>
        </html>
        """.formatted(magicLink, magicLink, magicLink);

        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);
        Request request = new Request();

        System.out.println("Sending magic link email to: " + toEmail);

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            System.out.println("Request Started: " + request.getMethod() + " " + request.getEndpoint());
            Response response = sendGridClient.api(request);

            if (response.getStatusCode() >= 400) {
                throw new IOException("Failed to send email. Response code: " + response.getStatusCode());
            }

            System.out.println("Email sent successfully to: " + toEmail);
        } catch (IOException ex) {
            throw ex;
        }
    }

    // Custom Client subclass to set HTTP timeouts
    private static class CustomSendGridClient extends Client {

        public CustomSendGridClient() {
            super();

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(10_000)        // 10 seconds connect timeout
                    .setSocketTimeout(10_000)         // 10 seconds socket timeout
                    .setConnectionRequestTimeout(10_000) // 10 seconds connection request timeout
                    .build();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .build();

            try {
                var httpClientField = Client.class.getDeclaredField("httpClient");
                httpClientField.setAccessible(true);
                httpClientField.set(this, httpClient);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to set custom HttpClient with timeout", e);
            }
        }
    }
}
