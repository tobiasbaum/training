package de.set.trainingUI.generators;

import java.util.BitSet;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

/**
 * Maps lines of code from an old version of the code to a new one.
 * The new code may only contain less lines of code, additions (and moves) are not supported.
 */
public class LineMap {

	public static class LineMapBuilder {
		private final CompilationUnit cu;
		private final BitSet oldLines;

		private LineMapBuilder(CompilationUnit cu) {
			this.cu = cu;
			this.oldLines = this.determineUsedLines(cu);
		}

		private BitSet determineUsedLines(CompilationUnit cu) {
			final BitSet ret = new BitSet();
			this.addUsedLines(ret, cu);
			return ret;
		}

		private void addUsedLines(BitSet ret, Node node) {
			if (node.getBegin().isPresent()) {
				ret.set(node.getBegin().get().line);
			}
			if (node.getEnd().isPresent()) {
				ret.set(node.getEnd().get().line);
			}
			for (final Node child : node.getChildNodes()) {
				this.addUsedLines(ret, child);
			}
		}

		/**
		 * Finished creating the line map by diffing the old state to the current state
		 * of the {@link CompilationUnit}.
		 */
		public LineMap snapshotNewCode() {
			final BitSet newLines = this.determineUsedLines(this.cu);
			int sumOfDeletionsSoFar = 0;
			final TreeMap<Integer, Integer> deletionsFrom = new TreeMap<>();
			for (int i = 1; i < this.oldLines.length(); i++) {
				if (this.oldLines.get(i) && !newLines.get(i)) {
					// line i was deleted
					sumOfDeletionsSoFar++;
					deletionsFrom.put(i, sumOfDeletionsSoFar);
				}
			}
			return new LineMap(deletionsFrom);
		}
	}

	private final TreeMap<Integer, Integer> deletionsFrom;

	private LineMap(TreeMap<Integer, Integer> deletionsFrom) {
		this.deletionsFrom = deletionsFrom;
	}

	/**
	 * Starts building a line map from the current state of the given {@link CompilationUnit}.
	 */
	public static LineMapBuilder buildFrom(CompilationUnit code) {
		return new LineMapBuilder(code);
	}

	public static LineMap identity() {
		return new LineMap(new TreeMap<>());
	}

	/**
	 * Maps the given line. If it was removed in the new code, null is returned.
	 * If it moved upwards due to other deletions, the adjusted line number is returned.
	 * Lines that did not move are returned unchanged.
	 */
	public Integer mapLine(int oldLine) {
		final Entry<Integer, Integer> deletions = this.deletionsFrom.floorEntry(oldLine);
		if (deletions == null) {
			return oldLine;
		} else {
			return oldLine - deletions.getValue();
		}
	}

}
