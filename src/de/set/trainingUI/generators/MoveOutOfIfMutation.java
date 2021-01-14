package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import de.set.trainingUI.RemarkType;

public class MoveOutOfIfMutation extends Mutation {

	private final IfStmt stmt;
	private final IfStmt topmostIf;

	public MoveOutOfIfMutation(IfStmt stmt) {
		this.stmt = stmt;
		this.topmostIf = determineTopmostIf(stmt);
	}

	public static boolean isApplicable(IfStmt s) {
		return (isBlockWithMultiple(s.getThenStmt()) || isBlockWithMultiple(s.getElseStmt()))
			&& isContainedInBlock(s);
	}

	private static boolean isBlockWithMultiple(Optional<Statement> s) {
		return s.isPresent() && isBlockWithMultiple(s.get());
	}

	private static boolean isBlockWithMultiple(Statement s) {
		return (s instanceof BlockStmt)
			&& ((BlockStmt) s).getStatements().size() > 1;
	}

	private static boolean isContainedInBlock(IfStmt s) {
		return determineTopmostIf(s) != null;
	}

	@Override
	public boolean isStillValid() {
		return isInCU(this.topmostIf) && isInSameCU(this.stmt, this.topmostIf);
	}

	private static IfStmt determineTopmostIf(IfStmt s) {
		final Node parent = s.getParentNode().orElse(null);
		if (parent instanceof BlockStmt) {
			return s;
		} else if (parent instanceof IfStmt) {
			return determineTopmostIf((IfStmt) parent);
		} else {
			return null;
		}
	}

	@Override
	public void apply(Random r) {
		if (isBlockWithMultiple(this.stmt.getThenStmt())) {
			if (isBlockWithMultiple(this.stmt.getElseStmt())) {
				if (r.nextBoolean()) {
					this.applyOn(r, (BlockStmt) this.stmt.getThenStmt());
				} else {
					this.applyOn(r, (BlockStmt) this.stmt.getElseStmt().get());
				}
			} else {
				this.applyOn(r, (BlockStmt) this.stmt.getThenStmt());
			}
		} else {
			this.applyOn(r, (BlockStmt) this.stmt.getElseStmt().get());
		}
	}

	private void applyOn(Random r, BlockStmt blockInIf) {
		final int countToMove = r.nextInt(blockInIf.getStatements().size() - 1) + 1;
		if (r.nextBoolean()) {
			//move some of the last statements behind the if
			final List<Statement> toMove = new ArrayList<>();
			for (int i = 0; i < countToMove; i++) {
				toMove.add(blockInIf.getStatements().removeLast());
			}
			final BlockStmt target = (BlockStmt) this.topmostIf.getParentNode().get();
			for (final Statement s : toMove) {
				target.getStatements().addAfter(s, this.topmostIf);
			}
		} else {
			//move some of the first statements in front of the if
			final List<Statement> toMove = new ArrayList<>();
			for (int i = 0; i < countToMove; i++) {
				toMove.add(blockInIf.getStatements().removeFirst());
			}
			final BlockStmt target = (BlockStmt) this.topmostIf.getParentNode().get();
			for (final Statement s : toMove) {
				target.getStatements().addBefore(s, this.topmostIf);
			}
		}
	}

	@Override
	public void createRemark(int nbr, Properties p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        addBeginToEnd(lines, this.topmostIf.getParentNode().get());
        this.setRemark(nbr, p, lines, RemarkType.OTHER_ALGORITHMIC_PROBLEM, ".+", "muss in if(" + this.stmt.getCondition() + ")");
	}

	@Override
	public int getAnchorLine() {
		return this.topmostIf.getBegin().get().line - 1;
	}

}
