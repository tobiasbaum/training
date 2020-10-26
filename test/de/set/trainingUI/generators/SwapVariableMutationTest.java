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

import de.set.trainingUI.generators.SwapVariableExpressionMutation;
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
        assertEquals(Arrays.asList("b,4", "System,4"),
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
        assertEquals(Arrays.asList("b,5", "c,5", "System,5"),
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
        assertEquals(Arrays.asList("j,6", "i,7", "j,5", "j,5"),
                findApplicable(u, data));
        assertEquals(Arrays.asList("i,4", "i,9", "i,3", "i,3"),
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
        assertEquals("5;OTHER_ALGORITHMIC_PROBLEM;.+", p.getProperty("remark.42.pattern"));
        assertEquals("5;OTHER_ALGORITHMIC_PROBLEM;die Variable b muss statt c verwendet werden", p.getProperty("remark.42.example"));
        assertEquals(5, mutation.getAnchorLine());
    }

}
