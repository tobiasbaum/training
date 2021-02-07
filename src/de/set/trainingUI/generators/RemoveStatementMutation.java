package de.set.trainingUI.generators;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import de.set.trainingUI.RemarkType;

final class RemoveStatementMutation extends Mutation {

    private final Statement n;
    private final Node parent;

    public RemoveStatementMutation(final Statement n) {
        this.n = n;
        this.parent = this.n.getParentNode().get();
    }

    @Override
    public void apply(final Random r) {
        this.n.remove();
    }

    @Override
    public int getAnchorLine() {
        return this.n.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        addBeginToEnd(lines, this.parent);
        this.setRemark(nbr, p, lines, RemarkType.MISSING_CODE, ".+", this.n.toString() + " fehlt");
    }

    public static boolean isApplicable(final ExpressionStmt stmt) {
        if (!hasSiblings(stmt)) {
            return false;
        }
        if (!stmt.getExpression().isMethodCallExpr()) {
            return false;
        }
        return true;
    }

    public static boolean isApplicable(final IfStmt stmt) {
        return hasSiblings(stmt);
    }

    public static boolean isApplicable(final ContinueStmt stmt) {
        return hasSiblings(stmt);
    }

    public static boolean isApplicable(final BreakStmt stmt) {
        return hasSiblings(stmt);
    }

    public static boolean isApplicable(final ReturnStmt stmt) {
        if (!hasSiblings(stmt)) {
        	return false;
        }
        if (stmt.getExpression().isEmpty()) {
        	return true;
        } else {
        	final BlockStmt parent = (BlockStmt) stmt.getParentNode().get();
    		return parent.findAncestor(BlockStmt.class,
    				(BlockStmt b) -> getLast(b.getChildNodes()) instanceof ReturnStmt)
    			.isPresent();
        }
    }

	private static<T> T getLast(List<T> list) {
		return list.isEmpty() ? null : list.get(list.size() - 1);
	}

	private static boolean hasSiblings(final Statement stmt) {
		return stmt.getParentNode().get().getChildNodes().size() > 1
			&& stmt.getParentNode().get() instanceof BlockStmt;
	}

}