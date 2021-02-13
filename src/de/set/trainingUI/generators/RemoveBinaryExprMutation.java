package de.set.trainingUI.generators;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;

import de.set.trainingUI.RemarkType;

public class RemoveBinaryExprMutation extends Mutation {

    private final BinaryExpr expr;

	public RemoveBinaryExprMutation(BinaryExpr expr) {
		this.expr = expr;
	}

	public static boolean isApplicable(BinaryExpr expr) {
		switch (expr.getOperator()) {
		case AND:
		case BINARY_AND:
		case BINARY_OR:
		case DIVIDE:
		case LEFT_SHIFT:
		case MINUS:
		case MULTIPLY:
		case OR:
		case PLUS:
		case REMAINDER:
		case SIGNED_RIGHT_SHIFT:
		case UNSIGNED_RIGHT_SHIFT:
		case XOR:
			return true;
		default:
			return false;
		}
	}

	@Override
	public void apply(Random r) {
		final Expression child1;
		final Expression child2;
		if (r.nextBoolean()) {
			child1 = this.expr.getLeft();
			child2 = this.expr.getRight();
		} else {
			child1 = this.expr.getRight();
			child2 = this.expr.getLeft();
		}

		final Expression replacement;
		if (child1 instanceof LiteralExpr) {
			replacement = child2;
		} else {
		    replacement = child1;
		}
		this.expr.replace(replacement);
	}

	@Override
	public void createRemark(int nbr, RemarkCreator p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        addBeginToEnd(lines, this.expr);
        final Set<RemarkType> types = EnumSet.of(RemarkType.MISSING_CODE);
        types.add(FlipOperatorMutation.determineTypeForOperator(this.expr.getOperator()));
        this.setRemark(nbr, p, lines, types, ".+", "müsste " + this.expr + " sein");
	}

	@Override
	public int getAnchorLine() {
		return this.expr.getBegin().get().line;
	}

}
