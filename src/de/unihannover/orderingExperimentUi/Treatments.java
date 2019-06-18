package de.unihannover.orderingExperimentUi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Treatments {

    public static final class TreatmentCombination {
        private final Treatment t1;
        private final Treatment t2;
        private final Diff d1;
        private final Diff d2;

        public TreatmentCombination(Treatment t1, Treatment t2, Diff d1, Diff d2) {
            this.t1 = t1;
            this.t2 = t2;
            this.d1 = d1;
            this.d2 = d2;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TreatmentCombination)) {
                return false;
            }
            final TreatmentCombination tc = (TreatmentCombination) o;
            return this.t1.equals(tc.t1)
                && this.t2.equals(tc.t2)
                && this.d1.equals(tc.d1)
                && this.d2.equals(tc.d2);
        }

        @Override
        public int hashCode() {
            return this.t1.hashCode() + this.d1.hashCode();
        }

        @Override
        public String toString() {
            return this.t1 + ", " + this.t2 + ", " + this.d1 + ", " + this.d2;
        }

        public Diff getD1() {
            return this.d1;
        }

        public Diff getD2() {
            return this.d2;
        }

        public Treatment getT1() {
            return this.t1;
        }

        public Treatment getT2() {
            return this.t2;
        }
    }

    public static final class TreatmentUsageData {
        private int count;
        private int reviewScoreSum;

        public TreatmentUsageData() {
            this(0, 0);
        }

        public TreatmentUsageData(int count, int reviewScoreSum) {
            this.count = count;
            this.reviewScoreSum = reviewScoreSum;
        }

        public int getReviewScoreSum() {
            return this.reviewScoreSum;
        }

        public void remove(int testReviewScore) {
            this.count--;
            this.reviewScoreSum -= testReviewScore;
        }

        public void adjust(int testReviewScore) {
            this.count++;
            this.reviewScoreSum += testReviewScore;
        }

        public void adjust(TreatmentUsageData value) {
            this.count += value.count;
            this.reviewScoreSum += value.reviewScoreSum;
        }

        public double getImprovementInBalance(double newAvg, int newValue) {
            if (this.count == 0) {
                return newAvg;
            }

            final double avgWithout = ((double) this.reviewScoreSum) / this.count;
            final double avgWith = ((double) this.reviewScoreSum + newValue) / (this.count + 1);
            final double distWithout = Math.abs(newAvg - avgWithout);
            final double distWith = Math.abs(newAvg - avgWith);
            return distWithout - distWith;
        }

        public double getReviewScoreAvg() {
            return ((double) this.reviewScoreSum) / this.count;
        }

        public TreatmentUsageData copy() {
            final TreatmentUsageData ret = new TreatmentUsageData();
            ret.adjust(this);
            return ret;
        }

        @Override
        public String toString() {
            return "count=" + this.count + ", reviewScoreSum=" + this.reviewScoreSum;
        }
    }

    private static final double EPSILON = 0.0000001;

    private final Map<TreatmentCombination, TreatmentUsageData> map = new LinkedHashMap<>();
    private final Random random;

    public Treatments(Random r) {
        this.random = r;
        //fill the map with the combinations we are interested in (i.e. where the theory makes a prediction)
        this.addPermutationsToMap(Treatment.OPTIMAL_FILES, Treatment.WORST_FILES);
        this.addPermutationsToMap(Treatment.OPTIMAL_NO_FILES, Treatment.WORST_NO_FILES);
        this.addPermutationsToMap(Treatment.WORST_NO_FILES, Treatment.WORST_FILES);

//        //balanced
//        for (final Treatment t1 : Treatment.values()) {
//            for (final Treatment t2 : Treatment.values()) {
//                if (t1 == t2) {
//                    continue;
//                }
//                for (final Diff d1 : Diff.values()) {
//                    for (final Diff d2 : Diff.values()) {
//                        if (d1 == d2) {
//                            continue;
//                        }
//                        this.map.put(new TreatmentCombination(t1, t2, d1, d2), new TreatmentUsageData());
//                    }
//                }
//            }
//        }
    }

    private void addPermutationsToMap(Treatment t1, Treatment t2) {
        this.map.put(new TreatmentCombination(t1, t2, Diff.DIFF1, Diff.DIFF2), new TreatmentUsageData());
        this.map.put(new TreatmentCombination(t1, t2, Diff.DIFF2, Diff.DIFF1), new TreatmentUsageData());
        this.map.put(new TreatmentCombination(t2, t1, Diff.DIFF1, Diff.DIFF2), new TreatmentUsageData());
        this.map.put(new TreatmentCombination(t2, t1, Diff.DIFF2, Diff.DIFF1), new TreatmentUsageData());
    }

    public synchronized TreatmentCombination getNextBestTreatment(
                    int testReviewScore) {
        final List<TreatmentCombination> minCombinations = this.determineCombinationsWithMinCount();
        final List<TreatmentCombination> minTreatments = this.determineTreatmentsWithMinCount(minCombinations);
        final List<TreatmentCombination> afterReviewScore = this.determineCombinationsThatIncreaseBalance(
                        minTreatments, testReviewScore);

        List<TreatmentCombination> toSampleFrom;
        if (afterReviewScore.isEmpty()) {
            toSampleFrom = minTreatments;
        } else {
            toSampleFrom = afterReviewScore;
        }

        final int index = this.random.nextInt(toSampleFrom.size());
        final TreatmentCombination ret = toSampleFrom.get(index);
        this.map.get(ret).adjust(testReviewScore);
        return ret;
    }

    private List<TreatmentCombination> determineCombinationsThatIncreaseBalance(
                    List<TreatmentCombination> toCheck,
                    int newValue) {

        final Map<Treatment, TreatmentUsageData> usePerTreatment = new EnumMap<>(Treatment.class);
        for (final Treatment t : Treatment.values()) {
            usePerTreatment.put(t, new TreatmentUsageData());
        }
        final TreatmentUsageData total = new TreatmentUsageData();

        for (final Entry<TreatmentCombination, TreatmentUsageData> e : this.map.entrySet()) {
            total.adjust(e.getValue());
            usePerTreatment.get(e.getKey().t1).adjust(e.getValue());
            usePerTreatment.get(e.getKey().t2).adjust(e.getValue());
        }
        total.adjust(newValue);
        final double newAvg = total.getReviewScoreAvg();

        final double maxImprovement = this.determineMaxImprovementInBalance(toCheck, usePerTreatment, newAvg, newValue);
        final List<TreatmentCombination> ret = new ArrayList<>();
        for (final TreatmentCombination key : toCheck) {
            if (this.getImprovementInBalance(key, usePerTreatment, newAvg, newValue) >= maxImprovement - EPSILON) {
                ret.add(key);
            }
        }
        return ret;
    }

    private double determineMaxImprovementInBalance(List<TreatmentCombination> toCheck,
                    Map<Treatment, TreatmentUsageData> usePerTreatment, double newAvg, int newValue) {

        double max = 0.0;
        for (final TreatmentCombination c : toCheck) {
            max = Math.max(max, this.getImprovementInBalance(c, usePerTreatment, newAvg, newValue));
        }
        return max;
    }

    private double getImprovementInBalance(TreatmentCombination key, Map<Treatment, TreatmentUsageData> usePerTreatment,
                    double newAvg, int newValue) {

        final double improvement1 = usePerTreatment.get(key.t1).getImprovementInBalance(newAvg, newValue);
        final double improvement2 = usePerTreatment.get(key.t2).getImprovementInBalance(newAvg, newValue);
        return improvement1 + improvement2;
    }

    private List<TreatmentCombination> determineTreatmentsWithMinCount(
                    Collection<TreatmentCombination> toCheck) {
        final Set<Treatment> minCount = this.determineMinCountTreatments(this.getTreatmentsFrom(toCheck));

        final List<TreatmentCombination> minCombinations = new ArrayList<>();
        for (final TreatmentCombination key : toCheck) {
            if (minCount.contains(key.t1) || minCount.contains(key.t2)) {
                minCombinations.add(key);
            }
        }
        return minCombinations;
    }

    private Set<Treatment> getTreatmentsFrom(Collection<TreatmentCombination> toCheck) {
        final Set<Treatment> ret = EnumSet.noneOf(Treatment.class);
        for (final TreatmentCombination c : toCheck) {
            ret.add(c.t1);
            ret.add(c.t2);
        }
        return ret;
    }

    private Set<Treatment> determineMinCountTreatments(Set<Treatment> relevantTreatments) {
        final Map<Treatment, AtomicInteger> counts = new EnumMap<>(Treatment.class);
        for (final Treatment t : Treatment.values()) {
            counts.put(t, new AtomicInteger(0));
        }

        for (final Entry<TreatmentCombination, TreatmentUsageData> e : this.map.entrySet()) {
            counts.get(e.getKey().t1).addAndGet(e.getValue().count);
            counts.get(e.getKey().t2).addAndGet(e.getValue().count);
        }

        int minCount = Integer.MAX_VALUE;
        for (final Entry<Treatment, AtomicInteger> e : counts.entrySet()) {
            if (relevantTreatments.contains(e.getKey())) {
                minCount = Math.min(minCount, e.getValue().get());
            }
        }

        final Set<Treatment> ret = EnumSet.noneOf(Treatment.class);
        for (final Entry<Treatment, AtomicInteger> e : counts.entrySet()) {
            if (e.getValue().get() == minCount && relevantTreatments.contains(e.getKey())) {
                ret.add(e.getKey());
            }
        }
        return ret;
    }

    private List<TreatmentCombination> determineCombinationsWithMinCount() {
        final int minCount = this.determineMinCountOfTreatmentCombination(this.map.keySet());

        final List<TreatmentCombination> minCombinations = new ArrayList<>();
        for (final TreatmentCombination key : this.map.keySet()) {
            if (this.map.get(key).count == minCount) {
                minCombinations.add(key);
            }
        }
        return minCombinations;
    }

    private int determineMinCountOfTreatmentCombination(Collection<TreatmentCombination> toCheck) {
        int minCount = Integer.MAX_VALUE;
        for (final TreatmentCombination key : toCheck) {
            minCount = Math.min(minCount, this.map.get(key).count);
        }
        return minCount;
    }

    public synchronized Map<TreatmentCombination, TreatmentUsageData> getEntries() {
        final Map<TreatmentCombination, TreatmentUsageData> ret = new LinkedHashMap<>();
        for (final Entry<TreatmentCombination, TreatmentUsageData> e : this.map.entrySet()) {
            ret.put(e.getKey(), e.getValue().copy());
        }
        return ret;
    }

    public synchronized void remove(TreatmentCombination treatmentCombination, int testReviewScore) {
        this.map.get(treatmentCombination).remove(testReviewScore);
    }

    public synchronized void add(TreatmentCombination treatmentCombination, int count, int reviewScoreSum) {
        this.map.get(treatmentCombination).adjust(new TreatmentUsageData(count, reviewScoreSum));
    }

}
