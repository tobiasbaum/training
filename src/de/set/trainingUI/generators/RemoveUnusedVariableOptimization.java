package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class RemoveUnusedVariableOptimization implements CodeOptimization {

	@Override
	public boolean optimize(CompilationUnit cu) {
		final AtomicBoolean changed = new AtomicBoolean();
		final List<VariableDeclarationExpr> toRemove = new ArrayList<>();
		cu.accept(new VoidVisitorAdapter<Void>() {
		    @Override
			public void visit(final VariableDeclarationExpr n, final Void arg) {
		    	for (final VariableDeclarator d : new ArrayList<>(n.getVariables())) {
		    		if (RemoveUnusedVariableOptimization.this.isUnused(d)) {
		    			if (d.getInitializer().isEmpty() || !couldHaveSideEffect(d.getInitializer().get())) {
		    				d.removeForced();
		    			} else {
		    				if (n.getVariables().size() > 1) {
		    					throw new UnsupportedOperationException("not yet supported");
		    				}
		    				n.replace(d.getInitializer().get());
		    			}
		    			changed.set(true);
		    		}
		    	}
		    	if (n.getVariables().isEmpty()) {
		    		toRemove.add(n);
		    	}
		    }
		}, null);
		for (final VariableDeclarationExpr d : toRemove) {
			d.removeForced();
		}
		return changed.get();
	}

	private static boolean couldHaveSideEffect(Expression expression) {
		final Boolean ret = expression.accept(new GenericVisitorAdapter<Boolean, Void>() {
			@Override
			public Boolean visit(AssignExpr e, Void v) {
				return Boolean.TRUE;
			}
			@Override
			public Boolean visit(ObjectCreationExpr e, Void v) {
				return Boolean.TRUE;
			}
			@Override
			public Boolean visit(MethodCallExpr e, Void v) {
				return Boolean.TRUE;
			}
		}, null);
		return ret != null && ret.booleanValue();
	}

	private boolean isUnused(VariableDeclarator d) {
		final BlockStmt parentBlock = d.findAncestor(BlockStmt.class).get();
		final Boolean foundUse = parentBlock.accept(new GenericVisitorAdapter<Boolean, Void>() {
			@Override
			public Boolean visit(NameExpr e, Void v) {
				if (e.getNameAsString().equals(d.getNameAsString())) {
					return Boolean.TRUE;
				} else {
					return null;
				}
			}
		}, null);
		return foundUse == null || !foundUse.booleanValue();
	}

}
