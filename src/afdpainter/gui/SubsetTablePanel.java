package afdpainter.gui;

import afdpainter.model.State;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubsetTablePanel extends JScrollPane {

    public SubsetTablePanel(Set<Character> alphabet, List<Set<State>> order, List<Map<Character, Integer>> deltaByIndex) {
        List<Character> symbols = new ArrayList<>(alphabet);

        String[] columnNames = new String[symbols.size() + 1];
        columnNames[0] = "Subconjunto (AFN)";
        for (int i = 0; i < symbols.size(); i++) columnNames[i + 1] = String.valueOf(symbols.get(i));

        Object[][] data = new Object[order.size()][columnNames.length];
        for (int row = 0; row < order.size(); row++) {
            Set<State> subset = order.get(row);
            boolean initial = row == 0;
            boolean finalRow = false;
            List<String> memberNames = new ArrayList<>();
            for (State s : subset) {
                memberNames.add(s.getName());
                if (s.isFinalState()) finalRow = true;
            }
            data[row][0] = Labels.subsetCellHtml(memberNames, initial, finalRow);

            Map<Character, Integer> deltaRow = deltaByIndex.get(row);
            for (int col = 0; col < symbols.size(); col++) {
                char symbol = symbols.get(col);
                Integer targetIdx = (deltaRow == null) ? null : deltaRow.get(symbol);
                if (targetIdx == null) {
                    data[row][col + 1] = "-";
                } else {
                    List<String> targetNames = new ArrayList<>();
                    for (State s : order.get(targetIdx)) targetNames.add(s.getName());
                    data[row][col + 1] = Labels.subsetCellHtml(targetNames, false, false);
                }
            }
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setEnabled(false);
        table.setRowHeight(24);
        setViewportView(table);
        setPreferredSize(new Dimension(280, 150));
    }
}
