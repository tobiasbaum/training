package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class RemoveUnnecessaryParenthesesOptimization implements CodeOptimization {

	@Override
	public boolean optimize(CompilationUnit cu) {
		final List<EnclosedExpr> unnecessary = new ArrayList<>();
		cu.accept(new VoidVisitorAdapter<Void>() {
			@Override
			public void visit(EnclosedExpr expr, Void arg) {
				super.visit(expr, arg);
				if (isUnnecessaryFor(expr.getInner())) {
					unnecessary.add(expr);
				}
			}
		}, null);

		for (final EnclosedExpr expr : unnecessary) {
			expr.replace(expr.getInner());
		}
		return !unnecessary.isEmpty();
	}

	private static boolean isUnnecessaryFor(Expression inner) {
		return inner instanceof LiteralExpr
			|| inner instanceof MethodCallExpr
			|| inner instanceof NameExpr
			|| inner instanceof ThisExpr;
	}
}
