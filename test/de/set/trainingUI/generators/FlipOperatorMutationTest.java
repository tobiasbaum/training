package de.set.trainingUI.generators;

import org.junit.Test;

public class FlipOperatorMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveStatementMutationTest.checkMutationCount(input, FlipOperatorMutation.class, expectedCount);
	}

	private static void checkMutation(String input, int i, long seed, String expected) {
		RemoveStatementMutationTest.checkMutation(input, FlipOperatorMutation.class, i, seed, expected);
	}

    @Test
    public void testMutateNumericPlus() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        return 1 + 2;\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        return 1 - 2;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testDontMutateStringPlus() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        return \"hallo\" + 2;\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, 0);
    }

}
