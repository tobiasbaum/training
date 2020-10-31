package de.set.trainingUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

public class ReviewResultRatingTest {

	@Test
	public void testIsCorrectSimpleMatch() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(new RemarkPattern("1;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect")),
				Arrays.asList(new Remark(1, RemarkType.WRONG_CALCULATION, "ein Fehler")));
		assertTrue(rrr.isCorrect());
	}

	@Test
	public void testIsCorrectLineMismatch() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(new RemarkPattern("1;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect")),
				Arrays.asList(new Remark(2, RemarkType.WRONG_CALCULATION, "ein Fehler")));
		assertFalse(rrr.isCorrect());
	}

	@Test
	public void testIsCorrectTypeMismatch() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(new RemarkPattern("1;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect")),
				Arrays.asList(new Remark(1, RemarkType.MISSING_CODE, "ein Fehler")));
		assertFalse(rrr.isCorrect());
	}

	@Test
	public void testIsCorrectTextMismatch() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(new RemarkPattern("1;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect")),
				Arrays.asList(new Remark(1, RemarkType.WRONG_CALCULATION, "")));
		assertFalse(rrr.isCorrect());
	}

	@Test
	public void testIsCorrectAdditionalRemarksAreOK() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(new RemarkPattern("1;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect")),
				Arrays.asList(
						new Remark(1, RemarkType.WRONG_CALCULATION, "ein Fehler"),
						new Remark(2, RemarkType.MISSING_CODE, "noch ein Fehler")));
		assertTrue(rrr.isCorrect());
	}

	@Test
	public void testIsCorrectMissingRemarksAreNotOK() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(
						new RemarkPattern("1;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect"),
						new RemarkPattern("2;MISSING_CODE;.+", "2;MISSING_CODE;a defect")),
				Arrays.asList(
						new Remark(1, RemarkType.WRONG_CALCULATION, "ein Fehler")));
		assertFalse(rrr.isCorrect());
	}

	@Test
	public void testIsCorrectTwoMatches() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(
						new RemarkPattern("1;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect"),
						new RemarkPattern("2;MISSING_CODE;.+", "2;MISSING_CODE;a defect")),
				Arrays.asList(
						new Remark(1, RemarkType.WRONG_CALCULATION, "ein Fehler"),
						new Remark(2, RemarkType.MISSING_CODE, "noch ein Fehler")));
		assertTrue(rrr.isCorrect());
	}

	@Test
	public void testGetAnnotatedSolution() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(
						new RemarkPattern("1;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect"),
						new RemarkPattern("2;MISSING_CODE;.+", "2;MISSING_CODE;another defect")),
				Arrays.asList(
						new Remark(1, RemarkType.WRONG_CALCULATION, "ein Fehler")));
		final Map<Remark, Boolean> actual = rrr.getAnnotatedSolution();
		checkSolution(actual, "a defect", Boolean.TRUE);
		checkSolution(actual, "another defect", Boolean.FALSE);
		assertEquals(2, actual.size());
	}

	@Test
	public void testGetAnnotatedSolutionBestOneIsChosen() {
		final ReviewResultRating rrr = new ReviewResultRating(
				Arrays.asList(
						new RemarkPattern("1,2,3,4;WRONG_CALCULATION;.+", "1;WRONG_CALCULATION;a defect"),
						new RemarkPattern("2;WRONG_CALCULATION;.+", "2;WRONG_CALCULATION;another defect"),
						new RemarkPattern("2,3,4;WRONG_CALCULATION;.+", "3;WRONG_CALCULATION;defect 3")),
				Arrays.asList(
						new Remark(2, RemarkType.WRONG_CALCULATION, "ein Fehler")));
		final Map<Remark, Boolean> actual = rrr.getAnnotatedSolution();
		checkSolution(actual, "a defect", Boolean.FALSE);
		checkSolution(actual, "another defect", Boolean.TRUE);
		checkSolution(actual, "defect 3", Boolean.FALSE);
		assertEquals(3, actual.size());
	}

	private static void checkSolution(Map<Remark, Boolean> actual, String message, Boolean expectedValue) {
		assertEquals(expectedValue, getValueForRemark(actual, message));
	}

	private static Boolean getValueForRemark(Map<Remark, Boolean> actual, String message) {
		for (final Entry<Remark, Boolean> e : actual.entrySet()) {
			if (e.getKey().getMessage().equals(message)) {
				return e.getValue();
			}
		}
		throw new AssertionError("found no remark for " + message);
	}

}
