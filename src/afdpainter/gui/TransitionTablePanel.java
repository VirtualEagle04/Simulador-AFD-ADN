package afdpainter.gui;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tabla de transiciones (delta) del autómata actual: filas = estados,
 * columnas = símbolos del alfabeto (más épsilon en modo AFN). Cada celda
 * muestra el/los estado(s) destino. Durante la simulación, la fila del
 * estado activo y la celda de la transición recién usada se resaltan con
 * los mismos colores que el lienzo de dibujo.
 */
public class TransitionTablePanel extends JScrollPane {

    private final JTable table;
    private List<State> rowStates = new ArrayList<>();
    private List<Character> colSymbols = new ArrayList<>();

    private Set<State> highlightedRows = Collections.emptySet();
    private Color rowHighlightColor = Palette.CURRENT_STATE;
    private Set<State> highlightedCellFrom = Collections.emptySet();
    private Character highlightedSymbol;

    public TransitionTablePanel() {
        table = new JTable(new DefaultTableModel());
        table.setEnabled(false);
        table.setRowHeight(24);
        setViewportView(table);
        setPreferredSize(new Dimension(260, 170));
    }

    /** Reconstruye la tabla a partir del estado actual del autómata. */
    public void refresh(Automaton automaton) {
        rowStates = new ArrayList<>(automaton.getStates());
        colSymbols = new ArrayList<>(automaton.getAlphabet());
        if (automaton.getKind() == Automaton.Kind.NFA) {
            colSymbols.add(Transition.EPSILON);
        }

        String[] columnNames = new String[colSymbols.size() + 1];
        columnNames[0] = "Estado";
        for (int i = 0; i < colSymbols.size(); i++) {
            char c = colSymbols.get(i);
            columnNames[i + 1] = (c == Transition.EPSILON) ? "\u03b5" : String.valueOf(c);
        }

        Object[][] data = new Object[rowStates.size()][columnNames.length];
        for (int row = 0; row < rowStates.size(); row++) {
            State s = rowStates.get(row);
            String prefix = (s.isInitial() ? "\u2192" : "") + (s.isFinalState() ? "*" : "");
            data[row][0] = prefix + s.getName();
            for (int col = 0; col < colSymbols.size(); col++) {
                char symbol = colSymbols.get(col);
                List<Transition> ts = automaton.getTransitions(s, symbol);
                if (ts.isEmpty()) {
                    data[row][col + 1] = "-";
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Transition t : ts) {
                        if (sb.length() > 0) sb.append(',');
                        sb.append(t.getTo().getName());
                    }
                    data[row][col + 1] = sb.toString();
                }
            }
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setModel(model);
        installRenderer();
        clearHighlight();
    }

    private void installRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, false, false, row, col);
                c.setBackground(Color.WHITE);
                if (row >= 0 && row < rowStates.size()) {
                    State rowState = rowStates.get(row);
                    if (col == 0 && highlightedRows.contains(rowState)) {
                        c.setBackground(rowHighlightColor);
                    } else if (col > 0 && highlightedSymbol != null
                            && colSymbols.get(col - 1) == highlightedSymbol
                            && highlightedCellFrom.contains(rowState)) {
                        c.setBackground(Palette.ACTIVE_EDGE);
                    }
                }
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    /** Resalta (como en el lienzo) las filas de los estados activos actuales. */
    public void setRowHighlight(Set<State> states, Color color) {
        this.highlightedRows = states == null ? Collections.emptySet() : states;
        this.rowHighlightColor = color;
        table.repaint();
    }

    /** Resalta la celda (estado origen, símbolo) de la transición recién usada. */
    public void setCellHighlight(Set<State> fromStates, Character symbol) {
        this.highlightedCellFrom = fromStates == null ? Collections.emptySet() : fromStates;
        this.highlightedSymbol = symbol;
        table.repaint();
    }

    public void clearHighlight() {
        highlightedRows = new LinkedHashSet<>();
        highlightedCellFrom = new LinkedHashSet<>();
        highlightedSymbol = null;
        rowHighlightColor = Palette.CURRENT_STATE;
        table.repaint();
    }
}
