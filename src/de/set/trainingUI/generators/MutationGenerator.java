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
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import de.set.trainingUI.generators.SwapCalledMethodMutation.SwapMethodData;
import de.set.trainingUI.generators.SwapVariableExpressionMutation.SwapVariableData;

@SuppressWarnings("nls")
public class MutationGenerator extends Generator {

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
    	final int maxMutationCount = this.determineCount(rand);
    	final String curFile = this.normalize(this.sourceFile);
    	try {
    		return applyMutations(taskProperties, rand, maxMutationCount, curFile, MutationGenerator::findPossibleMutations);
    	} catch (final Exception e) {
    		throw new RuntimeException("problem while mutating " + curFile, e);
    	}
    }

	static String applyMutations(
			final Properties taskProperties,
			final Random rand,
			int maxMutationCount,
			String initialFile,
			Function<CompilationUnit, List<Mutation>> mutationSource) {
		int remainingMutationCount = maxMutationCount;
		int firstAllowedLine = 0;
		String curFile = initialFile;

    	// to avoid that later mutations lead to changes in the line numbers or mutated code
    	//  of earlier mutations, mutations are applied from top to bottom
    	final List<Mutation> appliedMutations = new ArrayList<>();
    	while (remainingMutationCount > 0) {
            final CompilationUnit ast = StaticJavaParser.parse(curFile);
            final List<Mutation> allMutations = mutationSource.apply(ast);
    		final List<Mutation> withCorrectPos = selectWithCorrectPos(allMutations, firstAllowedLine);
    		if (withCorrectPos.isEmpty()) {
    			// no further mutations possible
    			break;
    		}
    		final List<Mutation> randomSubset = selectRandomSubset(withCorrectPos, remainingMutationCount, rand);
    		final Mutation toApply = pickTopmostMutation(randomSubset);
    		toApply.apply(rand);
    		final String nextFile = ast.toString();
    		// It might happen that a mutation changes code before its anchor line and that
    		//  this leads to a change too early in the file. Only use the mutation if this
    		//  did not happen.
    		final int firstChangedLine = findFirstChangedLine(curFile, nextFile);
    		if (firstChangedLine >= firstAllowedLine) {
    			appliedMutations.add(toApply);
	    		toApply.createRemark(appliedMutations.size(), taskProperties);

	    		firstAllowedLine = findStartOfUnchangedSuffix(curFile, nextFile);
	    		if (firstAllowedLine <= 0) {
	    			throw new IllegalArgumentException("mutation did not change anything: " + toApply + " in " + curFile);
	    		}
	    		// make sure that two remarks never occur in the same line
	    		firstAllowedLine = Math.max(firstAllowedLine, toApply.getAnchorLine() + 1);
	    		curFile = nextFile;
    		}
    		remainingMutationCount--;
    	}
        if (appliedMutations.isEmpty()) {
        	throw new IllegalArgumentException("no mutations found for " + initialFile);
        }
        taskProperties.put("usedMutations",
        		appliedMutations.stream().map((Mutation m) -> m.getClass().getName()).collect(Collectors.joining(",")));
    	return curFile;
	}

    private static List<Mutation> selectWithCorrectPos(List<Mutation> mutations, int minPos) {
    	final List<Mutation> ret = new ArrayList<Mutation>();
    	for (final Mutation m : mutations) {
    		if (m.getAnchorLine() >= minPos) {
    			ret.add(m);
    		}
    	}
		return ret;
	}

	private static int findFirstChangedLine(String curFile, String nextFile) {
    	final String[] lines1 = curFile.split("\n");
    	final String[] lines2 = nextFile.split("\n");
    	final int minSize = Math.min(lines1.length, lines2.length);
    	for (int prefixLength = 0; prefixLength < minSize; prefixLength++) {
    		if (!lines1[prefixLength].equals(lines2[prefixLength])) {
    			return prefixLength + 1;
    		}
    	}
		return minSize + 1;
	}

	private static int findStartOfUnchangedSuffix(String curFile, String nextFile) {
    	final String[] lines1 = curFile.split("\n");
    	final String[] lines2 = nextFile.split("\n");
    	final int minSize = Math.min(lines1.length, lines2.length);
    	for (int suffixLength = 0; suffixLength < minSize; suffixLength++) {
    		if (!lines1[lines1.length - 1 - suffixLength].equals(lines2[lines2.length - 1 - suffixLength])) {
    			return lines2.length + 1 - suffixLength;
    		}
    	}
		return 0;
	}

	private static List<Mutation> selectRandomSubset(List<Mutation> mutations, int maxCount, Random rand) {
    	final Mutation[] copy = mutations.toArray(new Mutation[mutations.size()]);
    	final int toPick = Math.min(maxCount, copy.length);
    	final List<Mutation> ret = new ArrayList<Mutation>(toPick);
    	while (ret.size() < toPick) {
    		final int index = rand.nextInt(copy.length - ret.size());
    		ret.add(copy[index]);
    		copy[index] = copy[copy.length - ret.size()];
    	}
    	return ret;
	}

	private static Mutation pickTopmostMutation(List<Mutation> mutations) {
    	Mutation topmost = null;
    	int topmostPos = Integer.MAX_VALUE;
    	for (final Mutation m : mutations) {
    		if (m.getAnchorLine() < topmostPos) {
    			topmost = m;
    			topmostPos = m.getAnchorLine();
    		}
    	}
		return topmost;
	}

    private int determineCount(final Random rand) {
    	final double d = rand.nextDouble();
    	if (d < 0.05) {
    		return 3;
    	} else if (d < 0.2) {
    		return 2;
    	} else {
    		return 1;
    	}
    }

    private String normalize(final File file) throws FileNotFoundException {
        final CompilationUnit parsed = StaticJavaParser.parse(file);
        //remove package statement, if contained
        parsed.removePackageDeclaration();
        //parse twice to normalize line numbers
        return parsed.toString();
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
            public Void visit(final EnclosedExpr n, final Void v) {
                super.visit(n, v);
                if (RemoveParenthesesMutation.isApplicable(n)) {
                	ret.add(new RemoveParenthesesMutation(n));
                }
                return null;
            }
            @Override
            public Void visit(final BinaryExpr n, final Void v) {
                super.visit(n, v);
                if (FlipOperatorMutation.isApplicable(n)) {
                    ret.add(new FlipOperatorMutation(n));
                }
                if (RemoveBinaryExprMutation.isApplicable(n)) {
                	ret.add(new RemoveBinaryExprMutation(n));
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
            @Override
            public Void visit(final ReturnStmt n, final Void v) {
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
