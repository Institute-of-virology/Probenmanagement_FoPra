package de.unimarburg.samplemanagement.utils;

import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.Rendering;
import com.vaadin.flow.dom.Element;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import de.unimarburg.samplemanagement.model.Analysis;
import de.unimarburg.samplemanagement.model.Sample;
import de.unimarburg.samplemanagement.repository.AddressStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Component
public class GENERAL_UTIL {
    @Autowired
    private static AddressStoreRepository addressStoreRepository;

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
        // Convert LocalDate to an Instant
        Instant instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Create a Date from the Instant
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


    public static String readFileToString(String resourcename) {
        //get resource
        URL mdUrl = GENERAL_UTIL.class.getClassLoader().getResource(resourcename);
        if (mdUrl == null) {
            return "couldn't load "+resourcename;
        }
        try {
            return new String(mdUrl.openStream().readAllBytes());
        } catch (Exception e) {
            return "couldn't load "+resourcename;
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
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .contains(new SimpleGrantedAuthority(role));
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


}
