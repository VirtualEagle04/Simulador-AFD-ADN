package afdpainter.gui;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;
import afdpainter.sim.Simulator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainFrame extends JFrame {

    private final Automaton automaton = new Automaton();
    private final DrawingPanel drawingPanel = new DrawingPanel(automaton);
    private final ToolBar toolBar = new ToolBar();
    private final PlaybackBar playbackBar = new PlaybackBar();

    private final JTextField alphabetField = new JTextField("0,1", 8);
    private final JTextField stringField = new JTextField(16);
    private final JLabel resultLabel = new JLabel(" ");

    private Simulator.Result currentResult;
    private int stepIndex;
    private Timer timer;
    private boolean playing = false;

    public MainFrame() {
        super("Editor y Simulador de AFD");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        toolBar.setModeListener(drawingPanel::setMode);
        toolBar.setClearListener(this::onClear);
        add(toolBar, BorderLayout.NORTH);

        add(new JScrollPane(drawingPanel), BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new java.awt.GridLayout(2, 1));
        south.add(buildInputPanel());
        south.add(playbackBar);
        add(south, BorderLayout.SOUTH);

        wirePlayback();
    }

    private JPanel buildInputPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        p.add(new JLabel("Alfabeto \u03A3:"));
        p.add(alphabetField);
        JButton applyAlphabet = new JButton("Aplicar Alfabeto");
        applyAlphabet.addActionListener(e -> applyAlphabet());
        p.add(applyAlphabet);

        p.add(new JLabel("   Cadena:"));
        p.add(stringField);
        JButton validate = new JButton("Validar / Ejecutar");
        validate.addActionListener(e -> validateAndRun());
        p.add(validate);

        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD));
        p.add(resultLabel);
        return p;
    }

    private void onClear() {
        int opt = JOptionPane.showConfirmDialog(this, "¿Borrar todo el autómata dibujado?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            stopPlayback();
            automaton.clear();
            drawingPanel.clearSimulationHighlight();
            resultLabel.setText(" ");
            playbackBar.setControlsEnabled(false);
            playbackBar.setProgressText("Sin simulación");
            drawingPanel.repaint();
        }
    }

    private Set<Character> parseSymbolSet(String text) {
        Set<Character> set = new LinkedHashSet<>();
        text = text.trim();
        if (text.isEmpty()) return set;
        if (text.contains(",")) {
            for (String part : text.split(",")) {
                part = part.trim();
                if (!part.isEmpty()) set.add(part.charAt(0));
            }
        } else {
            for (char c : text.toCharArray()) {
                if (!Character.isWhitespace(c)) set.add(c);
            }
        }
        return set;
    }

    private void applyAlphabet() {
        Set<Character> alphabet = parseSymbolSet(alphabetField.getText());
        automaton.setAlphabet(alphabet);
        JOptionPane.showMessageDialog(this, "Alfabeto establecido: " + alphabet,
                "Alfabeto", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------------------- Validación + simulación ----------------------------

    private void validateAndRun() {
        stopPlayback();
        drawingPanel.clearSimulationHighlight();

        if (automaton.getAlphabet().isEmpty()) {
            Set<Character> alphabet = parseSymbolSet(alphabetField.getText());
            automaton.setAlphabet(alphabet);
        }

        if (automaton.getInitialState() == null) {
            resultLabel.setText("Debe marcar un estado inicial.");
            playbackBar.setControlsEnabled(false);
            return;
        }

        String input = stringField.getText().trim();
        Set<Character> alphabet = automaton.getAlphabet();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!alphabet.contains(c)) {
                resultLabel.setText("Símbolo inválido '" + c + "' (no pertenece a \u03A3).");
                playbackBar.setControlsEnabled(false);
                return;
            }
        }

        currentResult = Simulator.simulate(automaton, input);
        stepIndex = 0;
        drawingPanel.setCurrentSimState(currentResult.path.get(0));
        drawingPanel.repaint();
        playbackBar.setControlsEnabled(true);
        resultLabel.setText("Listo para reproducir.");
        updateProgressLabel();
    }

    // ---------------------------- Reproducción "multimedia" ----------------------------

    private void wirePlayback() {
        playbackBar.getBtnStart().addActionListener(e -> jumpToStart());
        playbackBar.getBtnStop().addActionListener(e -> jumpToStart());
        playbackBar.getBtnStepFwd().addActionListener(e -> { pauseTimer(); stepForward(); });
        playbackBar.getBtnStepBack().addActionListener(e -> { pauseTimer(); stepBackward(); });
        playbackBar.getBtnPlayPause().addActionListener(e -> togglePlay());
    }

    private void togglePlay() {
        if (currentResult == null) return;
        if (playing) {
            pauseTimer();
        } else {
            if (stepIndex >= currentResult.path.size() - 1) return; // ya terminó
            playing = true;
            playbackBar.getBtnPlayPause().setText("\u23F8");
            int delay = playbackBar.getSpeedSlider().getValue();
            timer = new Timer(delay, e -> stepForward());
            timer.start();
        }
    }

    private void pauseTimer() {
        if (timer != null) timer.stop();
        playing = false;
        playbackBar.getBtnPlayPause().setText("\u25B6");
    }

    private void stopPlayback() {
        pauseTimer();
    }

    private void jumpToStart() {
        if (currentResult == null) return;
        pauseTimer();
        stepIndex = 0;
        drawingPanel.setFinishedColor(null);
        drawingPanel.setActiveEdge(null);
        drawingPanel.setCurrentSimState(currentResult.path.get(0));
        drawingPanel.repaint();
        resultLabel.setText("Listo para reproducir.");
        updateProgressLabel();
    }

    private void stepForward() {
        if (currentResult == null) return;
        int lastIndex = currentResult.path.size() - 1;
        if (stepIndex >= lastIndex) {
            pauseTimer();
            showVerdict();
            return;
        }
        State from = currentResult.path.get(stepIndex);
        char symbol = currentResult.input.charAt(stepIndex);
        Transition edge = automaton.findTransition(from, symbol);
        drawingPanel.setActiveEdge(edge);

        stepIndex++;
        State to = currentResult.path.get(stepIndex);
        drawingPanel.setCurrentSimState(to);
        drawingPanel.repaint();
        updateProgressLabel();

        if (stepIndex == lastIndex) {
            pauseTimer();
            showVerdict();
        }
    }

    private void stepBackward() {
        if (currentResult == null || stepIndex <= 0) return;
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
        resultLabel.setText("Listo para reproducir.");
        updateProgressLabel();
    }

    private void showVerdict() {
        State last = currentResult.path.get(stepIndex);
        switch (currentResult.verdict) {
            case ACCEPTED:
                drawingPanel.setFinishedColor(Palette.ACCEPT);
                resultLabel.setText("CADENA ACEPTADA \u2713 (termina en estado final " + last.getName() + ")");
                break;
            case REJECTED:
                drawingPanel.setFinishedColor(Palette.REJECT);
                resultLabel.setText("CADENA RECHAZADA (termina en " + last.getName() + ", no es final)");
                break;
            case STUCK:
                drawingPanel.setFinishedColor(Palette.REJECT);
                resultLabel.setText("CADENA RECHAZADA: sin transición para '"
                        + currentResult.input.charAt(currentResult.stuckAtIndex) + "' desde " + last.getName());
                break;
            default:
                break;
        }
        drawingPanel.repaint();
    }

    private void updateProgressLabel() {
        String input = currentResult.input;
        String consumed = input.substring(0, Math.min(stepIndex, input.length()));
        String remaining = input.length() >= stepIndex ? input.substring(stepIndex) : "";
        State cur = currentResult.path.get(stepIndex);
        playbackBar.setProgressText("<html>Consumido: <b>" + escape(consumed) + "</b> | Restante: "
                + escape(remaining) + "  &nbsp; (estado actual: " + cur.getName() + ")</html>");
    }

    private String escape(String s) {
        return s.isEmpty() ? "\u2205" : s;
    }
}
