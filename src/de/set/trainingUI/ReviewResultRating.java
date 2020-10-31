package de.set.trainingUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ReviewResultRating implements AnnotatedSolution {

	private final List<RemarkPattern> expectedRemarks;
	private final Set<RemarkPattern> unmatchedRemarks;

	public ReviewResultRating(List<RemarkPattern> expectedRemarks, List<Remark> actualRemarks) {
		this.expectedRemarks = expectedRemarks;

		this.unmatchedRemarks = new LinkedHashSet<>(expectedRemarks);
		for (final Remark actual : actualRemarks) {
			this.unmatchedRemarks.remove(findBestMatch(this.unmatchedRemarks, actual));
		}
	}

	private static RemarkPattern findBestMatch(
			Set<RemarkPattern> patterns, Remark actual) {
		RemarkPattern bestSoFar = null;
		for (final RemarkPattern pattern : patterns) {
			if (pattern.matches(actual)) {
				if (bestSoFar == null) {
					bestSoFar = pattern;
				} else {
					final int bestDist = bestSoFar.distanceToExample(actual);
					final int curDist = pattern.distanceToExample(actual);
					if (curDist < bestDist) {
						bestSoFar = pattern;
					}
				}
			}
		}
		return bestSoFar;
	}

	@Override
	public boolean isCorrect() {
		return this.unmatchedRemarks.isEmpty();
	}

	public Map<Remark, Boolean> getAnnotatedSolution() {
		final Map<Remark, Boolean> ret = new LinkedHashMap<>();
		for (final RemarkPattern p : this.expectedRemarks) {
			ret.put(p.getExample(), !this.unmatchedRemarks.contains(p));
		}
		return ret;
	}

    @Override
    public List<List<String>> formatSolution() {
        final List<List<String>> ret = new ArrayList<>();
        for (final Entry<Remark, Boolean> e : this.getAnnotatedSolution().entrySet()) {
            ret.add(Arrays.asList(
                    "Zeile " + e.getKey().getLine(),
                    e.getKey().getType().getText(),
                    e.getKey().getMessage(),
                    e.getValue() ? "OK" : ""));
        }
        return ret;
    }

}
