package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import de.set.trainingUI.RemarkType;

final class FlipOperatorMutation extends Mutation {

    private final BinaryExpr expr;
    private final String correct;

    public FlipOperatorMutation(final BinaryExpr expr) {
        this.expr = expr;
        this.correct = expr.toString();
    }

    public static boolean isApplicable(final BinaryExpr ex) {
        return ex.getOperator() != flipOperator(ex.getOperator(), new Random(42), containsString(ex));
    }

    @Override
    public void apply(final Random r) {
        this.expr.setOperator(flipOperator(this.expr.getOperator(), r, containsString(this.expr)));
    }

    private static boolean containsString(BinaryExpr expr2) {
		return expr2.getLeft() instanceof StringLiteralExpr
			|| expr2.getRight() instanceof StringLiteralExpr;
	}

	private static Operator flipOperator(final Operator operator, final Random r, boolean withString) {
        switch (operator) {
        case PLUS:
        	if (withString) {
        		return operator;
        	} else {
                return another(r, operator, Operator.MINUS, Operator.PLUS);
        	}
        case MINUS:
            return another(r, operator, Operator.MINUS, Operator.PLUS);
        case LESS:
        case LESS_EQUALS:
        case GREATER:
        case GREATER_EQUALS:
            return another(r, operator, Operator.LESS, Operator.LESS_EQUALS, Operator.GREATER_EQUALS, Operator.GREATER);
        case OR:
        case AND:
            return another(r, operator, Operator.OR, Operator.AND);
        //$CASES-OMITTED$
        default:
            return operator;
        }
    }

    private static Operator another(
            final Random r, final Operator old, final Operator... operators) {
        final List<Operator> choices = new ArrayList<>(Arrays.asList(operators));
        choices.remove(old);
        if (choices.size() == 1) {
            return choices.get(0);
        } else {
            return pickRandom(choices, r);
        }
    }

    @Override
    public int getAnchorLine() {
        return this.expr.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
        final RemarkType type = determineTypeForOperator(this.expr.getOperator());
        this.setRemark(nbr, p, lines,
                type, ".+", "korrekt wäre " + this.correct);
    }

    /**
     * Returns {@link RemarkType.WRONG_COMPARISON} for operators usually used in a boolean
     * context and {@link RemarkType.WRONG_CALCULATION} for operators usually used in calculations.
     */
	static RemarkType determineTypeForOperator(Operator operator) throws AssertionError {
        switch (operator) {
        case AND:
        case OR:
        case LESS:
        case GREATER:
        case GREATER_EQUALS:
        case LESS_EQUALS:
        case EQUALS:
            return RemarkType.WRONG_COMPARISON;
        case PLUS:
        case MINUS:
        case MULTIPLY:
        case DIVIDE:
        case REMAINDER:
        case BINARY_AND:
        case BINARY_OR:
        case XOR:
        case LEFT_SHIFT:
        case UNSIGNED_RIGHT_SHIFT:
        case SIGNED_RIGHT_SHIFT:
            return RemarkType.WRONG_CALCULATION;
        //$CASES-OMITTED$
        default:
            throw new AssertionError("invalid operator");
        }
	}

}