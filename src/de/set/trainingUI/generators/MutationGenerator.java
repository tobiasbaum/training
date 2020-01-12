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
            p.setProperty("remark." + nbr + ".pattern",
                    lines.stream().map((final Integer i) -> i.toString()).collect(Collectors.joining(","))
                    + ";" + type.name()
                    + ";" + pattern);
            p.setProperty("remark." + nbr + ".example",
                    lines.iterator().next()
                    + ";" + type.name()
                    + ";" + text);
        }

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
        final Mutation chosen = mutations.get(rand.nextInt(mutations.size()));
        chosen.apply(rand);
        chosen.createRemark(1, taskProperties);
        return ast.toString();
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
                ret.add(new InvertMutation(n));
                return null;
            }
            @Override
            public Void visit(final ExpressionStmt n, final Void v) {
                if (n.getParentNode().get().getChildNodes().size() <= 1) {
                    return null;
                }
                if (!n.getExpression().isMethodCallExpr()) {
                    return null;
                }
                ret.add(new RemoveMutation(n));
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
        public void createRemark(final int nbr, final Properties p) {
            final Set<Integer> lines = Collections.singleton(this.ifStmt.getBegin().get().line);
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

}
