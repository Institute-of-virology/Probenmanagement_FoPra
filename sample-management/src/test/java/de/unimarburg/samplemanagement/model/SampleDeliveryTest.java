package de.unimarburg.samplemanagement.model;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SampleDeliveryTest {

    @Test
    void testGetRunningNumber() {
        Study study = new Study();
        study.setSampleDeliveryList(new ArrayList<>());

        SampleDelivery delivery1 = new SampleDelivery();
        delivery1.setId(1L);
        delivery1.setStudy(study);
        study.getSampleDeliveryList().add(delivery1);

        SampleDelivery delivery2 = new SampleDelivery();
        delivery2.setId(2L);
        delivery2.setStudy(study);
        study.getSampleDeliveryList().add(delivery2);

        assertEquals(1, delivery1.getRunningNumber());
        assertEquals(2, delivery2.getRunningNumber());
    }
}
