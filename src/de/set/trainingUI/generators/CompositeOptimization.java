package de.set.trainingUI.generators;

import java.util.Arrays;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;

public class CompositeOptimization implements CodeOptimization {

	private final List<CodeOptimization> children;

	public CompositeOptimization(CodeOptimization... children) {
		this.children = Arrays.asList(children);
	}

	@Override
	public boolean optimize(CompilationUnit cu) {
		boolean changed = false;
		for (final CodeOptimization child : this.children) {
			changed |= child.optimize(cu);
		}
		return changed;
	}

	public static CodeOptimization empty() {
		return new CompositeOptimization();
	}

}
