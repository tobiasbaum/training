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
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import de.set.trainingUI.generators.SwapCalledMethodMutation.SwapMethodData;

@SuppressWarnings("nls")
public class SwapCalledMethodsMutationTest {

    private static CompilationUnit parse(final String fileContent) {
        return StaticJavaParser.parse(fileContent);
    }

    private static void findApplicableAndNonApplicable(
            final CompilationUnit ast,
            final SwapMethodData data,
            final List<MethodCallExpr> applBuffer,
            final List<MethodCallExpr> nonApplBuffer) {
        ast.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(final MethodCallExpr n, final Void v) {
                super.visit(n, v);
                if (data.isApplicable(n)) {
                	applBuffer.add(n);
                } else {
                	nonApplBuffer.add(n);
                }
            }
        }, null);
    }

    private static List<String> toStrings(final List<MethodCallExpr> expr) {
        final List<String> ret = new ArrayList<>();
        for (final MethodCallExpr e : expr) {
            ret.add(e.getNameAsString() + "," + e.getRange().get().begin.line);
        }
        return ret;
    }

    private static List<String> findApplicable(final CompilationUnit ast, final SwapMethodData data) {
        final List<MethodCallExpr> ret = new ArrayList<>();
        findApplicableAndNonApplicable(ast, data, ret, new ArrayList<>());
        return toStrings(ret);
    }

    private static List<String> findNonApplicable(final CompilationUnit ast, final SwapMethodData data) {
        final List<MethodCallExpr> ret = new ArrayList<>();
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

        final SwapMethodData data = SwapCalledMethodMutation.analyze(u);
        assertNotNull(data);
        assertEquals(Collections.emptyList(), findApplicable(u, data));
        assertEquals(Collections.emptyList(), findNonApplicable(u, data));
    }

    @Test
    public void testApplicable() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    private void foo(int x) {\n"
                + "    }\n"
                + "    private void bar(int y) {\n"
                + "    }\n"
                + "    public void a() {\n"
                + "        this.foo(5);\n"
                + "    }\n"
                + "}\n");

        final SwapMethodData data = SwapCalledMethodMutation.analyze(u);
        assertNotNull(data);
        assertEquals(Collections.singletonList("foo,7"), findApplicable(u, data));
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

        final SwapMethodData data = SwapCalledMethodMutation.analyze(u);
        assertEquals(Collections.emptyList(),
                findApplicable(u, data));
        assertEquals(Arrays.asList("println,4"),
                findNonApplicable(u, data));
    }

    @Test
    public void testMutation() {
        final CompilationUnit u = parse(
                "class A {\n"
                + "    private void p1() {\n"
                + "    }\n"
                + "    private void p2() {\n"
                + "    }\n"
                + "    public void a(int x) {\n"
                + "        if (x == 2) {\n"
                + "            this.p1();\n"
                + "        } else {\n"
                + "            this.p2();\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        final SwapMethodData data = SwapCalledMethodMutation.analyze(u);
        final List<MethodCallExpr> applicable = new ArrayList<>();
        findApplicableAndNonApplicable(u, data, applicable, new ArrayList<>());
        final SwapCalledMethodMutation mutation =
                new SwapCalledMethodMutation(data, applicable.get(0));
        mutation.apply(new Random(123));
        final Properties p = new Properties();
        mutation.createRemark(42, new RemarkCreator(p));
        assertEquals("10;OTHER_ALGORITHMIC_PROBLEM;.+", p.getProperty("remark.42.pattern"));
        assertEquals("10;OTHER_ALGORITHMIC_PROBLEM;die Methode p2 muss statt p1 verwendet werden", p.getProperty("remark.42.example"));
        assertEquals(10, mutation.getAnchorLine());
    }

}
