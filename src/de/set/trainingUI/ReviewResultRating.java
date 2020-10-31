package de.set.trainingUI;

import java.util.List;

public class ReviewResultRating {

	private final List<RemarkPattern> expectedRemarks;
	private final List<Remark> actualRemarks;

	public ReviewResultRating(List<RemarkPattern> expectedRemarks, List<Remark> actualRemarks) {
		this.expectedRemarks = expectedRemarks;
		this.actualRemarks = actualRemarks;
	}

	public boolean isCorrect() {
        for (final RemarkPattern expected : this.expectedRemarks) {
            if (!this.matchesAny(expected, this.actualRemarks)) {
                return false;
            }
        }
        return true;
	}

    private boolean matchesAny(final RemarkPattern expected, final List<Remark> remarks) {
        for (final Remark remark : remarks) {
            if (expected.matches(remark)) {
                return true;
            }
        }
        return false;
    }

}
