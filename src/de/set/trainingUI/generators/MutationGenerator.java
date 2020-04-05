package de.set.trainingUI.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import de.set.trainingUI.DefectFindTask.RemarkType;

@SuppressWarnings("nls")
public class MutationGenerator extends Generator {

    private abstract static class Mutation {

        public abstract void apply(Random r);

        public abstract void createRemark(final int nbr, Properties taskProperties);

        protected void setRemark(final int nbr, final Properties p, final Set<Integer> lines, final RemarkType type,
                final String pattern, final String text) {
            assert lines.contains(this.getAnchorLine());
            p.setProperty("remark." + nbr + ".pattern",
                    lines.stream().map((final Integer i) -> i.toString()).collect(Collectors.joining(","))
                    + ";" + type.name()
                    + ";" + pattern);
            p.setProperty("remark." + nbr + ".example",
                    this.getAnchorLine()
                    + ";" + type.name()
                    + ";" + text);
        }

        public abstract int getAnchorLine();

    }

    private final File directory;
    private final Properties values;

    public MutationGenerator(final File template) throws IOException {
        this.directory = template;
        this.values = loadProperties(new File(template, "template.properties"));
    }

    @Override
    protected Properties getValues() {
        return this.values;
    }

    private static Properties loadProperties(final File file) throws IOException {
        final Properties p = new Properties();
        try (FileInputStream s = new FileInputStream(file)) {
            p.load(s);
        }
        return p;
    }

    @Override
    public void generate(final File targetDir, final Random rand) throws IOException, NoSuchAlgorithmException {
        final VelocityContext params = new VelocityContext();
        for (final Entry<Object, Object> e : this.values.entrySet()) {
            final String valueSet = e.getValue().toString();
            final List<String> values = Arrays.asList(valueSet.split("\\|"));
            params.put(e.getKey().toString(), values.get(rand.nextInt(values.size())));
        }

        final Properties taskProperties = loadProperties(new File(this.directory, "task.properties"));
        final String s = this.mutateSource(taskProperties, rand);
        final String id = this.hash(s);

        final File dir = new File(targetDir, id);
        dir.mkdir();
        Files.write(dir.toPath().resolve("source"), s.getBytes("UTF-8"));

        try (FileOutputStream out = new FileOutputStream(new File(dir, "task.properties"))) {
            taskProperties.store(out, null);
        }
    }

    private String mutateSource(final Properties taskProperties, final Random rand) throws FileNotFoundException {
        final CompilationUnit ast = this.parseNormalized(new File(this.directory, "source"));
        final List<Mutation> mutations = findPossibleMutations(ast);
        final int count = this.determineCount(rand, mutations.size());
        Collections.shuffle(mutations, rand);
        final List<Mutation> chosen = mutations.subList(0, count);
        this.removeInSameLine(chosen);
        for (final Mutation m : chosen) {
            m.apply(rand);
        }
        int i = 1;
        for (final Mutation m : chosen) {
            m.createRemark(i++, taskProperties);
        }
        return ast.toString();
    }

    private void removeInSameLine(final List<Mutation> chosen) {
        final Set<Integer> usedLines = new HashSet<>();
        final Iterator<Mutation> iter = chosen.iterator();
        while (iter.hasNext()) {
            final int line = iter.next().getAnchorLine();
            if (usedLines.contains(line)) {
                iter.remove();
            } else {
                usedLines.add(line);
            }
        }
    }

    private int determineCount(final Random rand, final int size) {
        if (size <= 1 || rand.nextDouble() < 0.8) {
            return 1;
        }
        if (size <= 2 || rand.nextDouble() < 0.8) {
            return 2;
        }
        return 3;
    }

    private CompilationUnit parseNormalized(final File file) throws FileNotFoundException {
        final CompilationUnit parsed = StaticJavaParser.parse(file);
        //parse twice to normalize line numbers
        return StaticJavaParser.parse(parsed.toString());
    }

    private String generateFile(final String string, final VelocityContext params) {
        final org.apache.velocity.Template t = Velocity.getTemplate(new File(this.directory, string).getPath());

        final StringWriter w = new StringWriter();
        t.merge(params, w);
        return w.toString();
    }

    private String hash(final String s) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] encodedhash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedhash);
    }

    private static String bytesToHex(final byte[] hash) {
        final StringBuffer hexString = new StringBuffer();
        for (final byte element : hash) {
            final String hex = Integer.toHexString(0xff & element);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void main(final String[] args) throws Exception {
        final File template = new File(args[0]);

        final MutationGenerator t = new MutationGenerator(template);
        t.generateMultiple(new File("taskdb2"), new Random(123));
    }

    static List<Mutation> findPossibleMutations(final CompilationUnit ast) {
        final List<Mutation> ret = new ArrayList<>();
        ast.accept(new GenericVisitorAdapter<Void, Void>() {
            @Override
            public Void visit(final IfStmt n, final Void v) {
                super.visit(n, v);
                ret.add(new InvertMutation(n));
                return null;
            }
            @Override
            public Void visit(final ExpressionStmt n, final Void v) {
                super.visit(n, v);
                if (n.getParentNode().get().getChildNodes().size() <= 1) {
                    return null;
                }
                if (!n.getExpression().isMethodCallExpr()) {
                    return null;
                }
                ret.add(new RemoveMutation(n));
                return null;
            }
            @Override
            public Void visit(final BinaryExpr n, final Void v) {
                super.visit(n, v);
                if (FlipOperatorMutation.isApplicable(n)) {
                    ret.add(new FlipOperatorMutation(n));
                }
                return null;
            }
        }, null);
        return ret;
    }

    private static final class InvertMutation extends Mutation {

        private final IfStmt ifStmt;

        public InvertMutation(final IfStmt n) {
            this.ifStmt = n;
        }

        @Override
        public void apply(final Random r) {
            this.ifStmt.setCondition(new UnaryExpr(
                    this.ifStmt.getCondition(),
                    UnaryExpr.Operator.LOGICAL_COMPLEMENT));
        }

        @Override
        public int getAnchorLine() {
            return this.ifStmt.getBegin().get().line;
        }

        @Override
        public void createRemark(final int nbr, final Properties p) {
            final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
            this.setRemark(nbr, p, lines, RemarkType.WRONG_CONDITION, ".+", "die Bedingung muss invertiert werden");
        }

    }

    private static final class RemoveMutation extends Mutation {

        private final Statement n;
        private final Node parent;

        public RemoveMutation(final Statement n) {
            this.n = n;
            this.parent = this.n.getParentNode().get();
        }

        @Override
        public void apply(final Random r) {
            this.n.remove();
        }

        @Override
        public int getAnchorLine() {
            return this.n.getBegin().get().line;
        }

        @Override
        public void createRemark(final int nbr, final Properties p) {
            final Set<Integer> lines = new LinkedHashSet<>();
            final int start = this.parent.getBegin().get().line;
            final int end = this.parent.getEnd().get().line;
            for (int i = start; i < end; i++) {
                lines.add(i);
            }
            this.setRemark(nbr, p, lines, RemarkType.MISSING_CODE, ".+", this.n.toString() + " fehlt");
        }

    }

    private static final class FlipOperatorMutation extends Mutation {

        private final BinaryExpr expr;
        private final String correct;

        public FlipOperatorMutation(final BinaryExpr expr) {
            this.expr = expr;
            this.correct = expr.toString();
        }

        public static boolean isApplicable(final BinaryExpr ex) {
            return ex.getOperator() != flipOperator(ex.getOperator(), new Random(42));
        }

        @Override
        public void apply(final Random r) {
            this.expr.setOperator(flipOperator(this.expr.getOperator(), r));
        }

        private static Operator flipOperator(final Operator operator, final Random r) {
            switch (operator) {
            case PLUS:
            case MINUS:
                return another(r, operator, Operator.MINUS, Operator.PLUS);
            case LESS:
            case LESS_EQUALS:
            case GREATER:
            case GREATER_EQUALS:
                return another(r, operator, Operator.LESS, Operator.LESS_EQUALS, Operator.GREATER_EQUALS, Operator.GREATER);
            case OR:
            case AND:
                return another(r, operator, Operator.OR, Operator.AND);
            //$CASES-OMITTED$
            default:
                return operator;
            }
        }

        private static Operator another(
                final Random r, final Operator old, final Operator... operators) {
            final List<Operator> choices = new ArrayList<>(Arrays.asList(operators));
            choices.remove(old);
            if (choices.size() == 1) {
                return choices.get(0);
            } else {
                return choices.get(r.nextInt(choices.size()));
            }
        }

        @Override
        public int getAnchorLine() {
            return this.expr.getBegin().get().line;
        }

        @Override
        public void createRemark(final int nbr, final Properties p) {
            final Set<Integer> lines = Collections.singleton(this.getAnchorLine());
            final RemarkType type;
            switch (this.expr.getOperator()) {
            case AND:
            case OR:
                type = RemarkType.WRONG_CONDITION;
                break;
            case LESS:
            case GREATER:
            case GREATER_EQUALS:
            case LESS_EQUALS:
                type = RemarkType.WRONG_COMPARISON;
                break;
            case PLUS:
            case MINUS:
                type = RemarkType.WRONG_CALCULATION;
                break;
            //$CASES-OMITTED$
            default:
                throw new AssertionError("invalid operator");
            }
            this.setRemark(nbr, p, lines,
                    type, ".+", "korrekt wäre " + this.correct);
        }

    }

}
