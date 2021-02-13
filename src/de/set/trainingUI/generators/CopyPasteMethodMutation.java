package de.set.trainingUI.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.SimpleName;

import de.set.trainingUI.RemarkType;

public class CopyPasteMethodMutation extends Mutation {

	private final MethodDeclaration method;
	private int newBodyLength = -1;

	public CopyPasteMethodMutation(MethodDeclaration method) {
		this.method = method;
	}

	public static boolean isApplicable(MethodDeclaration m) {
		return !getAlternativeMethods(m).isEmpty();
	}

	private static List<MethodDeclaration> getAlternativeMethods(MethodDeclaration m) {
		final Node parent = m.getParentNode().get();
		if (!(parent instanceof TypeDeclaration)) {
			return Collections.emptyList();
		}
		final TypeDeclaration<?> parentType = (TypeDeclaration<?>) parent;
		final List<MethodDeclaration> ret = new ArrayList<MethodDeclaration>();
		for (final MethodDeclaration otherMethod : parentType.getMethods()) {
			if (otherMethod != m
					&& hasCompatibleSignature(otherMethod, m)
					&& !m.getBody().equals(otherMethod.getBody())) {
				ret.add(otherMethod);
			}
		}
		return ret;
	}

	/**
	 * Returns true iff the code in m1 could probably also be used in m2,
	 * according to the method signatures.
	 */
	private static boolean hasCompatibleSignature(
			MethodDeclaration m1, MethodDeclaration m2) {
		if (m1.isAbstract() || m1.isNative()) {
			return false;
		}
		if (!m1.getTypeAsString().equals(m2.getTypeAsString())) {
			return false;
		}
		for (final Parameter p : m1.getParameters()) {
			if (!hasParameter(m2, p.getName(), p.getTypeAsString())) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasParameter(MethodDeclaration m2, SimpleName name, String type) {
		for (final Parameter p : m2.getParameters()) {
			if (p.getName().equals(name) && p.getTypeAsString().equals(type)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void apply(Random r) {
		final MethodDeclaration alternative = pickRandom(getAlternativeMethods(this.method), r);
		this.newBodyLength = alternative.getEnd().get().line - alternative.getBegin().get().line;
		this.method.setBody(alternative.getBody().get().clone());
		this.method.setAbstract(false);
		this.method.setNative(false);
	}

	@Override
	public void createRemark(int nbr, RemarkCreator p) {
        final Set<Integer> lines = new LinkedHashSet<>();
        assert this.newBodyLength > 0;
        final int anchor = this.getAnchorLine();
        for (int i = anchor; i < anchor + this.newBodyLength; i++) {
        	lines.add(i);
        }
        final Set<RemarkType> types = EnumSet.of(
        		RemarkType.OTHER_ALGORITHMIC_PROBLEM, RemarkType.DUPLICATE_CODE);
        this.setRemark(nbr, p, lines, types, ".+", this.method.getNameAsString() + " ist falsch implementiert. Copy-Paste?");
	}

	@Override
	public int getAnchorLine() {
		return this.method.getBegin().get().line;
	}

}
