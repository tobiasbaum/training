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

import de.set.trainingUI.DefectFindTask.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

final class FlipOperatorMutation extends Mutation {

    private final BinaryExpr expr;
    private final String correct;

    public FlipOperatorMutation(final BinaryExpr expr) {
        this.expr = expr;
        this.correct = expr.toString();
    }

    public static boolean isApplicable(final BinaryExpr ex) {
        return ex.getOperator() != flipOperator(ex.getOperator(), new Random(42));
    }

    @Override
    public void apply(final Random r) {
        this.expr.setOperator(flipOperator(this.expr.getOperator(), r));
    }

    private static Operator flipOperator(final Operator operator, final Random r) {
        switch (operator) {
        case PLUS:
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
        final RemarkType type;
        switch (this.expr.getOperator()) {
        case AND:
        case OR:
            type = RemarkType.WRONG_CONDITION;
            break;
        case LESS:
        case GREATER:
        case GREATER_EQUALS:
        case LESS_EQUALS:
            type = RemarkType.WRONG_COMPARISON;
            break;
        case PLUS:
        case MINUS:
            type = RemarkType.WRONG_CALCULATION;
            break;
        //$CASES-OMITTED$
        default:
            throw new AssertionError("invalid operator");
        }
        this.setRemark(nbr, p, lines,
                type, ".+", "korrekt wäre " + this.correct);
    }

}