package de.set.trainingUI.generators;

import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import de.set.trainingUI.DefectFindTask.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

final class InvertMutation extends Mutation {

    private final IfStmt ifStmt;

    public InvertMutation(final IfStmt n) {
        this.ifStmt = n;
    }

	public static boolean isApplicable(IfStmt n) {
		if (n.getElseStmt().isPresent()) {
			return true;
		}
		// don't invert simple comparisons because this looks strange in the code
		return !(n.getCondition() instanceof BinaryExpr);
	}

    @Override
    public void apply(final Random r) {
    	if (this.ifStmt.getElseStmt().isPresent()) {
    		final Statement t = this.ifStmt.getThenStmt();
    		final Statement e = this.ifStmt.getElseStmt().get();
    		this.ifStmt.setThenStmt(e);
    		this.ifStmt.setElseStmt(t);
    	} else {
    		if (this.isNegated(this.ifStmt.getCondition())) {
    			this.ifStmt.setCondition(((UnaryExpr) this.ifStmt.getCondition()).getExpression());
    		} else {
    			this.ifStmt.setCondition(new UnaryExpr(
    					this.ifStmt.getCondition(),
    					UnaryExpr.Operator.LOGICAL_COMPLEMENT));
    		}
    	}
    }

    private boolean isNegated(Expression condition) {
		return condition instanceof UnaryExpr
			&& ((UnaryExpr) condition).getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT;
	}

	@Override
    public int getAnchorLine() {
        return this.ifStmt.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
        this.setRemark(nbr, p, lines, RemarkType.WRONG_CONDITION, ".+", "die Bedingung muss invertiert werden");
    }

}