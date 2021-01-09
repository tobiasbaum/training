package de.set.trainingUI.generators;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import de.set.trainingUI.RemarkType;

public class RemoveIfMutation extends Mutation {

    private final IfStmt n;
    private final Node parent;

    public RemoveIfMutation(final IfStmt n) {
        this.n = n;
        this.parent = this.n.getParentNode().get();
    }

	@Override
	public boolean isStillValid() {
		return isInCU(this.n) && isInSameCU(this.n, this.parent);
	}

    @Override
    public void apply(final Random r) {
    	if (this.n.hasElseBranch()) {
    		if (r.nextBoolean()) {
        		this.replace(this.n.getThenStmt());
    		} else {
        		this.replace(this.n.getElseStmt().get());
    		}
    	} else {
    		this.replace(this.n.getThenStmt());
    	}
    }

    private void replace(Statement replacement) {
    	if (replacement instanceof BlockStmt) {
    		final Node parent = this.n.getParentNode().get();
    		if (parent instanceof BlockStmt) {
    			final BlockStmt b = (BlockStmt) parent;
    			final int index = b.getStatements().indexOf(this.n);
    			b.getStatements().addAll(index, ((BlockStmt) replacement).getStatements());
    			b.remove(this.n);
    		} else {
        		this.n.replace(replacement);
    		}
    	} else {
    		this.n.replace(replacement);
    	}
    }

    @Override
    public int getAnchorLine() {
        return this.n.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        addBeginToEnd(lines, this.parent);
        this.setRemark(nbr, p, lines, RemarkType.MISSING_CODE, ".+", "Prüfung auf " + this.n.getCondition() + " fehlt");
    }

}
