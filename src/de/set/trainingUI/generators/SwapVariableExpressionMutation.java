package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import de.set.trainingUI.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

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
                if (v.name.equals(varName)) {
                    cur = v;
                }
            }
            if (cur == null) {
                return Collections.emptyList();
            }
            final List<String> ret = new ArrayList<>();
            for (final VarInfo v : varsInScope) {
                if (v.type.equals(cur.type) && v != cur) {
                    ret.add(v.name);
                }
            }
            return ret;
        }

        private List<VarInfo> getVarsInScope(final NameExpr n) {
            final List<VarInfo> ret = new ArrayList<>();
            final Position pos = n.getBegin().get();
            for (final VarInfo v : this.vars) {
                if (v.pos.isBefore(pos) && this.belongsToBlock(n, v.block)) {
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
	public boolean isStillValid() {
		return isInCU(this.expr);
	}

    @Override
    public void apply(final Random r) {
        this.expr.setName(pickRandom(this.possibleOtherNames, r));
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
        this.setRemark(nbr, p, lines, RemarkType.OTHER_ALGORITHMIC_PROBLEM, ".+",
                "die Variable " + this.correctName + " muss statt " + this.expr.getNameAsString() + " verwendet werden");
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
            || node instanceof ForStmt;
    }

}
