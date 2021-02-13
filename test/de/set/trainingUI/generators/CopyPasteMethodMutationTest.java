package de.set.trainingUI.generators;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.junit.Test;

public class CopyPasteMethodMutationTest {

	private static void checkMutationCount(String input, int expectedCount) {
		RemoveStatementMutationTest.checkMutationCount(input, CopyPasteMethodMutation.class, expectedCount);
	}

	private static void checkMutations(String input, int i, String... expecteds) {
		RemoveStatementMutationTest.checkMutations(input, CopyPasteMethodMutation.class, i, expecteds);
	}

	private static List<CopyPasteMethodMutation> determineMutations(String input) {
		return RemoveStatementMutationTest.determineApplicableMutations(input, CopyPasteMethodMutation.class);
	}

    @Test
    public void testMutate() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        return 1;\n"
                + "    }\n"
                + "    public int b() {\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n";

        checkMutations(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        return 2;\n"
                + "    }\n"
                + "\n"
                + "    public int b() {\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n");
        checkMutations(input,
        		1,
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        return 1;\n"
                + "    }\n"
                + "\n"
                + "    public int b() {\n"
                + "        return 1;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 2);
    }

    @Test
    public void testDontMutateWithSameEmptyBody() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "    }\n"
                + "    public void b() {\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, 0);
    }

    @Test
    public void testMutateLessParameters() {
        final String input =
                "class A {\n"
                + "    public int a(int x) {\n"
                + "        return x;\n"
                + "    }\n"
                + "    public int b() {\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n";

        checkMutations(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public int a(int x) {\n"
                + "        return 2;\n"
                + "    }\n"
                + "\n"
                + "    public int b() {\n"
                + "        return 2;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 1);
    }

    @Test
    public void testMutateParametersOK() {
        final String input =
                "class A {\n"
                + "    public String a(int x, char y) {\n"
                + "        return Integer.toString(x + y);\n"
                + "    }\n"
                + "    public String b(int x, char y) {\n"
                + "        return Integer.toString(x - y);\n"
                + "    }\n"
                + "}\n";

        checkMutations(input,
        		0,
                "class A {\n"
                + "\n"
                + "    public String a(int x, char y) {\n"
                + "        return Integer.toString(x - y);\n"
                + "    }\n"
                + "\n"
                + "    public String b(int x, char y) {\n"
                + "        return Integer.toString(x - y);\n"
                + "    }\n"
                + "}\n");
        checkMutations(input,
        		1,
                "class A {\n"
                + "\n"
                + "    public String a(int x, char y) {\n"
                + "        return Integer.toString(x + y);\n"
                + "    }\n"
                + "\n"
                + "    public String b(int x, char y) {\n"
                + "        return Integer.toString(x + y);\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, 2);
    }

    @Test
    public void testPositionAndTypeOfMutation() {
        final String input =
                "class A {\n"
                + "\n"
                + "    public void a(double d) {\n"
                + "        System.out.println(\"1\");\n"
                + "    }\n"
                + "\n"
                + "    public void b() {\n"
                + "        System.out.println(\"2\");\n"
                + "        System.out.println(\"3\");\n"
                + "    }\n"
                + "}\n";

        final List<CopyPasteMethodMutation> list = determineMutations(input);
        final CopyPasteMethodMutation mutation = list.get(0);
        mutation.apply(new Random(123));
        assertEquals(3, mutation.getAnchorLine());
        final Properties p = new Properties();
        mutation.createRemark(42, new RemarkCreator(p, LineMap.identity()));
        assertEquals("3,4,5;OTHER_ALGORITHMIC_PROBLEM,DUPLICATE_CODE;.+", p.getProperty("remark.42.pattern"));
        assertEquals("3;OTHER_ALGORITHMIC_PROBLEM;a ist falsch implementiert. Copy-Paste?", p.getProperty("remark.42.example"));
    }
}
