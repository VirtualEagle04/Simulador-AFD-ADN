package afdpainter.gui;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formatea nombres de estado como HTML para las tablas de transición
 * (JTable/JLabel sí soportan HTML básico, incluido &lt;sub&gt;), de modo
 * que "q0" se vea como en notación matemática (q con 0 en subíndice), y
 * los estados de aceptación se marquen con dos comillas simples (k0'').
 */
final class Labels {

    private static final Pattern TRAILING_DIGITS = Pattern.compile("^(.*?)(\\d+)$");

    private Labels() { }

    /** "q0" -> "q&lt;sub&gt;0&lt;/sub&gt;" (sin las etiquetas &lt;html&gt;). */
    static String subscriptHtml(String name) {
        String escaped = escape(name);
        Matcher m = TRAILING_DIGITS.matcher(escaped);
        if (m.matches()) {
            return m.group(1) + "<sub>" + m.group(2) + "</sub>";
        }
        return escaped;
    }

    /** Celda de encabezado de fila: flecha si es inicial, subíndice, y '' si es de aceptación. */
    static String stateCellHtml(String name, boolean initial, boolean finalState) {
        StringBuilder sb = new StringBuilder("<html>");
        if (initial) sb.append('\u2192');
        sb.append(subscriptHtml(name));
        if (finalState) sb.append("''");
        sb.append("</html>");
        return sb.toString();
    }

    /** Celda con uno o varios nombres de estado destino separados por coma (AFN). */
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

    /** Celda con un subconjunto {q0,q1}: flecha si es inicial, y '' si es de aceptación. */
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
