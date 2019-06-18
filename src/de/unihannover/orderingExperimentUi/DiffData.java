package de.unihannover.orderingExperimentUi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class DiffData {

    public static final class FragmentData {
        private final String header;
        private final int startLineNumber;
        private final String content;

        public FragmentData(String description, int lineNumber, String string) {
            this.header = description;
            this.startLineNumber = lineNumber;
            this.content = string;
        }

        public String getHeader() {
            return this.header;
        }

        public int getStartLineNumber() {
            return this.startLineNumber;
        }

        public String getContentEscaped() {
            return this.content.replace("\\", "\\\\").replace("\n", "\\n").replace("'", "\\'").replace("\"", "\\\"");
        }

        public String getContent() {
            return this.content;
        }

        public int getLineCount() {
            return this.content.split("\n", -1).length;
        }

        public List<String> getLines() {
            if (this.content.isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.asList(this.content.split("\n"));
        }

        public FragmentData shrink(int prefix, int suffix) {
            final List<String> lines = this.getLines();
            return new FragmentData(this.header, this.startLineNumber + prefix, join(lines.subList(prefix, lines.size() - suffix)));
        }

    }

    public static final class ChangePart {
        private final String id;
        private final int index;
        private final FragmentData left;
        private final FragmentData right;
        private final String prefix;
        private final String suffix;

        public ChangePart(String id, int index, FragmentData left2, FragmentData right2, String prefix, String suffix) {
            this.id = id;
            this.index = index;
            this.left = left2;
            this.right = right2;
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String getId() {
            return this.id;
        }

        public int getIndex() {
            return this.index;
        }

        public FragmentData getLeft() {
            return this.left;
        }

        public FragmentData getRight() {
            return this.right;
        }

        public int getLineCount() {
            return Math.max(this.left.getLineCount(), this.right.getLineCount());
        }

        public int getContextLineCount() {
            return this.countLines(this.prefix) + this.countLines(this.suffix);
        }

        public int getPrefixLineCount() {
            return this.countLines(this.prefix);
        }

        private int countLines(String s) {
            if (s.isEmpty()) {
                return 0;
            }
            return new FragmentData("", 0, s).getLineCount();
        }

        public String getPrefix() {
            return this.prefix;
        }

        public String getPrefixEscaped() {
            if (this.prefix.isEmpty()) {
                return "";
            }
            return new FragmentData("", 0, this.prefix).getContentEscaped() + "\\n";
        }

        public String getSuffixEscaped() {
            if (this.suffix.isEmpty()) {
                return "";
            }
            return  "\\n" + new FragmentData("", 0, this.suffix).getContentEscaped();
        }

        public boolean hasMoreContext() {
            return !(this.suffix.isEmpty() && this.prefix.isEmpty());
        }
    }

    private String description;
    private String questions;
    private final List<ChangePart> changes = new ArrayList<>();

    public static DiffData load(String diffPath, Treatment treatment) throws IOException {
        final Properties orders = new Properties();
        orders.load(DiffData.class.getResourceAsStream(fullPath(diffPath, "permutations.properties")));
        final String[] idsInOrder = orders.getProperty(treatment.toString()).split(",");
        final DiffData ret = new DiffData();
        int index = 0;
        for (final String id : idsInOrder) {
            ret.changes.add(loadChangePart(index++, diffPath, id));
        }
        ret.description = read(fullPath(diffPath, "description"));
        ret.questions = read(fullPath(diffPath, "questions"));
        return ret;
    }

    private static String read(String resource) throws IOException {
        final StringBuilder ret = new StringBuilder();
        try (InputStream s = DiffData.class.getResourceAsStream(resource)) {
            final BufferedReader r = new BufferedReader(new InputStreamReader(s, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                ret.append(line).append('\n');
            }
            return ret.toString();
        }
    }

    private static ChangePart loadChangePart(int index, String diffPath, String id) throws IOException {
        try (InputStream s = DiffData.class.getResourceAsStream(fullPath(diffPath, id + ".hunk"))) {
            final BufferedReader r = new BufferedReader(new InputStreamReader(s, "UTF-8"));
            final FragmentData left = loadFragmentData(r);
            final FragmentData right = loadFragmentData(r);
            final List<String> commonPrefix = new ArrayList<>();
            final List<String> commonSuffix = new ArrayList<>();
            determineCommonPrefixAndSuffix(left, right, commonPrefix, commonSuffix);
            return new ChangePart(
                            id,
                            index,
                            left.shrink(commonPrefix.size(), commonSuffix.size()),
                            right.shrink(commonPrefix.size(), commonSuffix.size()),
                            join(commonPrefix),
                            join(commonSuffix));
        }
    }

    private static void determineCommonPrefixAndSuffix(
                    FragmentData left,
                    FragmentData right,
                    List<String> commonPrefixBuffer,
                    List<String> commonSuffixBuffer) {
        final LinkedList<String> linesLeft = new LinkedList<>(left.getLines());
        final LinkedList<String> linesRight = new LinkedList<>(right.getLines());
        boolean didSomething;
        do {
            didSomething = false;
            if (linesLeft.isEmpty() || linesRight.isEmpty()) {
                break;
            }
            if (linesLeft.getFirst().equals(linesRight.getFirst())) {
                commonPrefixBuffer.add(linesLeft.removeFirst());
                linesRight.removeFirst();
                didSomething = true;
            }
            if (linesLeft.isEmpty() || linesRight.isEmpty()) {
                break;
            }
            if (linesLeft.getLast().equals(linesRight.getLast())) {
                commonSuffixBuffer.add(linesLeft.removeLast());
                linesRight.removeLast();
                didSomething = true;
            }
        } while (didSomething);

        //four context lines shall stay
        for (int i = 0; i < 4; i++) {
            if (!commonPrefixBuffer.isEmpty()) {
                commonPrefixBuffer.remove(commonPrefixBuffer.size() - 1);
            }
            if (!commonSuffixBuffer.isEmpty()) {
                commonSuffixBuffer.remove(commonSuffixBuffer.size() - 1);
            }
        }

        //when the remaining context is very small, show it also
        if (commonPrefixBuffer.size() <= 2) {
            commonPrefixBuffer.clear();
        }
        if (commonSuffixBuffer.size() <= 2) {
            commonSuffixBuffer.clear();
        }

        //reverse the suffix list so that its in document order
        Collections.reverse(commonSuffixBuffer);
    }

    private static String join(List<String> list) {
        if (list.isEmpty()) {
            return "";
        }
        final Iterator<String> iter = list.iterator();
        final StringBuilder ret = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            ret.append('\n').append(iter.next());
        }
        return ret.toString();
    }

    private static FragmentData loadFragmentData(BufferedReader r) throws IOException {
        final StringBuilder content = new StringBuilder();
        while (true) {
            final String line = r.readLine();
            if (line.startsWith("<<<")) {
                final int comma = line.indexOf(",");
                final int lineNumber = Integer.parseInt(line.substring(3, comma));
                final String description = line.substring(comma + 1);
                String contentWithoutNewlineAtEnd;
                if (content.length() == 0) {
                    contentWithoutNewlineAtEnd = content.toString();
                } else {
                    contentWithoutNewlineAtEnd = content.substring(0, content.length() - 1).toString();
                }
                return new FragmentData(description, lineNumber, contentWithoutNewlineAtEnd);
            } else {
                content.append(line).append('\n');
            }
        }
    }

    private static String fullPath(String diffPath, String filename) {
        return "/changes/" + diffPath + "/" + filename;
    }

    public List<ChangePart> getChangeParts() {
        return this.changes;
    }

    public String getDescription() {
        return this.description;
    }

    public String getQuestions() {
        return this.questions;
    }

}
