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

	private final int ADDITIONAL_REMARK_THRESHOLD = 5;

	private final List<RemarkPattern> expectedRemarks;
	private final Set<RemarkPattern> unmatchedRemarks;
	private final Map<Remark, Boolean> annotatedInput;
	private int additionalRemarks;

	public ReviewResultRating(List<RemarkPattern> expectedRemarks, List<Remark> actualRemarks) {
		this.expectedRemarks = expectedRemarks;

		this.unmatchedRemarks = new LinkedHashSet<>(expectedRemarks);
		this.annotatedInput = new LinkedHashMap<>();
		for (final Remark actual : actualRemarks) {
			final RemarkPattern match = findBestMatch(this.unmatchedRemarks, actual);
			if (match == null) {
				this.additionalRemarks++;
			}
			this.unmatchedRemarks.remove(match);
			this.annotatedInput.put(actual, match != null);
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
		return this.unmatchedRemarks.isEmpty() && this.additionalRemarks < this.ADDITIONAL_REMARK_THRESHOLD;
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
        return format(this.getAnnotatedSolution());
    }

	@Override
	public List<List<String>> formatInput() {
		return format(this.annotatedInput);
	}

	private static List<List<String>> format(final Map<Remark, Boolean> annotatedSolution) {
		final List<List<String>> ret = new ArrayList<>();
		for (final Entry<Remark, Boolean> e : annotatedSolution.entrySet()) {
            ret.add(Arrays.asList(
                    "Zeile " + e.getKey().getLine(),
                    e.getKey().getType().getText(),
                    e.getKey().getMessage(),
                    e.getValue() ? "OK" : ""));
        }
        return ret;
	}

}
