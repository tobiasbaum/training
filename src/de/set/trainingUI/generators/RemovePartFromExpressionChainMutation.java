package de.set.trainingUI.generators;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;

import de.set.trainingUI.DefectFindTask.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

public class RemovePartFromExpressionChainMutation extends Mutation {

    private final MethodCallExpr n;

    public RemovePartFromExpressionChainMutation(final MethodCallExpr n) {
        this.n = n;
    }

    @Override
    public void apply(final Random r) {
        this.n.replace(this.n.getScope().get());
    }

    @Override
    public int getAnchorLine() {
        return this.n.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        lines.add(this.n.getBegin().get().line);
        this.setRemark(nbr, p, lines, RemarkType.MISSING_CODE, ".+", this.n.toString() + " fehlt");
    }

    public static boolean isApplicable(final MethodCallExpr stmt) {
    	return stmt.getScope().isPresent()
			&& (getIfExists(stmt.getScope()) instanceof MethodCallExpr
				|| getIfExists(stmt.getParentNode()) instanceof MethodCallExpr);
    }

	private static Node getIfExists(Optional<? extends Node> optional) {
		return optional.orElse(null);
	}

}
