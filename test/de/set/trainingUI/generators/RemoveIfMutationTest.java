package de.set.trainingUI.generators;

import org.junit.Test;

public class RemoveIfMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveStatementMutationTest.checkMutationCount(input, RemoveIfMutation.class, expectedCount);
	}

	private static void checkMutation(String input, int i, String expected) {
		RemoveStatementMutationTest.checkMutation(input, RemoveIfMutation.class, i, expected);
	}

	private static void checkMutation(String input, int i, long seed, String expected) {
		RemoveStatementMutationTest.checkMutation(input, RemoveIfMutation.class, i, seed, expected);
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
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        System.out.println(\"y\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testRemoveWithElse1() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        } else {\n"
                + "            System.out.println(\"z\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"y\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testRemoveWithElse2() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        } else {\n"
                + "            System.out.println(\"z\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		-1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"z\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testRemoveWithIfElse() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"x\");\n"
                + "        } else if (System.out != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        } else {\n"
                + "            System.out.println(\"z\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
        		123,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"x\");\n"
                + "        } else {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		0,
        		-1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"x\");\n"
                + "        } else {\n"
                + "            System.out.println(\"z\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		1,
        		123,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		1,
        		-1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.out != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        } else {\n"
                + "            System.out.println(\"z\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 2);
    }

}
