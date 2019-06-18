package de.set.assessmentUi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.set.assessmentUi.Experiment;

public class ExperimentTest {

    @Test
    public void testAlphaId() {
        assertEquals("88h", new Experiment(237464089129L).getIdAlpha());
        assertEquals("8fl", new Experiment(237464089130L).getIdAlpha());
        assertEquals("8mp", new Experiment(237464089131L).getIdAlpha());
        assertEquals("8tt", new Experiment(237464089132L).getIdAlpha());
        assertEquals("90x", new Experiment(237464089133L).getIdAlpha());
    }

}
