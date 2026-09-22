package afdpainter.gui;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Labels {

    private static final Pattern TRAILING_DIGITS = Pattern.compile("^(.*?)(\\d+)$");

    private Labels() { }

    static String subscriptHtml(String name) {
        String escaped = escape(name);
        Matcher m = TRAILING_DIGITS.matcher(escaped);
        if (m.matches()) {
            return m.group(1) + "<sub>" + m.group(2) + "</sub>";
        }
        return escaped;
    }

    static String stateCellHtml(String name, boolean initial, boolean finalState) {
        StringBuilder sb = new StringBuilder("<html>");
        if (initial) sb.append('\u2192');
        sb.append(subscriptHtml(name));
        if (finalState) sb.append("''");
        sb.append("</html>");
        return sb.toString();
    }

    static String nameListHtml(List<String> names) {
        if (names.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder("<html>");
        boolean first = true;
        for (String n : names) {
            if (!first) sb.append(", ");
            sb.append(subscriptHtml(n));
            first = false;
        }
        sb.append("</html>");
        return sb.toString();
    }

    static String subsetCellHtml(Collection<String> memberNames, boolean initial, boolean finalState) {
        StringBuilder sb = new StringBuilder("<html>");
        if (initial) sb.append('\u2192');
        sb.append('{');
        boolean first = true;
        for (String n : memberNames) {
            if (!first) sb.append(',');
            sb.append(subscriptHtml(n));
            first = false;
        }
        sb.append('}');
        if (finalState) sb.append("''");
        sb.append("</html>");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
