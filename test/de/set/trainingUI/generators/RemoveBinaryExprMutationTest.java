package de.set.trainingUI.generators;

import org.junit.Test;

public class RemoveBinaryExprMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveStatementMutationTest.checkMutationCount(input, RemoveBinaryExprMutation.class, expectedCount);
	}

	private static void checkMutations(String input, int i, String... expecteds) {
		RemoveStatementMutationTest.checkMutations(input, RemoveBinaryExprMutation.class, i, expecteds);
	}

    @Test
    public void testRemovePlusWithLiteral() {
        final String input =
                "class A {\n"
                + "    public int a(int b) {\n"
                + "        return b + 43;\n"
                + "    }\n"
                + "}\n";

        checkMutations(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public int a(int b) {\n"
                + "        return b;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testRemovePlusWithoutLiteral() {
        final String input =
                "class A {\n"
                + "    public int a(int b, int c) {\n"
                + "        return b + c;\n"
                + "    }\n"
                + "}\n";

        checkMutations(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public int a(int b, int c) {\n"
                + "        return b;\n"
                + "    }\n"
                + "}\n",
                "class A {\n"
                + "\n"
                + "    public int a(int b, int c) {\n"
                + "        return c;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testNotApplicableForEquals() {
        final String input =
                "class A {\n"
                + "    public boolean a(int b, int c) {\n"
                + "        return b == c;\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, 0);
    }

    @Test
    public void testRemoveMultiple() {
        final String input =
                "class A {\n"
                + "    public int a(int b, int c) {\n"
                + "        return b * b - c;\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, 2);
        checkMutations(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public int a(int b, int c) {\n"
                + "        return b - c;\n"
                + "    }\n"
                + "}\n");
        checkMutations(input,
        		1,
                "class A {\n"
                + "\n"
                + "    public int a(int b, int c) {\n"
                + "        return b * b;\n"
                + "    }\n"
                + "}\n",
                "class A {\n"
                + "\n"
                + "    public int a(int b, int c) {\n"
                + "        return c;\n"
                + "    }\n"
                + "}\n");
    }
}
