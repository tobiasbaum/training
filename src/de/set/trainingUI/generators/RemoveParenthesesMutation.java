package de.set.trainingUI.generators;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import de.set.trainingUI.RemarkType;

public class RemoveParenthesesMutation extends Mutation {

    private final EnclosedExpr expr;

	public RemoveParenthesesMutation(EnclosedExpr expr) {
		this.expr = expr;
	}

	public static boolean isApplicable(EnclosedExpr expr) {
		if (expr.getParentNode().isEmpty()) {
			return false;
		}
		final Node parent = expr.getParentNode().get();
		if (!(parent instanceof BinaryExpr)) {
			return false;
		}
		final BinaryExpr.Operator parentOperator = ((BinaryExpr) parent).getOperator();
		if (!(expr.getInner() instanceof BinaryExpr)) {
			return false;
		}
		final BinaryExpr.Operator childOperator = ((BinaryExpr) expr.getInner()).getOperator();
		final int parentBinding = determineBinding(parentOperator);
		final int childBinding = determineBinding(childOperator);
		return parentBinding <= childBinding;
	}

	private static int determineBinding(Operator operator) {
		switch (operator) {
		case MULTIPLY:
		case DIVIDE:
		case REMAINDER:
			return 1;
		case PLUS:
		case MINUS:
			return 2;
		case LEFT_SHIFT:
		case SIGNED_RIGHT_SHIFT:
		case UNSIGNED_RIGHT_SHIFT:
			return 3;
		case LESS:
		case LESS_EQUALS:
		case GREATER:
		case GREATER_EQUALS:
			return 4;
		case EQUALS:
		case NOT_EQUALS:
			return 5;
		case BINARY_AND:
			return 6;
		case XOR:
			return 7;
		case BINARY_OR:
			return 8;
		case AND:
			return 9;
		case OR:
			return 10;
		default:
			throw new AssertionError("invalid operator " + operator);
		}
	}

	@Override
	public void apply(Random r) {
		this.expr.replace(this.expr.getInner());
	}

	@Override
	public void createRemark(int nbr, Properties p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        addBeginToEnd(lines, this.expr);
        final Set<RemarkType> types = EnumSet.of(RemarkType.MISSING_CODE);
        this.addTypeOf(this.expr.getParentNode().get(), types);
        this.addTypeOf(this.expr.getInner(), types);
        this.setRemark(nbr, p, lines, types, ".+", this.expr + " muss in Klammern stehen");
	}

	private void addTypeOf(Node node, Set<RemarkType> set) {
		final RemarkType type = this.determineType(node);
		if (type != null) {
			set.add(type);
		}
	}

	private RemarkType determineType(Node node) {
		if (node instanceof IfStmt
				|| node instanceof ConditionalExpr
				|| node instanceof WhileStmt
				|| node instanceof DoStmt) {
			return RemarkType.WRONG_COMPARISON;
		} else if (node instanceof BinaryExpr) {
			switch (((BinaryExpr) node).getOperator()) {
			case AND:
			case EQUALS:
			case GREATER:
			case GREATER_EQUALS:
			case NOT_EQUALS:
			case LESS:
			case LESS_EQUALS:
			case OR:
				return RemarkType.WRONG_COMPARISON;
			default:
				return RemarkType.WRONG_CALCULATION;
			}
		} else {
			return null;
		}
	}

	@Override
	public int getAnchorLine() {
		return this.expr.getBegin().get().line;
	}

}
