package de.set.trainingUI.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import de.set.trainingUI.RemarkType;
import de.set.trainingUI.generators.SwapCalledMethodMutation.SwapMethodData;
import de.set.trainingUI.generators.SwapVariableExpressionMutation.SwapVariableData;

@SuppressWarnings("nls")
public class MutationGenerator extends Generator {

    abstract static class Mutation {

        public abstract void apply(Random r);

        public abstract void createRemark(final int nbr, Properties taskProperties);

        protected void setRemark(final int nbr, final Properties p, final Set<Integer> lines, final RemarkType type,
                final String pattern, final String text) {
        	this.setRemark(nbr, p, lines, EnumSet.of(type), pattern, text);
        }

        protected void setRemark(final int nbr, final Properties p, final Set<Integer> lines, final Set<RemarkType> type,
                final String pattern, final String text) {
            assert lines.contains(this.getAnchorLine());
            p.setProperty("remark." + nbr + ".pattern",
                    lines.stream().map((final Integer i) -> i.toString()).collect(Collectors.joining(","))
                    + ";" + type.stream().map((final RemarkType t) -> t.name()).collect(Collectors.joining(","))
                    + ";" + pattern);
            p.setProperty("remark." + nbr + ".example",
                    this.getAnchorLine()
                    + ";" + type.iterator().next().name()
                    + ";" + text);
        }

    	protected static void addBeginToEnd(final Set<Integer> lines, Node node) {
    		final int start = node.getBegin().get().line;
            final int end = node.getEnd().get().line;
            lines.add(start);
            for (int i = start; i < end; i++) {
                lines.add(i);
            }
    	}

        protected static<T> T pickRandom(final List<T> choices, final Random r) {
            return choices.get(r.nextInt(choices.size()));
        }

        public abstract int getAnchorLine();

		public abstract boolean isStillValid();

		protected static boolean isInCU(Node n) {
			return getCU(n) != null;
		}

		protected static boolean isInSameCU(Node n1, Node n2) {
			return getCU(n1) == getCU(n2);
		}

		private static CompilationUnit getCU(Node n) {
			if (n instanceof CompilationUnit) {
				return (CompilationUnit) n;
			} else {
				if (n.getParentNode().isPresent()) {
					return getCU(n.getParentNode().get());
				} else {
					return null;
				}
			}
		}

    }

    private final Properties properties;
	private final File sourceFile;
	private final int maxCount;


	/**
	 * MutationGenerator based on a template directory.
	 */
    public MutationGenerator(final File template) throws IOException {
        this.sourceFile = new File(template, "source");
        this.properties = loadProperties(new File(template, "task.properties"));
        final Properties tp = loadProperties(new File(template, "template.properties"));
        this.maxCount = Integer.parseInt(tp.getProperty("count"));
    }

	/**
	 * MutationGenerator based on a single source file.
	 */
    public MutationGenerator(final File source, int maxCount) throws IOException {
        this.sourceFile = source;
        this.properties = new Properties();
        this.properties.setProperty("type", "review");
        this.properties.setProperty("family", source.getName());
        this.maxCount = maxCount;
    }

    @Override
    protected int getMaxCount() {
        return this.maxCount;
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
        final Properties taskProperties = (Properties) this.properties.clone();
        final String s = this.mutateSource(taskProperties, rand);
        final String id = this.hash(s);

        final File dir = new File(targetDir, id);
        dir.mkdir();
        Files.write(dir.toPath().resolve("source"), s.getBytes("UTF-8"));

        try (FileOutputStream out = new FileOutputStream(new File(dir, "task.properties"))) {
            this.store(taskProperties, out);
        }
    }

    private void store(Properties p, OutputStream out) throws IOException {
    	final Set<String> keys = new TreeSet<>();
    	for (final Object o : p.keySet()) {
    		keys.add((String) o);
    	}
    	final Writer w = new OutputStreamWriter(out, "ISO-8859-1");
    	for (final String key : keys) {
    		w.write(escapeProperty(key));
    		w.write('=');
    		w.write(escapeProperty(p.getProperty(key)));
    		w.write('\n');
    	}
    	w.flush();
    }

	private static String escapeProperty(String theString) {

		final int len = theString.length();
		final StringBuffer outBuffer = new StringBuffer(len * 2);

		for (int x = 0; x < len; x++) {
			final char aChar = theString.charAt(x);
			switch (aChar) {
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=':
			case ':':
			case '\\':
			case '#':
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				if ((aChar < 0x0020) || (aChar > 0x007e)) {
					outBuffer.append(String.format("\\u%04x", Integer.valueOf(aChar)));
				} else {
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}

    private String mutateSource(final Properties taskProperties, final Random rand) throws FileNotFoundException {
        final CompilationUnit ast = this.parseNormalized(this.sourceFile);
        final List<Mutation> mutations = findPossibleMutations(ast);
        if (mutations.isEmpty()) {
        	throw new IllegalArgumentException("no mutations found for " + this.sourceFile);
        }
        final int count = this.determineCount(rand, mutations.size());
        Collections.shuffle(mutations, rand);
        final List<Mutation> chosen = mutations.subList(0, count);
        this.removeInSameLine(chosen);
        final List<Mutation> applied = new ArrayList<>();
        for (final Mutation m : chosen) {
        	if (m.isStillValid()) {
        		m.apply(rand);
        		applied.add(m);
        	}
        }
        int i = 1;
        for (final Mutation m : applied) {
            m.createRemark(i++, taskProperties);
        }
        taskProperties.put("usedMutations",
        		applied.stream().map((Mutation m) -> m.getClass().getName()).collect(Collectors.joining(",")));
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
        //remove package statement, if contained
        parsed.removePackageDeclaration();
        //parse twice to normalize line numbers
        return StaticJavaParser.parse(parsed.toString());
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
        final SwapVariableData swapVariableData = SwapVariableExpressionMutation.analyze(ast);
        final SwapMethodData swapMethodData = SwapCalledMethodMutation.analyze(ast);
        final List<Mutation> ret = new ArrayList<>();
        ast.accept(new GenericVisitorAdapter<Void, Void>() {
            @Override
            public Void visit(final FieldDeclaration n, final Void v) {
                super.visit(n, v);
                if (DeleteVolatileMutation.isApplicable(n)) {
                	ret.add(new DeleteVolatileMutation(n));
                }
                return null;
            }
            @Override
            public Void visit(final MethodDeclaration n, final Void v) {
                super.visit(n, v);
                if (DeleteSynchronizedMutation.isApplicable(n)) {
                	ret.add(new DeleteSynchronizedMutation(n));
                }
                return null;
            }
            @Override
            public Void visit(final IfStmt n, final Void v) {
                super.visit(n, v);
                if (InvertMutation.isApplicable(n)) {
                	ret.add(new InvertMutation(n));
                }
                if (RemoveStatementMutation.isApplicable(n)) {
                    ret.add(new RemoveStatementMutation(n));
                }
                if (MoveOutOfIfMutation.isApplicable(n)) {
                	ret.add(new MoveOutOfIfMutation(n));
                }
                ret.add(new RemoveIfMutation(n));
                return null;
            }
            @Override
            public Void visit(final ConditionalExpr n, final Void v) {
                super.visit(n, v);
                ret.add(new InvertConditionalExprMutation(n));
                ret.add(new RemoveTernaryMutation(n));
                return null;
            }
            @Override
            public Void visit(final ExpressionStmt n, final Void v) {
                super.visit(n, v);
                if (RemoveStatementMutation.isApplicable(n)) {
                    ret.add(new RemoveStatementMutation(n));
                }
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
            @Override
            public Void visit(final AssignExpr n, final Void v) {
                super.visit(n, v);
                if (FlipAssignMutation.isApplicable(n)) {
                    ret.add(new FlipAssignMutation(n));
                }
                return null;
            }
            @Override
            public Void visit(final NameExpr n, final Void v) {
                super.visit(n, v);
                if (swapVariableData.isApplicable(n)) {
                    ret.add(new SwapVariableExpressionMutation(swapVariableData, n));
                }
                return null;
            }
            @Override
            public Void visit(final MethodCallExpr n, final Void v) {
                super.visit(n, v);
                if (RemovePartFromExpressionChainMutation.isApplicable(n)) {
                    ret.add(new RemovePartFromExpressionChainMutation(n));
                }
                if (swapMethodData.isApplicable(n)) {
                    ret.add(new SwapCalledMethodMutation(swapMethodData, n));
                }
                return null;
            }
            @Override
            public Void visit(final ContinueStmt n, final Void v) {
                super.visit(n, v);
                if (RemoveStatementMutation.isApplicable(n)) {
                    ret.add(new RemoveStatementMutation(n));
                }
                return null;
            }
            @Override
            public Void visit(final BreakStmt n, final Void v) {
                super.visit(n, v);
                if (RemoveStatementMutation.isApplicable(n)) {
                    ret.add(new RemoveStatementMutation(n));
                }
                return null;
            }
        }, null);
        return ret;
    }

}
