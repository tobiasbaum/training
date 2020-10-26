package de.set.trainingUI.generators;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import de.set.trainingUI.generators.MutationGenerator.Mutation;

public class RemoveMethodCallMutationTest {

    private static CompilationUnit parse(final String fileContent) {
        return StaticJavaParser.parse(fileContent);
    }

    private static List<Mutation> determineApplicableMutations(
    		CompilationUnit cu, Class<? extends Mutation> mutationType) {
    	final List<Mutation> mutations = MutationGenerator.findPossibleMutations(cu);
    	mutations.removeIf((Mutation m) -> !mutationType.isInstance(m));
    	return mutations;
    }

    static void checkMutation(
    		String input,
    		Class<? extends Mutation> mutationType,
    		int mutationIndex,
    		String expectedSource) {
    	checkMutation(input, mutationType, mutationIndex, 123L, expectedSource);
    }

    static void checkMutation(
    		String input,
    		Class<? extends Mutation> mutationType,
    		int mutationIndex,
    		long seed,
    		String expectedSource) {
    	final CompilationUnit cu = parse(input);
        final List<Mutation> m = determineApplicableMutations(cu, mutationType);
    	m.get(mutationIndex).apply(new Random(seed));

    	assertEquals(expectedSource, cu.toString());
    }

    static void checkMutationCount(
    		String input,
    		Class<? extends Mutation> mutationType,
    		int expectedCount) {
    	final CompilationUnit cu = parse(input);
        final List<Mutation> m = determineApplicableMutations(cu, mutationType);
        assertEquals(expectedCount, m.size());
    }

    @Test
    public void testSingleStatementsAreNotRemoved() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, RemoveStatementMutation.class, 0);
    }

    @Test
    public void testRemoveMethodCall() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        System.out.println(\"y\");\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"y\");\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 2);
    }

    @Test
    public void testRemoveChainedCall() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        String asdf = \"qwer\";\n"
                + "        Runtime.getRuntime().doStuff(asdf);\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        String asdf = \"qwer\";\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 1);
    }

    @Test
    public void testRemoveCompleteIfStatement() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 2);
    }

    @Test
    public void testRemoveStatementInIf() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"x\");\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"x\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 2);
    }

}
