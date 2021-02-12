package de.set.trainingUI.generators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class RemoveUnusedVariableOptimizationTest {

	private static void check(String input, String expected) {
		final CompilationUnit cu = StaticJavaParser.parse(input);
		final CodeOptimization opt = new RemoveUnusedVariableOptimization();
		CodeOptimization.optimizeUntilSteadyState(cu, opt);
		final String actual = cu.toString();
		assertEquals(expected, actual);
	}

	@Test
	public void testUsedVariableIsLeftUnchanged() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public void bar() {\n" +
			"        int a = x();\n" +
			"        System.out.println(a);\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public void bar() {\n" +
			"        int a = x();\n" +
			"        System.out.println(a);\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testUnusedVariableIsRemovedButSideEffectIsKept() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public void bar() {\n" +
			"        int a = x();\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public void bar() {\n" +
			"        x();\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testUnusedVariableIsRemovedButLiteralAssignmentIsNotKept() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public void bar() {\n" +
			"        int a = 5;\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public void bar() {\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testUnusedVariableIsRemovedButSideEffectFreeAssignmentIsNotKept() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public void bar(int x) {\n" +
			"        int a = x * 10;\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public void bar(int x) {\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testUnusedVariableDeclarationIsRemoved() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public void bar(int x) {\n" +
			"        int a;\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public void bar(int x) {\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testRecursiveRemovalUntilSteadyState() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public void bar(int x) {\n" +
			"        int a = x;\n" +
			"        int b = 2 * a;\n" +
			"        int c = 3 - b;\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public void bar(int x) {\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testUnusedAttributeIsNotRemoved() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public int a;\n" +
			"\n" +
			"    public void bar(int x) {\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public int a;\n" +
			"\n" +
			"    public void bar(int x) {\n" +
			"    }\n" +
			"}\n");
	}

	@Test
	public void testWithMultipleVariables() {
		check(
			"class Foo {\n" +
			"\n" +
			"    public void bar() {\n" +
			"        int a = 5;\n" +
			"        int b = 6;\n" +
			"        int c = 7;\n" +
			"        System.out.println(a);\n" +
			"        System.out.println(c);\n" +
			"    }\n" +
			"}\n",
			"class Foo {\n" +
			"\n" +
			"    public void bar() {\n" +
			"        int a = 5;\n" +
			"        int c = 7;\n" +
			"        System.out.println(a);\n" +
			"        System.out.println(c);\n" +
			"    }\n" +
			"}\n");
	}

}
