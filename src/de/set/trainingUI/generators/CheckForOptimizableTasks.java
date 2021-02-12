package de.set.trainingUI.generators;

import java.io.File;
import java.io.FileNotFoundException;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class CheckForOptimizableTasks {

	public static void main(String[] args) throws FileNotFoundException {
		final File baseDir = new File("/home/ich/eclipse-workspace/training-tasks/mutRev");
		for (final File child : baseDir.listFiles()) {
			if (new File(child, "ignore").exists()) {
				continue;
			}
			final File source = new File(child, "source");
			final CompilationUnit cu = StaticJavaParser.parse(source);
			final CodeOptimization opt = new CompositeOptimization(
					new RemoveUnnecessaryParenthesesOptimization(),
					new RemoveUnusedVariableOptimization());
			final boolean changed = opt.optimize(cu);
			if (changed) {
				System.out.println(source + " can be optimized");
			}
		}
	}

}
