package de.set.trainingUI.generators;

import org.junit.Test;

public class InvertMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveStatementMutationTest.checkMutationCount(input, InvertMutation.class, expectedCount);
	}

	private static void checkMutation(String input, int i, long seed, String expected) {
		RemoveStatementMutationTest.checkMutation(input, InvertMutation.class, i, seed, expected);
	}

    @Test
    public void testMutateOnlyIf() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (isA()) {\n"
                + "            return 1;\n"
                + "        }\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (!isA()) {\n"
                + "            return 1;\n"
                + "        }\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testRemoveNot() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (!isA()) {\n"
                + "            return 1;\n"
                + "        }\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (isA()) {\n"
                + "            return 1;\n"
                + "        }\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testMutateIfElse() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (isA()) {\n"
                + "            return 1;\n"
                + "        } else {\n"
                + "            return 2;\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (isA()) {\n"
                + "            return 2;\n"
                + "        } else {\n"
                + "            return 1;\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testMutateIfElsifElse() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (isA()) {\n"
                + "            return 1;\n"
                + "        } else if (isB()) {\n"
                + "            return 2;\n"
                + "        } else {\n"
                + "            return 3;\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		1,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (!isA()) {\n"
                + "            return 1;\n"
                + "        } else if (isB()) {\n"
                + "            return 2;\n"
                + "        } else {\n"
                + "            return 3;\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        if (isA()) {\n"
                + "            return 1;\n"
                + "        } else if (isB()) {\n"
                + "            return 3;\n"
                + "        } else {\n"
                + "            return 2;\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 2);
    }

    @Test
    public void testSwapIsApplicableForComparison() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public int a(int b) {\n"
                + "        if (b == 5) {\n"
                + "            return 1;\n"
                + "        } else {\n"
                + "            return 2;\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a(int b) {\n"
                + "        if (b == 5) {\n"
                + "            return 2;\n"
                + "        } else {\n"
                + "            return 1;\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testSimpleComparisonInvertIsNotApplicable() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public int a(int b) {\n"
                + "        if (b == 5) {\n"
                + "            return 1;\n"
                + "        }\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, 0);
    }

}
