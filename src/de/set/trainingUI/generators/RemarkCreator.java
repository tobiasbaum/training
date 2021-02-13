package de.set.trainingUI.generators;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import de.set.trainingUI.RemarkType;

public class RemarkCreator {

	private final Properties p;
	private final LineMap lineMap;

    public RemarkCreator(Properties taskProperties, LineMap lineMap) {
    	this.p = taskProperties;
    	this.lineMap = lineMap;
	}

	void setRemark(final int nbr, int anchorLine, final Set<Integer> lines, final RemarkType type,
            final String pattern, final String text) {
    	this.setRemark(nbr, anchorLine, lines, EnumSet.of(type), pattern, text);
    }

    void setRemark(final int nbr, int anchorLine, final Set<Integer> lines, final Set<RemarkType> type,
            final String pattern, final String text) {
        assert lines.contains(anchorLine);
        final Set<Integer> mappedLines = new LinkedHashSet<Integer>();
        for (final Integer line : lines) {
        	mappedLines.add(this.lineMap.mapLine(line));
        }
        this.p.setProperty("remark." + nbr + ".pattern",
                mappedLines.stream().map((final Integer i) -> i.toString()).collect(Collectors.joining(","))
                + ";" + type.stream().map((final RemarkType t) -> t.name()).collect(Collectors.joining(","))
                + ";" + pattern);
        this.p.setProperty("remark." + nbr + ".example",
                this.lineMap.mapLine(anchorLine)
                + ";" + type.iterator().next().name()
                + ";" + text);
    }

}
