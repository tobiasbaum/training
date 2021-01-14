package de.set.trainingUI.generators;

import org.junit.Test;

public class MoveOutOfIfMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveStatementMutationTest.checkMutationCount(input, MoveOutOfIfMutation.class, expectedCount);
	}

	private static void checkMutation(String input, int i, long seed, String expected) {
		RemoveStatementMutationTest.checkMutation(input, MoveOutOfIfMutation.class, i, seed, expected);
	}

    @Test
    public void testSimpleMove() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "            System.out.println(\"z\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        System.out.println(\"y\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"z\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testSimpleMoveBehind() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "            System.out.println(\"z\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		44,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "        System.out.println(\"z\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testMoveMultiple() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "            System.out.println(\"3\");\n"
                + "            System.out.println(\"4\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        System.out.println(\"2\");\n"
                + "        System.out.println(\"3\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"4\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		0,
        		-1,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        System.out.println(\"2\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"3\");\n"
                + "            System.out.println(\"4\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testMoveMultipleBehind() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "            System.out.println(\"3\");\n"
                + "            System.out.println(\"4\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		44,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "        }\n"
                + "        System.out.println(\"3\");\n"
                + "        System.out.println(\"4\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testMoveOutOfElse() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "        } else {\n"
                + "            System.out.println(\"3\");\n"
                + "            System.out.println(\"4\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        System.out.println(\"3\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "        } else {\n"
                + "            System.out.println(\"4\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testMoveOutOfNestedIf() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "        } else if (System.err != null) {\n"
                + "            System.out.println(\"3\");\n"
                + "            System.out.println(\"4\");\n"
                + "        } else {\n"
                + "            System.out.println(\"5\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        System.out.println(\"3\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "        } else if (System.err != null) {\n"
                + "            System.out.println(\"4\");\n"
                + "        } else {\n"
                + "            System.out.println(\"5\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testMoveOutOfIfOrElse() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "            System.out.println(\"3\");\n"
                + "        } else {\n"
                + "            System.out.println(\"4\");\n"
                + "            System.out.println(\"5\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "        } else {\n"
                + "            System.out.println(\"4\");\n"
                + "            System.out.println(\"5\");\n"
                + "        }\n"
                + "        System.out.println(\"3\");\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		0,
        		-1,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        System.out.println(\"1\");\n"
                + "        System.out.println(\"4\");\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"2\");\n"
                + "            System.out.println(\"3\");\n"
                + "        } else {\n"
                + "            System.out.println(\"5\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

}
