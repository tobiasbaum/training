package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import de.set.trainingUI.RemarkType;

@SuppressWarnings("nls")
public class SwapVariableExpressionMutation extends Mutation {


    private static class VarInfo {
        private final String name;
        private final String type;
        private final Position pos;
        private final Node block;

        public VarInfo(final String name, final String type, final Position pos, final Node block) {
            this.name = name;
            this.type = type;
            this.pos = pos;
            this.block = block;
        }

        public boolean isAttribute() {
        	return this.block instanceof TypeDeclaration;
        }

		public boolean isLessSpecificThan(VarInfo v) {
			return this.isLessSpecificThan(v.block);
		}

		private boolean isLessSpecificThan(Node otherBlock) {
			return otherBlock == this.block
				|| (otherBlock.getParentNode().isPresent()
					&& this.isLessSpecificThan(otherBlock.getParentNode().get()));
		}
    }

    public static class SwapVariableData {

        private final List<VarInfo> vars = new ArrayList<>();

        public boolean isApplicable(final NameExpr n) {
            return !this.getPossibleOtherNames(n).isEmpty();
        }

        private List<String> getPossibleOtherNames(final NameExpr n) {
            final List<VarInfo> varsInScope = this.getVarsInScope(n);
            final String varName = n.getNameAsString();
            VarInfo cur = null;
            for (final VarInfo v : varsInScope) {
                if (v.name.equals(varName) && (cur == null || cur.isLessSpecificThan(v))) {
                    cur = v;
                }
            }
            if (cur == null) {
                return Collections.emptyList();
            }
            final List<String> ret = new ArrayList<>();
            for (final VarInfo v : varsInScope) {
                if (v.type.equals(cur.type)
                		&& v.isAttribute() == cur.isAttribute()
                		&& !v.name.equals(cur.name)) {
                    ret.add(v.name);
                }
            }
            return ret;
        }

        private List<VarInfo> getVarsInScope(final NameExpr n) {
            final List<VarInfo> ret = new ArrayList<>();
            final Position pos = n.getBegin().get();
            for (final VarInfo v : this.vars) {
                if ((v.pos.isBefore(pos) || v.isAttribute()) && this.belongsToBlock(n, v.block)) {
                    ret.add(v);
                }
            }
            return ret;
        }

        private boolean belongsToBlock(final NameExpr n, final Node block) {
            Node cur = n;
            do {
                cur = getSurroundingBlock(cur);
                if (block == cur) {
                    return true;
                }
            } while (cur != null);
            return false;
        }

    }


    private final NameExpr expr;
    private final String correctName;
    private final List<String> possibleOtherNames;

    public SwapVariableExpressionMutation(final SwapVariableData data, final NameExpr n) {
        this.expr = n;
        this.correctName = n.getNameAsString();
        this.possibleOtherNames = data.getPossibleOtherNames(n);
    }

    @Override
    public void apply(final Random r) {
		this.expr.setName(pickRandom(this.possibleOtherNames, r));
    }

    @Override
    public void createRemark(final int nbr, final RemarkCreator p) {
        final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
        final Set<RemarkType> types = EnumSet.of(RemarkType.OTHER_ALGORITHMIC_PROBLEM);
        if (this.isInCalculation()) {
        	types.add(RemarkType.WRONG_CALCULATION);
        }
        if (this.isBooleanExpr()) {
        	types.add(RemarkType.WRONG_COMPARISON);
        }
        this.setRemark(nbr, p, lines, types, ".+",
                "die Variable " + this.correctName + " muss statt " + this.expr.getNameAsString() + " verwendet werden");
    }

    private boolean isInCalculation() {
    	return this.expr.getParentNode().isPresent()
			&& this.expr.getParentNode().get() instanceof BinaryExpr
			&& isCalculation(((BinaryExpr) this.expr.getParentNode().get()).getOperator());
	}

    private boolean isBooleanExpr() {
    	return this.isComparisonExpr() || this.isInIf();
	}

    private boolean isComparisonExpr() {
    	return this.expr.getParentNode().isPresent()
			&& this.expr.getParentNode().get() instanceof BinaryExpr
			&& isComparison(((BinaryExpr) this.expr.getParentNode().get()).getOperator());
	}

    private boolean isInIf() {
    	return isInIf(this.expr);
	}

    private static boolean isInIf(Expression ex) {
    	final Node parent = ex.getParentNode().orElse(null);
    	return parent instanceof IfStmt
			|| (parent instanceof Expression && isInIf((Expression) parent));
    }

	private static boolean isCalculation(Operator operator) {
		switch (operator) {
		case PLUS:
		case MINUS:
		case MULTIPLY:
		case DIVIDE:
		case REMAINDER:
			return true;
		default:
			return false;
		}
	}

	private static boolean isComparison(Operator operator) {
		switch (operator) {
		case LESS:
		case LESS_EQUALS:
		case GREATER:
		case GREATER_EQUALS:
		case EQUALS:
		case NOT_EQUALS:
		case OR:
		case AND:
			return true;
		default:
			return false;
		}
	}

	@Override
    public int getAnchorLine() {
        return this.expr.getBegin().get().line;
    }

    public static SwapVariableData analyze(final CompilationUnit ast) {
        final SwapVariableData d = new SwapVariableData();
        ast.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(final VariableDeclarator n, final Void v) {
                super.visit(n, v);
                d.vars.add(new VarInfo(
                        n.getNameAsString(),
                        n.getType().toString(),
                        n.getEnd().get(),
                        getSurroundingBlock(n)));
            }
            @Override
            public void visit(final Parameter n, final Void v) {
                super.visit(n, v);
                d.vars.add(new VarInfo(
                        n.getNameAsString(),
                        n.getType().toString(),
                        n.getEnd().get(),
                        getSurroundingBlock(n)));
            }
        }, null);
        return d;
    }

    private static Node getSurroundingBlock(final Node n) {
        final Optional<Node> parent = n.getParentNode();
        if (!parent.isPresent()) {
            return null;
        } else if (isBlockBuilding(parent.get())) {
            return parent.get();
        } else {
            return getSurroundingBlock(parent.get());
        }
    }

    private static boolean isBlockBuilding(final Node node) {
        return node instanceof BlockStmt
            || node instanceof ForStmt
            || node instanceof ForEachStmt
            || node instanceof CallableDeclaration
            || node instanceof TypeDeclaration;
    }

    @Override
	public String toString() {
    	return "SwapVariableExpressionMutation for " + this.correctName;
    }

}
