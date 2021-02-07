package de.set.trainingUI.generators;

import org.junit.Test;

public class RemoveParenthesesMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveStatementMutationTest.checkMutationCount(input, RemoveParenthesesMutation.class, expectedCount);
	}

	private static void checkMutation(String input, int i, String expected) {
		RemoveStatementMutationTest.checkMutation(input, RemoveParenthesesMutation.class, i, 23, expected);
	}

    @Test
    public void testRemoveInCalculation() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        return (5 + 3) * 2;\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        return 5 + 3 * 2;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testRemoveInAndOr() {
        final String input =
                "class A {\n"
                + "    public int a(boolean x, boolean y, boolean z) {\n"
                + "        return (x || y) && z;\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public int a(boolean x, boolean y, boolean z) {\n"
                + "        return x || y && z;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testRemoveInStringConcat() {
        final String input =
                "class A {\n"
                + "    public int a(int x) {\n"
                + "        return \"x + 1 = \" + (x + 1) + \"!\";\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public int a(int x) {\n"
                + "        return \"x + 1 = \" + x + 1 + \"!\";\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testNotApplicableIfSemanticWillNotChange() {
        final String input =
                "class A {\n"
                + "    public int a(boolean x, boolean y, boolean z) {\n"
                + "        return x || (y && z);\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, 0);
    }

}
