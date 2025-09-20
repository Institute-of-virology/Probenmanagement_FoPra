package de.unimarburg.samplemanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "analysis_type")
public class AnalysisType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true, nullable = false)
    private String analysisName;

    private String analysisDescription;

    private String analysisUnit;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AnalysisType other)) return false;

        // If either ID is null, compare by object identity
        if (this.id == null || other.id == null) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        // If ID is null, use system identity hash to distinguish new objects
        return (id != null) ? id.hashCode() : System.identityHashCode(this);
    }
}
