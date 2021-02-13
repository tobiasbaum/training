package de.set.trainingUI.generators;

import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import de.set.trainingUI.RemarkType;

public class RemarkCreator {

	private final Properties p;

    public RemarkCreator(Properties taskProperties) {
    	this.p = taskProperties;
	}

	void setRemark(final int nbr, int anchorLine, final Set<Integer> lines, final RemarkType type,
            final String pattern, final String text) {
    	this.setRemark(nbr, anchorLine, lines, EnumSet.of(type), pattern, text);
    }

    void setRemark(final int nbr, int anchorLine, final Set<Integer> lines, final Set<RemarkType> type,
            final String pattern, final String text) {
        assert lines.contains(anchorLine);
        this.p.setProperty("remark." + nbr + ".pattern",
                lines.stream().map((final Integer i) -> i.toString()).collect(Collectors.joining(","))
                + ";" + type.stream().map((final RemarkType t) -> t.name()).collect(Collectors.joining(","))
                + ";" + pattern);
        this.p.setProperty("remark." + nbr + ".example",
                anchorLine
                + ";" + type.iterator().next().name()
                + ";" + text);
    }

}
