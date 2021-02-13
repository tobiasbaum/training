package de.set.trainingUI.generators;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import de.set.trainingUI.generators.LineMap.LineMapBuilder;

public class LineMapTest {

	private static CompilationUnit parse(String s) {
		return StaticJavaParser.parse(s);
	}

	private static void deleteStatement(CompilationUnit cu, String string) {
		cu.accept(new VoidVisitorAdapter<Void>() {
			@Override
			public void visit(BlockStmt s, Void arg) {
				for (final Statement child : new ArrayList<>(s.getStatements())) {
					if (containsLiteral(child, string)) {
						child.remove();
					}
				}
			}
		}, null);
	}

	private static boolean containsLiteral(Statement child, String string) {
		final Boolean ret = child.accept(new GenericVisitorAdapter<Boolean, Void>() {
			@Override
			public Boolean visit(StringLiteralExpr s, Void arg) {
				if (s.getValue().equals(string)) {
					return Boolean.TRUE;
				}
				return null;
			}
		}, null);
		return ret != null && ret.booleanValue();
	}

	@Test
	public void testIdentity() {
		final LineMap m = LineMap.identity();
		assertEquals(Integer.valueOf(1), m.mapLine(1));
		assertEquals(Integer.valueOf(2), m.mapLine(2));
		assertEquals(Integer.valueOf(3), m.mapLine(3));
	}

	@Test
	public void testWithUnchangedCode() {
		final CompilationUnit cu = parse(
				"class A {\n"
				+ "    public void foo() {\n"
				+ "        System.out.println(\"a\");\n"
				+ "        System.out.println(\"b\");\n"
				+ "        System.out.println(\"c\");\n"
				+ "    }\n"
				+ "}\n");
		final LineMapBuilder b = LineMap.buildFrom(cu);
		final LineMap m = b.snapshotNewCode();
		assertEquals(Integer.valueOf(1), m.mapLine(1));
		assertEquals(Integer.valueOf(2), m.mapLine(2));
		assertEquals(Integer.valueOf(3), m.mapLine(3));
		assertEquals(Integer.valueOf(4), m.mapLine(4));
		assertEquals(Integer.valueOf(5), m.mapLine(5));
		assertEquals(Integer.valueOf(6), m.mapLine(6));
		assertEquals(Integer.valueOf(7), m.mapLine(7));
	}

	@Test
	public void testDeleteSingleLine() {
		final CompilationUnit cu = parse(
				"class A {\n"
				+ "    public void foo() {\n"
				+ "        System.out.println(\"a\");\n"
				+ "        System.out.println(\"b\");\n"
				+ "        System.out.println(\"c\");\n"
				+ "    }\n"
				+ "}\n");
		final LineMapBuilder b = LineMap.buildFrom(cu);
		deleteStatement(cu, "b");
		final LineMap m = b.snapshotNewCode();
		assertEquals(Integer.valueOf(1), m.mapLine(1));
		assertEquals(Integer.valueOf(2), m.mapLine(2));
		assertEquals(Integer.valueOf(3), m.mapLine(3));
		assertEquals(Integer.valueOf(3), m.mapLine(4));
		assertEquals(Integer.valueOf(4), m.mapLine(5));
		assertEquals(Integer.valueOf(5), m.mapLine(6));
		assertEquals(Integer.valueOf(6), m.mapLine(7));
	}

	@Test
	public void testDeleteTwoSeparatedLines() {
		final CompilationUnit cu = parse(
				"class A {\n"
				+ "    public void foo() {\n"
				+ "        System.out.println(\"a\");\n"
				+ "        System.out.println(\"b\");\n"
				+ "        System.out.println(\"c\");\n"
				+ "    }\n"
				+ "}\n");
		final LineMapBuilder b = LineMap.buildFrom(cu);
		deleteStatement(cu, "a");
		deleteStatement(cu, "c");
		final LineMap m = b.snapshotNewCode();
		assertEquals(Integer.valueOf(1), m.mapLine(1));
		assertEquals(Integer.valueOf(2), m.mapLine(2));
		assertEquals(Integer.valueOf(2), m.mapLine(3));
		assertEquals(Integer.valueOf(3), m.mapLine(4));
		assertEquals(Integer.valueOf(3), m.mapLine(5));
		assertEquals(Integer.valueOf(4), m.mapLine(6));
		assertEquals(Integer.valueOf(5), m.mapLine(7));
	}

	@Test
	public void testDeleteTwoAdjacentLines() {
		final CompilationUnit cu = parse(
				"class A {\n"
				+ "    public void foo() {\n"
				+ "        System.out.println(\"a\");\n"
				+ "        System.out.println(\"b\");\n"
				+ "        System.out.println(\"c\");\n"
				+ "    }\n"
				+ "}\n");
		final LineMapBuilder b = LineMap.buildFrom(cu);
		deleteStatement(cu, "a");
		deleteStatement(cu, "b");
		final LineMap m = b.snapshotNewCode();
		assertEquals(Integer.valueOf(1), m.mapLine(1));
		assertEquals(Integer.valueOf(2), m.mapLine(2));
		assertEquals(Integer.valueOf(2), m.mapLine(3));
		assertEquals(Integer.valueOf(2), m.mapLine(4));
		assertEquals(Integer.valueOf(3), m.mapLine(5));
		assertEquals(Integer.valueOf(4), m.mapLine(6));
		assertEquals(Integer.valueOf(5), m.mapLine(7));
	}

	@Test
	public void testDeleteThreeLines() {
		final CompilationUnit cu = parse(
				"class A {\n"
				+ "    public void foo() {\n"
				+ "        System.out.println(\"a\");\n"
				+ "        System.out.println(\"b\");\n"
				+ "        System.out.println(\"c\");\n"
				+ "    }\n"
				+ "}\n");
		final LineMapBuilder b = LineMap.buildFrom(cu);
		deleteStatement(cu, "a");
		deleteStatement(cu, "b");
		deleteStatement(cu, "c");
		final LineMap m = b.snapshotNewCode();
		assertEquals(Integer.valueOf(1), m.mapLine(1));
		assertEquals(Integer.valueOf(2), m.mapLine(2));
		assertEquals(Integer.valueOf(2), m.mapLine(3));
		assertEquals(Integer.valueOf(2), m.mapLine(4));
		assertEquals(Integer.valueOf(2), m.mapLine(5));
		assertEquals(Integer.valueOf(3), m.mapLine(6));
		assertEquals(Integer.valueOf(4), m.mapLine(7));
	}

	@Test
	public void testDeleteWithComments() {
		final CompilationUnit cu = parse(
				"class A {\n"
				+ "    public void foo() {\n"
				+ "        // Kommentar 1\n"
				+ "        System.out.println(\"a\");\n"
				+ "        // Kommentar 2\n"
				+ "        System.out.println(\"b\");\n"
				+ "        // Kommentar 3\n"
				+ "        System.out.println(\"c\");\n"
				+ "    }\n"
				+ "}\n");
		final LineMapBuilder b = LineMap.buildFrom(cu);
		deleteStatement(cu, "a");
		deleteStatement(cu, "b");
		deleteStatement(cu, "c");
		final LineMap m = b.snapshotNewCode();
		assertEquals(Integer.valueOf(1), m.mapLine(1));
		assertEquals(Integer.valueOf(2), m.mapLine(2));
		assertEquals(Integer.valueOf(3), m.mapLine(3));
		assertEquals(Integer.valueOf(3), m.mapLine(4));
		assertEquals(Integer.valueOf(4), m.mapLine(5));
		assertEquals(Integer.valueOf(4), m.mapLine(6));
		assertEquals(Integer.valueOf(5), m.mapLine(7));
		assertEquals(Integer.valueOf(5), m.mapLine(8));
		assertEquals(Integer.valueOf(6), m.mapLine(9));
		assertEquals(Integer.valueOf(7), m.mapLine(10));
	}

}
