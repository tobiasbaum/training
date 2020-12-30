package de.set.trainingUI.generators;

import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.MethodDeclaration;

import de.set.trainingUI.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

public class DeleteSynchronizedMutation extends Mutation {

    private final MethodDeclaration method;

    public DeleteSynchronizedMutation(final MethodDeclaration field) {
        this.method = field;
    }

    public static boolean isApplicable(MethodDeclaration field) {
    	return field.hasModifier(Keyword.SYNCHRONIZED);
    }

	@Override
	public boolean isStillValid() {
		return isInCU(this.method);
	}

    @Override
    public void apply(final Random r) {
    	this.method.removeModifier(Keyword.SYNCHRONIZED);
    }

    @Override
    public int getAnchorLine() {
        return this.method.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
        this.setRemark(nbr, p, lines, RemarkType.MISSING_CODE, ".+",
        		"Die Methode " + this.method.getNameAsString() + " muss synchronized sein");
    }
}
