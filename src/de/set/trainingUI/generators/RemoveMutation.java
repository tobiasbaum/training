package de.set.trainingUI.generators;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;

import de.set.trainingUI.DefectFindTask.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

final class RemoveMutation extends Mutation {

    private final Statement n;
    private final Node parent;

    public RemoveMutation(final Statement n) {
        this.n = n;
        this.parent = this.n.getParentNode().get();
    }

    @Override
    public void apply(final Random r) {
        this.n.remove();
    }

    @Override
    public int getAnchorLine() {
        return this.n.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        final int start = this.parent.getBegin().get().line;
        final int end = this.parent.getEnd().get().line;
        for (int i = start; i < end; i++) {
            lines.add(i);
        }
        this.setRemark(nbr, p, lines, RemarkType.MISSING_CODE, ".+", this.n.toString() + " fehlt");
    }

}