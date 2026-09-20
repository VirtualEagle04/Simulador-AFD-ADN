package afdpainter.gui;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;
import afdpainter.sim.Simulator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainFrame extends JFrame {

    private final Automaton automaton = new Automaton();
    private final DrawingPanel drawingPanel = new DrawingPanel(automaton);
    private final ToolBar toolBar = new ToolBar();
    private final PlaybackBar playbackBar = new PlaybackBar();

    private final JTextField alphabetField = new JTextField();
    private final JTextField stringField = new JTextField();
    private final JLabel currentAlphabetLabel = new JLabel("\u03A3 = { }");
    private final JLabel statusLabel = new JLabel("<html>Sin simulaci\u00f3n activa</html>");
    private final JLabel resultLabel = new JLabel(" ");

    private Simulator.Result currentResult;
    private int stepIndex;
    private boolean simulationActive = false;
    private Timer timer;

    public MainFrame() {
        super("Editor y Simulador de AFD");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        toolBar.setModeListener(drawingPanel::setMode);
        toolBar.setClearListener(this::onClear);
        toolBar.setUndoListener(drawingPanel::undo);
        toolBar.setHelpListener(this::showHelp);
        add(toolBar, BorderLayout.NORTH);

        add(new JScrollPane(drawingPanel), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);
        add(playbackBar, BorderLayout.SOUTH);

        wirePlayback();
    }

    // ---------------------------- Interfaz ----------------------------

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setPreferredSize(new Dimension(290, 100));

        panel.add(sectionTitle("Alfabeto \u03A3"));
        alphabetField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        alphabetField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(alphabetField);
        JLabel hint = new JLabel("Ej: 0,1  o  a,b,c");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(hint);
        panel.add(Box.createVerticalStrut(4));
        JButton saveAlphabetBtn = new JButton("Guardar Alfabeto");
        saveAlphabetBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveAlphabetBtn.addActionListener(e -> saveAlphabet());
        panel.add(saveAlphabetBtn);
        currentAlphabetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(currentAlphabetLabel);

        panel.add(Box.createVerticalStrut(16));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(16));

        panel.add(sectionTitle("Cadena a validar"));
        stringField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        stringField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(stringField);
        panel.add(Box.createVerticalStrut(4));
        JButton verifyBtn = new JButton("Verificar / Cargar Cadena");
        verifyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        verifyBtn.addActionListener(e -> loadSimulation());
        panel.add(verifyBtn);

        panel.add(Box.createVerticalStrut(16));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(16));

        panel.add(sectionTitle("Estado de la simulaci\u00f3n"));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(8));
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 16f));
        resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(resultLabel);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void onClear() {
        int opt = JOptionPane.showConfirmDialog(this, "\u00bfBorrar todo el autómata dibujado?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            stopSimulation();
            automaton.clear();
            drawingPanel.repaint();
        }
    }

    private void showHelp() {
        String msg = "C\u00d3MO USAR EL PROGRAMA\n\n"
                + "1) Dibujar Estado: mantenga oprimido el mouse y dibuje libremente\n"
                + "   un c\u00edrculo cerrado. Al soltar, se crea un estado (q0, q1, ..., qN).\n"
                + "   El primer estado creado se marca autom\u00e1ticamente como inicial.\n"
                + "   Doble clic sobre un estado para renombrarlo.\n\n"
                + "2) Dibujar Transici\u00f3n: oprima el mouse SOBRE un estado de origen,\n"
                + "   arrastre y suelte SOBRE el estado destino (o sobre el mismo estado\n"
                + "   para crear un auto-lazo). Se le pedir\u00e1 el/los s\u00edmbolo(s).\n\n"
                + "3) Marcar Inicial / Marcar Final: con el modo activo, haga clic sobre\n"
                + "   un estado para alternar la marca.\n\n"
                + "4) Mover: arrastre un estado para reubicarlo (las transiciones lo siguen).\n\n"
                + "5) Borrar: haga clic sobre un estado o una flecha para eliminarla.\n\n"
                + "6) Defina el Alfabeto y la Cadena en el panel derecho, presione\n"
                + "   'Verificar / Cargar Cadena' y use el panel inferior para reproducir\n"
                + "   la simulaci\u00f3n paso a paso.";
        JOptionPane.showMessageDialog(this, msg, "Ayuda", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------------------- Alfabeto ----------------------------

    private void saveAlphabet() {
        String text = alphabetField.getText();
        Set<Character> alphabet = new LinkedHashSet<>();
        StringBuilder discarded = new StringBuilder();
        for (String token : text.split("[,\\s]+")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            if (t.length() == 1) {
                alphabet.add(t.charAt(0));
            } else {
                discarded.append(t).append(' ');
            }
        }
        automaton.setAlphabet(alphabet);
        currentAlphabetLabel.setText("\u03A3 = " + formatSet(alphabet));
        if (discarded.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Se ignoraron s\u00edmbolos con m\u00e1s de 1 car\u00e1cter: " + discarded,
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String formatSet(Set<Character> set) {
        if (set.isEmpty()) return "{ }";
        StringBuilder sb = new StringBuilder("{ ");
        boolean first = true;
        for (Character c : set) {
            if (!first) sb.append(", ");
            sb.append(c);
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }

    // ---------------------------- Simulación ----------------------------

    private void loadSimulation() {
        stopTimer();
        if (automaton.getAlphabet().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero defina y guarde el alfabeto \u03A3.",
                    "Falta el alfabeto", JOptionPane.ERROR_MESSAGE);
            return;
        }
        State initial = automaton.getInitialState();
        if (initial == null) {
            JOptionPane.showMessageDialog(this, "Debe marcar un estado como INICIAL.",
                    "Falta estado inicial", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String input = stringField.getText();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!automaton.getAlphabet().contains(c)) {
                JOptionPane.showMessageDialog(this,
                        "El s\u00edmbolo '" + c + "' de la cadena no pertenece a \u03A3 " + formatSet(automaton.getAlphabet()),
                        "Cadena inv\u00e1lida", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        currentResult = Simulator.simulate(automaton, input);
        stepIndex = 0;
        simulationActive = true;
        drawingPanel.setFinishedColor(null);
        drawingPanel.setActiveEdge(null);
        drawingPanel.setCurrentSimState(currentResult.path.get(0));
        drawingPanel.repaint();
        playbackBar.setControlsEnabled(true);
        updateStatusLabels();
    }

    private boolean isFinished() {
        return currentResult != null && stepIndex >= currentResult.path.size() - 1;
    }

    private void wirePlayback() {
        playbackBar.getBtnRestart().addActionListener(e -> restart());
        playbackBar.getBtnStop().addActionListener(e -> stopSimulation());
        playbackBar.getBtnStepFwd().addActionListener(e -> { stopTimer(); stepForward(); });
        playbackBar.getBtnStepBack().addActionListener(e -> { stopTimer(); stepBackward(); });
        playbackBar.getBtnPlayPause().addActionListener(e -> togglePlay());
    }

    private void togglePlay() {
        if (!simulationActive) return;
        if (timer != null && timer.isRunning()) {
            stopTimer();
        } else {
            if (isFinished()) return;
            int velocidad = playbackBar.getSpeedSlider().getValue();
            int intervalMs = 1300 - velocidad * 110; // 1 lento .. 10 r\u00e1pido
            timer = new Timer(Math.max(150, intervalMs), e -> {
                stepForward();
                if (isFinished()) stopTimer();
            });
            timer.start();
            playbackBar.getBtnPlayPause().setText("\u23F8 Pausar");
        }
    }

    private void stopTimer() {
        if (timer != null) timer.stop();
        playbackBar.getBtnPlayPause().setText("\u25B6 Reproducir");
    }

    private void restart() {
        if (!simulationActive) return;
        stopTimer();
        stepIndex = 0;
        drawingPanel.setFinishedColor(null);
        drawingPanel.setActiveEdge(null);
        drawingPanel.setCurrentSimState(currentResult.path.get(0));
        drawingPanel.repaint();
        updateStatusLabels();
    }

    private void stopSimulation() {
        stopTimer();
        simulationActive = false;
        currentResult = null;
        stepIndex = 0;
        drawingPanel.clearSimulationHighlight();
        playbackBar.setControlsEnabled(false);
        updateStatusLabels();
    }

    private void stepForward() {
        if (!simulationActive || isFinished()) return;
        State from = currentResult.path.get(stepIndex);
        char symbol = currentResult.input.charAt(stepIndex);
        Transition edge = automaton.findTransition(from, symbol);
        drawingPanel.setActiveEdge(edge);

        stepIndex++;
        State to = currentResult.path.get(stepIndex);
        drawingPanel.setCurrentSimState(to);

        if (isFinished()) {
            applyVerdictColor();
            stopTimer();
        }
        drawingPanel.repaint();
        updateStatusLabels();
    }

    private void stepBackward() {
        if (!simulationActive || stepIndex <= 0) return;
        drawingPanel.setFinishedColor(null);
        stepIndex--;
        drawingPanel.setCurrentSimState(currentResult.path.get(stepIndex));
        if (stepIndex > 0) {
            State from = currentResult.path.get(stepIndex - 1);
            char symbol = currentResult.input.charAt(stepIndex - 1);
            drawingPanel.setActiveEdge(automaton.findTransition(from, symbol));
        } else {
            drawingPanel.setActiveEdge(null);
        }
        drawingPanel.repaint();
        updateStatusLabels();
    }

    private void applyVerdictColor() {
        switch (currentResult.verdict) {
            case ACCEPTED:
                drawingPanel.setFinishedColor(Palette.ACCEPT);
                break;
            case REJECTED:
            case STUCK:
                drawingPanel.setFinishedColor(Palette.REJECT);
                break;
            default:
                break;
        }
    }

    private void updateStatusLabels() {
        if (!simulationActive) {
            statusLabel.setText("<html>Sin simulaci\u00f3n activa</html>");
            resultLabel.setText(" ");
            resultLabel.setForeground(Palette.TEXT);
            return;
        }
        State current = currentResult.path.get(stepIndex);
        String input = currentResult.input;
        String consumed = input.substring(0, Math.min(stepIndex, input.length()));
        String remaining = stepIndex < input.length() ? input.substring(stepIndex) : "(consumida)";
        statusLabel.setText("<html>Cadena: <b>" + escapeHtml(input) + "</b><br>"
                + "Consumido: <b>" + escapeHtml(consumed) + "</b><br>"
                + "Restante: <b>" + escapeHtml(remaining) + "</b><br>"
                + "Estado actual: <b>" + current.getName() + "</b></html>");

        if (isFinished()) {
            switch (currentResult.verdict) {
                case ACCEPTED:
                    resultLabel.setText("CADENA ACEPTADA");
                    resultLabel.setForeground(Palette.ACCEPT);
                    break;
                case REJECTED:
                    resultLabel.setText("CADENA RECHAZADA");
                    resultLabel.setForeground(Palette.REJECT);
                    break;
                case STUCK:
                    resultLabel.setText("CADENA RECHAZADA (sin transici\u00f3n)");
                    resultLabel.setForeground(Palette.REJECT);
                    break;
                default:
                    break;
            }
        } else {
            resultLabel.setText("Simulando...");
            resultLabel.setForeground(Palette.CURRENT_STATE);
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
