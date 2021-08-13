package de.set.trainingUI.generators;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.junit.Test;

public class RemoveBinaryExprMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveStatementMutationTest.checkMutationCount(input, RemoveBinaryExprMutation.class, expectedCount);
	}

	private static void checkMutations(String input, int i, String... expecteds) {
		RemoveStatementMutationTest.checkMutations(input, RemoveBinaryExprMutation.class, i, expecteds);
	}

	private static List<RemoveBinaryExprMutation> determineMutations(String input) {
		return RemoveStatementMutationTest.determineApplicableMutations(input, RemoveBinaryExprMutation.class);
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

    @Test
    public void testWithStringLiteralCountsAsWrongMessage() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public void a(double d) {\n"
                + "        System.out.println(\"test \" + d);\n"
                + "    }\n"
                + "}\n";

        final List<RemoveBinaryExprMutation> list = determineMutations(input);
        final RemoveBinaryExprMutation mutation = list.get(0);
        mutation.apply(new Random(123));
        assertEquals(4, mutation.getAnchorLine());
        final Properties p = new Properties();
        mutation.createRemark(42, new RemarkCreator(p, LineMap.identity()));
        assertEquals("4;WRONG_MESSAGE,MISSING_CODE;.+", p.getProperty("remark.42.pattern"));
        assertEquals("4;WRONG_MESSAGE;müsste \"test \" + d sein", p.getProperty("remark.42.example"));
    }

    @Test
    public void testWithNumbersCountsAsWrongCalculation() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public double a(double d) {\n"
                + "        return 2 + d;\n"
                + "    }\n"
                + "}\n";

        final List<RemoveBinaryExprMutation> list = determineMutations(input);
        final RemoveBinaryExprMutation mutation = list.get(0);
        mutation.apply(new Random(123));
        assertEquals(4, mutation.getAnchorLine());
        final Properties p = new Properties();
        mutation.createRemark(42, new RemarkCreator(p, LineMap.identity()));
        assertEquals("4;WRONG_CALCULATION,MISSING_CODE;.+", p.getProperty("remark.42.pattern"));
        assertEquals("4;WRONG_CALCULATION;müsste 2 + d sein", p.getProperty("remark.42.example"));
    }
}
