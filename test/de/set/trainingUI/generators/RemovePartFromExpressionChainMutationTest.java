package de.set.trainingUI.generators;

import org.junit.Test;

public class RemovePartFromExpressionChainMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveMethodCallMutationTest.checkMutationCount(input, RemovePartFromExpressionChainMutation.class, expectedCount);
	}

	private static void checkMutation(String input, int i, String expected) {
		RemoveMethodCallMutationTest.checkMutation(input, RemovePartFromExpressionChainMutation.class, i, expected);
	}

    @Test
    public void testRemoveMethodCall() {
        final String input =
                "class A {\n"
                + "    public String a(String x) {\n"
                + "        return x\n"
                + "            .replace(\"a\", \"A\")\n"
                + "            .replace(\"b\", \"B\")\n"
                + "            .replace(\"c\", \"C\");\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public String a(String x) {\n"
                + "        return x.replace(\"b\", \"B\").replace(\"c\", \"C\");\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		1,
                "class A {\n"
                + "\n"
                + "    public String a(String x) {\n"
                + "        return x.replace(\"a\", \"A\").replace(\"c\", \"C\");\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		2,
                "class A {\n"
                + "\n"
                + "    public String a(String x) {\n"
                + "        return x.replace(\"a\", \"A\").replace(\"b\", \"B\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 3);
    }

    @Test
    public void testSimpleCallsAreNotRemoved() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        return this.hashCode();\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, 0);
    }

}
