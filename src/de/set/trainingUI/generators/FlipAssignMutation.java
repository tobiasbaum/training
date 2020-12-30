package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;

import de.set.trainingUI.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

public class FlipAssignMutation extends Mutation {

    private final AssignExpr expr;
    private final String correct;

    public FlipAssignMutation(final AssignExpr expr) {
        this.expr = expr;
        this.correct = expr.toString();
    }

    public static boolean isApplicable(final AssignExpr ex) {
        return ex.getOperator() != flipOperator(ex.getOperator(), new Random(42));
    }

	@Override
	public boolean isStillValid() {
		return isInCU(this.expr);
	}

    @Override
    public void apply(final Random r) {
        this.expr.setOperator(flipOperator(this.expr.getOperator(), r));
    }

    private static Operator flipOperator(final Operator operator, final Random r) {
        switch (operator) {
        case PLUS:
        case MINUS:
            return another(r, operator, Operator.MINUS, Operator.PLUS, Operator.ASSIGN);
        case MULTIPLY:
        case DIVIDE:
        case REMAINDER:
            return another(r, operator, Operator.MINUS, Operator.PLUS, Operator.MULTIPLY,
            		Operator.DIVIDE, Operator.REMAINDER, Operator.ASSIGN);
        case BINARY_AND:
        case BINARY_OR:
        case XOR:
            return another(r, operator, Operator.BINARY_AND, Operator.BINARY_OR, Operator.ASSIGN);
        case LEFT_SHIFT:
            return another(r, operator, Operator.SIGNED_RIGHT_SHIFT, Operator.UNSIGNED_RIGHT_SHIFT, Operator.ASSIGN);
        case SIGNED_RIGHT_SHIFT:
        case UNSIGNED_RIGHT_SHIFT:
            return another(r, operator, Operator.LEFT_SHIFT, Operator.ASSIGN);
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
        final RemarkType type = RemarkType.WRONG_CALCULATION;
        this.setRemark(nbr, p, lines,
                type, ".+", "korrekt wäre " + this.correct);
    }

}
