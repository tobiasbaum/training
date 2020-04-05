package de.set.trainingUI.generators;

import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;

import de.set.trainingUI.DefectFindTask.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

final class InvertMutation extends Mutation {

    private final IfStmt ifStmt;

    public InvertMutation(final IfStmt n) {
        this.ifStmt = n;
    }

    @Override
    public void apply(final Random r) {
        this.ifStmt.setCondition(new UnaryExpr(
                this.ifStmt.getCondition(),
                UnaryExpr.Operator.LOGICAL_COMPLEMENT));
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