package de.set.assessmentUi;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import de.set.assessmentUi.Treatment;
import de.set.assessmentUi.Treatments;
import de.set.assessmentUi.Treatments.TreatmentCombination;
import de.set.assessmentUi.Treatments.TreatmentUsageData;

public class TreatmentsTest {

    @Test
    public void testBalanceOfTreatmentCombinations() {
        final Random r = new Random(23);
        final Treatments t = new Treatments(new Random(42));
        for (int j = 0; j < 20; j++) {
            final Set<TreatmentCombination> set = new HashSet<>();
            for (int i = 0; i < 36; i++) {
                final TreatmentCombination tr = t.getNextBestTreatment(r.nextInt(4));
                set.add(tr);
            }
            assertEquals(12, set.size());
        }
    }

    @Test
    public void testBalancingOfReviewSkill() {
        for (int j = 0; j < 20; j++) {
            final Treatments t = new Treatments(new Random(j));
            final Set<TreatmentCombination> set = new HashSet<>();
            for (int i = 0; i < 48; i++) {
                final TreatmentCombination tr = t.getNextBestTreatment(i % 2 == 0 ? 1 : 3);
                set.add(tr);
            }
            assertEquals(12, set.size());
            final Map<Treatment, TreatmentUsageData> countsPerTreatment = new HashMap<>();
            countsPerTreatment.put(Treatment.OPTIMAL_FILES, new TreatmentUsageData());
            countsPerTreatment.put(Treatment.OPTIMAL_NO_FILES, new TreatmentUsageData());
            countsPerTreatment.put(Treatment.WORST_FILES, new TreatmentUsageData());
            countsPerTreatment.put(Treatment.WORST_NO_FILES, new TreatmentUsageData());
            for (final Entry<TreatmentCombination, TreatmentUsageData> e : t.getEntries().entrySet()) {
                countsPerTreatment.get(e.getKey().getT1()).adjust(e.getValue());
                countsPerTreatment.get(e.getKey().getT2()).adjust(e.getValue());
            }
            assertEquals(2.0, countsPerTreatment.get(Treatment.OPTIMAL_FILES).getReviewScoreAvg(), 0.3);
            assertEquals(2.0, countsPerTreatment.get(Treatment.OPTIMAL_NO_FILES).getReviewScoreAvg(), 0.3);
            assertEquals(2.0, countsPerTreatment.get(Treatment.WORST_FILES).getReviewScoreAvg(), 0.3);
            assertEquals(2.0, countsPerTreatment.get(Treatment.WORST_NO_FILES).getReviewScoreAvg(), 0.3);
        }
    }

}
