package de.set.trainingUI.generators;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;

import de.set.trainingUI.RemarkType;

public class RemoveTernaryMutation extends Mutation {

	private final ConditionalExpr expr;

	public RemoveTernaryMutation(ConditionalExpr expr) {
		this.expr = expr;
	}

	@Override
	public boolean isStillValid() {
		return isInCU(this.expr);
	}

	@Override
	public void apply(Random r) {
		Expression replacement;
		if (r.nextBoolean()) {
			replacement = this.expr.getThenExpr();
		} else {
			replacement = this.expr.getElseExpr();
		}
		this.expr.replace(replacement);
	}

	@Override
	public void createRemark(int nbr, Properties p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        lines.add(this.expr.getBegin().get().line);
        lines.add(this.expr.getBegin().get().line + 1);
        this.setRemark(nbr, p, lines, RemarkType.MISSING_CODE, ".+", "Prüfung auf " + this.expr.getCondition() + " fehlt");
	}

	@Override
	public int getAnchorLine() {
		return this.expr.getBegin().get().line;
	}

}
