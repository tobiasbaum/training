package de.set.trainingUI.generators;

import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;

import de.set.trainingUI.RemarkType;

abstract class Mutation {

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

}