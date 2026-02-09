package de.unimarburg.samplemanagement.utils;

import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import de.unimarburg.samplemanagement.model.Analysis;
import de.unimarburg.samplemanagement.model.Sample;
import de.unimarburg.samplemanagement.repository.AddressStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;

@Component
public class GENERAL_UTIL {
    @Autowired
    private AddressStoreRepository addressStoreRepository; // Keep autowired, but not static

    @Value("${app.legal.files.path:./legal_docs}")
    private String legalFilesBasePath;

    // Constructor for Spring to inject dependencies if needed.
    public GENERAL_UTIL() {
    }

    // Existing static methods remain static
    public static String getAnalysisForSample(Sample sample, Long analysisTypeID) {
        try {
            return sample.getListOfAnalysis().stream()
                    .filter(analysis -> analysisTypeID.equals(analysis.getAnalysisType().getId()))
                    .map(Analysis::getAnalysisResult)
                    .findFirst()
                    .orElse("");
        } catch (Exception e) {
            return "N/A";
        }
    }

    public static Date convertToDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        Instant instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    public static LocalDate convertToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Renderer<Sample> renderDate() {
        return new LocalDateRenderer<>(sample -> convertToLocalDate(sample.getDateOfShipment()), "dd.MM.yyyy");
    }

    public static Renderer<Sample> renderDateYYYYMMDD() {
        return new LocalDateRenderer<>(sample -> convertToLocalDate(sample.getDateOfShipment()), "yyyy/MM/dd");
    }

    // New non-static method for reading legal files
    public String readLegalFileToString(String resourceName) {
        Path externalPath = Paths.get(legalFilesBasePath, resourceName);
        if (Files.exists(externalPath)) {
            try {
                return Files.readString(externalPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("Error reading from external path " + externalPath + ": " + e.getMessage());
                // Fallback to classpath if external read fails
            }
        }

        // Fallback to classpath (using a helper static method for classpath read)
        return readFileFromClasspath(resourceName);
    }

    // New non-static method for writing legal files
    public void writeLegalFileToString(String resourceName, String content) throws IOException {
        Path externalPath = Paths.get(legalFilesBasePath, resourceName);
        Files.createDirectories(externalPath.getParent()); // Ensure parent directories exist
        Files.writeString(externalPath, content, StandardCharsets.UTF_8);
    }

    // Helper static method for reading from classpath (extracted from original readFileToString)
    private static String readFileFromClasspath(String resourceName) {
        try (InputStream is = GENERAL_UTIL.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                return "Couldn't load " + resourceName + " from classpath.";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Couldn't load " + resourceName + " from classpath: " + e.getMessage();
        }
    }

    public static String markdownToHtml(String markdown) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int idx = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }

    public static boolean hasRole(String role) {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities();
        String fullRoleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        System.out.println("Checking for role: " + fullRoleName + ". Found authorities: " + authorities);
        return authorities.contains(new SimpleGrantedAuthority(fullRoleName));
    }

    public static String toOrdinal(int number) {
        if (number <= 0) {
            return String.valueOf(number);
        }
        String[] words = { "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth" };
        if (number <= words.length) {
            return words[number - 1];
        }
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (number % 100) {
            case 11:
            case 12:
            case 13:
                return number + "th";
            default:
                return number + suffixes[number % 10];
        }
    }

    public static String formatSampleAmount(String amount) {
        if (amount != null && !amount.isEmpty()) {
            try {
                return String.valueOf((int) Float.parseFloat(amount));
            } catch (NumberFormatException e) {
                return amount;
            }
        }
        return amount;
    }
}

