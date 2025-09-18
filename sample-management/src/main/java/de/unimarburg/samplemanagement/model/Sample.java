package de.unimarburg.samplemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "sample",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"sample_barcode", "study_id"}))

public class Sample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    @NotNull
    private Subject subject;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "study_id", referencedColumnName = "id")
    @NotNull
    private Study study; // Change here to relate to Study

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sample_delivery_id", referencedColumnName = "id")
    @NotNull
    private SampleDelivery sampleDelivery;

    private String coordinates;
    private int visits;
    // Renamed from sampleDate → dateOfShipment
    @Column(name = "date_of_shipment")
    private Date dateOfShipment;

    private String sample_amount;
    private String sample_barcode;
    private String sample_type;
    // Boolean flag
    @Column(name = "validated")
    private boolean validated;

    // New column for the timestamp of validation
    @Column(name = "validated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date validatedAt;

    @OneToMany(mappedBy = "sample", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Analysis> listOfAnalysis;

    @Transient
    public String getStudyName() {
        return study != null ? study.getName() : null;
    }

    @Transient
    public Long getSubjectAlias() {
        return subject != null ? subject.getAlias() : null;
    }

    /**
     * Set the subject and study of the sample
     * @param subject the subject to set (and its study)
     */
    public void setSubject(@NotNull Subject subject) {
        this.subject = subject;
        this.study = subject.getStudy();
    }

    public int getNumberFinishedAnalyses() {
        return (int) listOfAnalysis.stream()
                .filter(Analysis::isFinished)
                .count();
    }
    public int getNumberAnalyses() {
        return listOfAnalysis.size();
    }
}

