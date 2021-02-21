package de.set.trainingUI.generators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class RemoveUnnecessaryParenthesesOptimizationTest {

	private static void check(String input, String expected) {
		final CompilationUnit cu = StaticJavaParser.parse(input);
		final CodeOptimization opt = new RemoveUnnecessaryParenthesesOptimization();
		CodeOptimization.optimizeUntilSteadyState(cu, opt);
		final String actual = cu.toString();
		assertEquals(expected, actual);
	}

	@Test
	public void testRemoveAroundLiteral() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public int bar() {\n" +
			"        return (5);\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public int bar() {\n" +
			"        return 5;\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testRemoveAroundVariable() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public int bar(int x) {\n" +
			"        return (x);\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public int bar(int x) {\n" +
			"        return x;\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testRemoveAroundAndInMethodCall() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public int bar(int x) {\n" +
			"        return (foo((x), (4.0), 7));\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public int bar(int x) {\n" +
			"        return foo(x, 4.0, 7);\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testRemoveInIf() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public int bar(int x) {\n" +
			"        if ((x == 5)) {\n" +
			"            return 3;\n" +
			"        }\n" +
			"        return 6;\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public int bar(int x) {\n" +
			"        if (x == 5) {\n" +
			"            return 3;\n" +
			"        }\n" +
			"        return 6;\n" +
			"    }\n" +
			"}\n");
	}

}
