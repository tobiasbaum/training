package de.set.trainingUI.generators;

import org.junit.Test;

public class RemoveTernaryMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveMethodCallMutationTest.checkMutationCount(input, RemoveTernaryMutation.class, expectedCount);
	}

	private static void checkMutation(String input, int i, long seed, String expected) {
		RemoveMethodCallMutationTest.checkMutation(input, RemoveTernaryMutation.class, i, seed, expected);
	}

    @Test
    public void testRemoveCompleteIfStatement() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        return System.out != null ? 1 : 2;\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        return 1;\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		0,
        		-1,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

}
