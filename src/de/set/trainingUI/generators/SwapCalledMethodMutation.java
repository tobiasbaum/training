package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import de.set.trainingUI.RemarkType;

public class SwapCalledMethodMutation extends Mutation {


    private static class MethodInfo {
        private final String name;
        private final String returnType;
		private final List<String> parameterTypes;
		private final AccessSpecifier accessSpecifier;
		private final String parent;

        public MethodInfo(final String name,
        		final String returnType,
        		List<String> parameterTypes,
        		AccessSpecifier accessSpecifier,
        		String parent) {
            this.name = name;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
            this.accessSpecifier = accessSpecifier;
            this.parent = parent;
        }

		public boolean canBeReplacementFor(MethodInfo calledMethod) {
			return this.returnType.equals(calledMethod.returnType)
				&& this.parent.equals(calledMethod.parent)
				&& this.parameterTypes.equals(calledMethod.parameterTypes)
				&& this.isAtLeastAsPublicAs(calledMethod.accessSpecifier);
		}

		private boolean isAtLeastAsPublicAs(AccessSpecifier other) {
			return toNumericVisibility(this.accessSpecifier) >= toNumericVisibility(other);
		}

		private static int toNumericVisibility(AccessSpecifier a) {
			switch (a) {
			case PUBLIC:
				return 3;
			case PROTECTED:
				return 2;
			case PACKAGE_PRIVATE:
				return 1;
			case PRIVATE:
				return 0;
			default:
				throw new AssertionError();
			}
		}
    }

    public static class SwapMethodData {

        private final Map<String, List<MethodInfo>> methods = new HashMap<>();
        private final List<MethodInfo> allMethods = new ArrayList<>();

		public void add(MethodInfo methodInfo) {
			final String key = methodInfo.name + "," + methodInfo.parameterTypes.size();
			List<MethodInfo> list = this.methods.get(key);
			if (list == null) {
				list = new ArrayList<>();
				this.methods.put(key, list);
			}
			list.add(methodInfo);
			this.allMethods.add(methodInfo);
		}

		public List<String> getPossibleOtherNames(MethodCallExpr n) {
			final String key = n.getNameAsString() + "," + n.getArguments().size();
			final List<MethodInfo> possiblyCalledMethods = this.methods.get(key);
			if (possiblyCalledMethods == null) {
				return Collections.emptyList();
			}
			//intersect all possibilities
			final List<String> ret = new ArrayList<>(this.getPossibleOtherNames(possiblyCalledMethods.get(0)));
			for (int i = 1; i < possiblyCalledMethods.size(); i++) {
				ret.retainAll(this.getPossibleOtherNames(possiblyCalledMethods.get(i)));
			}
			return ret;
		}

        private Set<String> getPossibleOtherNames(MethodInfo calledMethod) {
        	final Set<String> ret = new LinkedHashSet<String>();
        	for (final MethodInfo m : this.allMethods) {
        		if (m.canBeReplacementFor(calledMethod)) {
        			ret.add(m.name);
        		}
        	}
        	ret.remove(calledMethod.name);
			return ret;
		}

		public boolean isApplicable(final MethodCallExpr n) {
            return !this.getPossibleOtherNames(n).isEmpty();
        }

    }


    private final MethodCallExpr expr;
    private final String correctName;
    private final List<String> possibleOtherNames;

    public SwapCalledMethodMutation(final SwapMethodData data, final MethodCallExpr n) {
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
                "die Methode " + this.correctName + " muss statt " + this.expr.getNameAsString() + " verwendet werden");
    }

    @Override
    public int getAnchorLine() {
        return this.expr.getBegin().get().line;
    }

    public static SwapMethodData analyze(final CompilationUnit ast) {
        final SwapMethodData d = new SwapMethodData();
        ast.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(final MethodDeclaration n, final Void v) {
                super.visit(n, v);
                d.add(new MethodInfo(
                        n.getNameAsString(),
                        n.getType().toString(),
                        mapParameters(n.getParameters()),
                        n.getAccessSpecifier(),
                        getParentName(n)));
            }
        }, null);
        return d;
    }

	private static List<String> mapParameters(NodeList<Parameter> parameters) {
		final List<String> ret = new ArrayList<>();
		for (final Parameter p : parameters) {
			ret.add(p.getTypeAsString());
		}
		return ret;
	}

	private static String getParentName(MethodDeclaration n) {
		final Node parent = n.getParentNode().get();
		return parent instanceof NodeWithSimpleName ? ((NodeWithSimpleName<?>) parent).getNameAsString() : "";
	}
}
