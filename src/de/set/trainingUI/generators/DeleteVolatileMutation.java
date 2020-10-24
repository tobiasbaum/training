package de.set.trainingUI.generators;

import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.FieldDeclaration;

import de.set.trainingUI.DefectFindTask.RemarkType;
import de.set.trainingUI.generators.MutationGenerator.Mutation;

public class DeleteVolatileMutation extends Mutation {

    private final FieldDeclaration field;

    public DeleteVolatileMutation(final FieldDeclaration field) {
        this.field = field;
    }

    public static boolean isApplicable(FieldDeclaration field) {
    	return field.hasModifier(Keyword.VOLATILE);
    }

    @Override
    public void apply(final Random r) {
    	this.field.removeModifier(Keyword.VOLATILE);
    }

    @Override
    public int getAnchorLine() {
        return this.field.getBegin().get().line;
    }

    @Override
    public void createRemark(final int nbr, final Properties p) {
        final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
        this.setRemark(nbr, p, lines, RemarkType.MISSING_CODE, ".+",
        		"Dem Attribut " + this.field.getVariable(0).getNameAsString() + " fehlt volatile");
    }
}
