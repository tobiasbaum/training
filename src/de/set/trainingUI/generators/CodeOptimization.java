package de.set.trainingUI.generators;

import com.github.javaparser.ast.CompilationUnit;

public interface CodeOptimization {

	/**
	 * Can "optimize" the code by removing artifacts that would make it too obvious to the reviewer that a mutation
	 * introduced. Returns true if something was changed and false otherwise.
	 */
	public abstract boolean optimize(CompilationUnit cu);

	/**
	 * Repeatedly calls the optimization until nothing can be optimized any more.
	 */
	public static void optimizeUntilSteadyState(CompilationUnit cu, CodeOptimization opt) {
		while (opt.optimize(cu)) {
		}
	}

}
