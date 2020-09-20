package de.set.trainingUI.generators;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

public class MutateFromSource {

	private final File target;
	private final Random random;

	private MutateFromSource(File target, Random random) {
		this.target = target;
		this.random = random;
	}

	public static void main(String[] args) throws Exception {
		final File baseDir = new File("/home/ich/eclipse-workspace/LearningSnippets/src");
		final MutateFromSource m = new MutateFromSource(new File("mutRevTasks"), new Random(1234));
		m.traverseFiles(baseDir);
	}

	private void traverseFiles(File dir) throws Exception {
		final File[] children = dir.listFiles();
		Arrays.sort(children);
		for (final File f : children) {
			if (f.isDirectory()) {
				this.traverseFiles(f);
			} else if (f.getName().endsWith(".java")) {
				System.out.println("Mutating " + f);
				final MutationGenerator g = new MutationGenerator(f, 7);
				g.generateMultiple(this.target, this.random);
			}
		}
	}

}
