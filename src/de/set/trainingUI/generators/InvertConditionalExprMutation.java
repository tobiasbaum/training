package de.set.trainingUI.generators;

import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;

import de.set.trainingUI.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

public class InvertConditionalExprMutation extends Mutation {

	private final ConditionalExpr expr;

	public InvertConditionalExprMutation(ConditionalExpr n) {
		this.expr = n;
	}

	@Override
	public boolean isStillValid() {
		return isInCU(this.expr);
	}

    @Override
    public void apply(final Random r) {
		final Expression t = this.expr.getThenExpr();
		final Expression e = this.expr.getElseExpr();
		this.expr.setThenExpr(e);
		this.expr.setElseExpr(t);
    }

    @Override
    public int getAnchorLine() {
        return this.expr.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
        this.setRemark(nbr, p, lines, RemarkType.WRONG_COMPARISON, ".+", "die Bedingung muss invertiert werden");
    }

}
