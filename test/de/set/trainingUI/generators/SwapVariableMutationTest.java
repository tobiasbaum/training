package de.set.trainingUI.generators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.junit.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import de.set.trainingUI.generators.SwapVariableExpressionMutation.SwapVariableData;

@SuppressWarnings("nls")
public class SwapVariableMutationTest {

    private static CompilationUnit parse(final String fileContent) {
        return StaticJavaParser.parse(fileContent);
    }

    private static void findApplicableAndNonApplicable(
            final CompilationUnit ast,
            final SwapVariableData data,
            final List<NameExpr> applBuffer,
            final List<NameExpr> nonApplBuffer) {
        ast.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(final NameExpr n, final Void v) {
                super.visit(n, v);
                if (data.isApplicable(n)) {
                    applBuffer.add(n);
                } else {
                    nonApplBuffer.add(n);
                }
            }
        }, null);
    }

    private static List<String> toStrings(final List<NameExpr> expr) {
        final List<String> ret = new ArrayList<>();
        Collections.sort(expr, (NameExpr e1, NameExpr e2) -> {
        	final int cmp = Integer.compare(e1.getRange().get().begin.line, e2.getRange().get().begin.line);
        	if (cmp != 0) {
        		return cmp;
        	}
        	return e1.getNameAsString().compareTo(e2.getNameAsString());
        });
        for (final NameExpr e : expr) {
            ret.add(e.getNameAsString() + "," + e.getRange().get().begin.line);
        }
        return ret;
    }

    private static List<String> findApplicable(final CompilationUnit ast, final SwapVariableData data) {
        final List<NameExpr> ret = new ArrayList<>();
        findApplicableAndNonApplicable(ast, data, ret, new ArrayList<>());
        return toStrings(ret);
    }

    private static List<String> findNonApplicable(final CompilationUnit ast, final SwapVariableData data) {
        final List<NameExpr> ret = new ArrayList<>();
        findApplicableAndNonApplicable(ast, data, new ArrayList<>(), ret);
        return toStrings(ret);
    }

    @Test
    public void testEmptyMethod() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a() {\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertNotNull(data);
        assertEquals(Collections.emptyList(), findApplicable(u, data));
        assertEquals(Collections.emptyList(), findNonApplicable(u, data));
    }

    @Test
    public void testNonApplicable() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a() {\n"
                + "        int b = 5;\n"
                + "        System.out.println(b);\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Collections.emptyList(),
                findApplicable(u, data));
        assertEquals(Arrays.asList("System,4", "b,4"),
                findNonApplicable(u, data));
    }

    @Test
    public void testApplicable() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a() {\n"
                + "        int b = 5;\n"
                + "        int c = 3;\n"
                + "        System.out.println(b / c);\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Arrays.asList("b,5", "c,5"),
                findApplicable(u, data));
        assertEquals(Arrays.asList("System,5"),
                findNonApplicable(u, data));
    }

    @Test
    public void testApplicableWithParameters() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a(int b, int c) {\n"
                + "        System.out.println(b / c);\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Arrays.asList("b,3", "c,3"),
                findApplicable(u, data));
        assertEquals(Arrays.asList("System,3"),
                findNonApplicable(u, data));
    }

    @Test
    public void testApplicableWithAttributes() {
        final CompilationUnit u = parse(
                "class A {\n"
        		+ "    public int b;\n"
        		+ "    public int c;\n"
                + "    public void a() {\n"
                + "        System.out.println(b / c);\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Arrays.asList("b,5", "c,5"),
                findApplicable(u, data));
        assertEquals(Arrays.asList("System,5"),
                findNonApplicable(u, data));
    }

    @Test
    public void testApplicableWithAttributesAtEnd() {
        final CompilationUnit u = parse(
                "class A {\n"
        		+ "    public void a() {\n"
        		+ "        System.out.println(b / c);\n"
        		+ "    }\n"
        		+ "    public int b;\n"
        		+ "    public int c;\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Arrays.asList("b,3", "c,3"),
                findApplicable(u, data));
        assertEquals(Arrays.asList("System,3"),
                findNonApplicable(u, data));
    }

    @Test
    public void testNotApplicableWithAttributes() {
        final CompilationUnit u = parse(
                "class A {\n"
        		+ "    public int b;\n"
                + "    public void a(int b) {\n"
                + "        System.out.println(this.b / b);\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Collections.emptyList(),
                findApplicable(u, data));
        assertEquals(Arrays.asList("System,4", "b,4"),
                findNonApplicable(u, data));
    }

    @Test
    public void testNotApplicableWithNestedAttributes() {
        final CompilationUnit u = parse(
                "class A {\n"
        		+ "    public int b;\n"
        		+ "    class Sub {\n"
        		+ "        public int b;\n"
                + "        public void a() {\n"
                + "            System.out.println(b);\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Collections.emptyList(),
                findApplicable(u, data));
        assertEquals(Arrays.asList("System,6", "b,6"),
                findNonApplicable(u, data));
    }

    @Test
    public void testDifferentTypes() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a() {\n"
                + "        int b = 5;\n"
                + "        double c = 3;\n"
                + "        System.out.println(b / c);\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Collections.emptyList(),
                findApplicable(u, data));
        assertEquals(Arrays.asList("System,5", "b,5", "c,5"),
                findNonApplicable(u, data));
    }

    @Test
    public void testScopeInMethod() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a() {\n"
                + "        int b = 5;\n"
                + "        int c = b;\n"
                + "        System.out.println(b / c);\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Arrays.asList("b,5", "c,5"),
                findApplicable(u, data));
        assertEquals(Arrays.asList("b,4", "System,5"),
                findNonApplicable(u, data));
    }

    @Test
    public void testScopeWithNestedLoops() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a() {\n"
                + "        for (int i = 0; i < 10; i++) {\n"
                + "            doStuff(i);\n"
                + "            for (int j = 0; j < 5; j++) {\n"
                + "                doStuff(j);\n"
                + "                doStuff(i);\n"
                + "            }\n"
                + "            doStuff(i);\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Arrays.asList("j,5", "j,5", "j,6", "i,7"),
                findApplicable(u, data));
        assertEquals(Arrays.asList("i,3", "i,3", "i,4", "i,9"),
                findNonApplicable(u, data));
    }

    @Test
    public void testMutation() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a() {\n"
                + "        int b = 5;\n"
                + "        int c = 3;\n"
                + "        System.out.println(b / c);\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        final List<NameExpr> applicable = new ArrayList<>();
        findApplicableAndNonApplicable(u, data, applicable, new ArrayList<>());
        final SwapVariableExpressionMutation mutation =
                new SwapVariableExpressionMutation(data, applicable.get(0));
        mutation.apply(new Random(123));
        final Properties p = new Properties();
        mutation.createRemark(42, p);
        assertEquals("5;WRONG_CALCULATION,OTHER_ALGORITHMIC_PROBLEM;.+", p.getProperty("remark.42.pattern"));
        assertEquals("5;WRONG_CALCULATION;die Variable b muss statt c verwendet werden", p.getProperty("remark.42.example"));
        assertEquals(5, mutation.getAnchorLine());
    }

    @Test
    public void testMutationInIfHasToTypes() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public void a(boolean a, boolean b) {\n"
                + "        if (a) {\n"
                + "        	System.out.println(\"x\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        final List<NameExpr> applicable = new ArrayList<>();
        findApplicableAndNonApplicable(u, data, applicable, new ArrayList<>());
        final SwapVariableExpressionMutation mutation =
                new SwapVariableExpressionMutation(data, applicable.get(0));
        mutation.apply(new Random(123));
        final Properties p = new Properties();
        mutation.createRemark(42, p);
        assertEquals("3;WRONG_COMPARISON,OTHER_ALGORITHMIC_PROBLEM;.+", p.getProperty("remark.42.pattern"));
        assertEquals("3;WRONG_COMPARISON;die Variable a muss statt b verwendet werden", p.getProperty("remark.42.example"));
        assertEquals(3, mutation.getAnchorLine());
    }

    @Test
    public void testMutationInConstructor() {
        for (int seed = 0; seed < 100; seed++) {
            final CompilationUnit u = parse(
                    "class A {\n"
                    + "    private double a;\n"
                    + "    private double b;\n"
                    + "    public A(double a, double b) {\n"
                    + "        this.a = a;\n"
                    + "        this.b = b;\n"
                    + "    }\n"
                    + "}\n");

	        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
	        final List<NameExpr> applicable = new ArrayList<>();
	        findApplicableAndNonApplicable(u, data, applicable, new ArrayList<>());
	        final SwapVariableExpressionMutation mutation1 =
	                new SwapVariableExpressionMutation(data, applicable.get(0));
	        mutation1.apply(new Random(seed));
	        final Properties p1 = new Properties();
	        mutation1.createRemark(42, p1);
	        assertEquals("with seed " + seed, "5;OTHER_ALGORITHMIC_PROBLEM;.+", p1.getProperty("remark.42.pattern"));
	        assertEquals("with seed " + seed, "5;OTHER_ALGORITHMIC_PROBLEM;die Variable a muss statt b verwendet werden", p1.getProperty("remark.42.example"));
	        assertEquals("with seed " + seed, 5, mutation1.getAnchorLine());

	        final SwapVariableExpressionMutation mutation2 =
	                new SwapVariableExpressionMutation(data, applicable.get(1));
	        mutation2.apply(new Random(seed));
	        final Properties p2 = new Properties();
	        mutation2.createRemark(23, p2);
	        assertEquals("with seed " + seed, "6;OTHER_ALGORITHMIC_PROBLEM;.+", p2.getProperty("remark.23.pattern"));
	        assertEquals("with seed " + seed, "6;OTHER_ALGORITHMIC_PROBLEM;die Variable b muss statt a verwendet werden", p2.getProperty("remark.23.example"));
	        assertEquals("with seed " + seed, 6, mutation2.getAnchorLine());
        }
    }

    @Test
    public void testMutationIsNotMisledByAttributesInOtherClasses() {
        final CompilationUnit u = parse(
                "class Envelope {\n"
                + "    private final double thickness;\n"
                + "}\n"
                + "\n"
                + "class A {\n"
                + "    public double getThickness() {\n"
                + "        final Envelope env = this.envelope == null ? new Envelope(0.0, 1.0) : this.envelope;\n"
                + "        double thickness = env.getThickness();\n"
                + "        for (final Sheet s : this.sheets) {\n"
                + "            thickness += s.getThickness() * env.getFoldingFactorFor(s.getSheetType());\n"
                + "        }\n"
                + "        return thickness;\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Collections.emptyList(),
                findApplicable(u, data));
        assertEquals(Arrays.asList("env,8", "env,10", "s,10", "s,10", "thickness,10", "thickness,12"),
                findNonApplicable(u, data));
    }

    @Test
    public void testMutationIsNotMisledByMultipleLoops() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    public static Map<String, String> createToDuplexMap(List<MediumMap> mediumMaps) {\n"
                + "        final Map<String, String> ret = new LinkedHashMap<>();\n"
                + "        final List<MediumMap> nonDuplex = new ArrayList<>();\n"
                + "        for (final MediumMap m : mediumMaps) {\n"
                + "            if (m.isDuplex()) {\n"
                + "                ret.put(m.getName(), m.getName());\n"
                + "            } else {\n"
                + "                nonDuplex.add(m);\n"
                + "            }\n"
                + "        }\n"
                + "        for (final MediumMap m : nonDuplex) {\n"
                + "            ret.put(m.getName(), \"x\");\n"
                + "        }\n"
                + "        return ret;\n"
                + "    }\n"
                + "}\n");

        final SwapVariableData data = SwapVariableExpressionMutation.analyze(u);
        assertEquals(Arrays.asList("mediumMaps,5", "nonDuplex,9", "nonDuplex,12"),
                findApplicable(u, data));
        assertEquals(Arrays.asList("m,6", "m,7", "m,7", "ret,7", "m,9", "m,13", "ret,13", "ret,15"),
                findNonApplicable(u, data));
    }

}
