package de.unimarburg.samplemanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "study", uniqueConstraints = {
        @UniqueConstraint(columnNames = "study_name")
})
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "study_name", nullable = false, unique = true)
    private String studyName;

    private String expectedNumberOfSubjects;
    private String expectedNumberOfSampleDeliveries;
    private String sender1;
    private String sender2;
    private String sender3;
    private String sponsor;
    private String remark;

    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Temporal(TemporalType.DATE)
    private Date endDate;

    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<Subject> listOfSubjects = new ArrayList<>();

    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<SampleDelivery> sampleDeliveryList = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "study_analysis_types",
            joinColumns = @JoinColumn(name = "study_id"),
            inverseJoinColumns = @JoinColumn(name = "analysis_types_id")
    )
    private Set<AnalysisType> analysisTypes = new HashSet<>();

    @Transient
    public int getNumberOfSubjects() {
        return listOfSubjects.size();
    }

    @Transient
    public List<Sample> getListOfSamples() {
        return listOfSubjects.stream()
                .map(Subject::getListOfSamples)
                .flatMap(List::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transient
    public String getName() {
        return studyName;
    }
}
