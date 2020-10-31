package de.set.trainingUI;

import java.util.List;

public interface AnnotatedSolution {

	public abstract boolean isCorrect();

	public abstract List<List<String>> formatSolution();

}
