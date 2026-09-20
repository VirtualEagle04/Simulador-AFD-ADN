package afdpainter.gui;

import afdpainter.model.Automaton;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;

/**
 * Barra de herramientas con los modos de edición estilo "Paint" (dibujo
 * a mano alzada para estados/transiciones, clic para marcar/borrar).
 */
public class ToolBar extends JPanel {

    public enum Mode { MOVE, ADD_STATE, ADD_TRANSITION, SET_INITIAL, TOGGLE_FINAL, DELETE }

    public interface ModeListener { void onModeChanged(Mode m); }
    public interface ClearListener { void onClear(); }
    public interface UndoListener { void onUndo(); }
    public interface HelpListener { void onHelp(); }
    public interface KindListener { void onKindChanged(Automaton.Kind kind); }
    public interface ConvertListener { void onConvert(); }

    private final ButtonGroup group = new ButtonGroup();
    private final ButtonGroup kindGroup = new ButtonGroup();
    private Mode currentMode = Mode.ADD_STATE;
    private ModeListener modeListener;
    private ClearListener clearListener;
    private UndoListener undoListener;
    private HelpListener helpListener;
    private KindListener kindListener;
    private ConvertListener convertListener;

    private final JToggleButton dfaBtn = new JToggleButton("AFD", true);
    private final JToggleButton nfaBtn = new JToggleButton("AFN");
    private final JButton convertBtn = new JButton("Convertir AFN \u2192 AFD");

    public ToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));

        JLabel kindLabel = new JLabel("Modo:");
        add(kindLabel);
        kindGroup.add(dfaBtn);
        kindGroup.add(nfaBtn);
        dfaBtn.addActionListener(e -> {
            if (kindListener != null) kindListener.onKindChanged(Automaton.Kind.DFA);
            convertBtn.setEnabled(false);
        });
        nfaBtn.addActionListener(e -> {
            if (kindListener != null) kindListener.onKindChanged(Automaton.Kind.NFA);
            convertBtn.setEnabled(true);
        });
        add(dfaBtn);
        add(nfaBtn);

        convertBtn.setEnabled(false);
        convertBtn.addActionListener(e -> { if (convertListener != null) convertListener.onConvert(); });
        add(convertBtn);

        add(new JSeparator(SwingConstants.VERTICAL));

        addModeButton("\u270F Dibujar Estado", Mode.ADD_STATE, true);
        addModeButton("\u2794 Dibujar Transici\u00f3n", Mode.ADD_TRANSITION, false);
        addModeButton("\u2192 Marcar Inicial", Mode.SET_INITIAL, false);
        addModeButton("\u25CE Marcar Final", Mode.TOGGLE_FINAL, false);
        addModeButton("\u270B Mover", Mode.MOVE, false);
        addModeButton("\uD83D\uDDD1 Borrar", Mode.DELETE, false);

        JButton undo = new JButton("\u21B6 Deshacer");
        undo.addActionListener(e -> { if (undoListener != null) undoListener.onUndo(); });
        add(undo);

        JButton clear = new JButton("Limpiar Todo");
        clear.addActionListener(e -> { if (clearListener != null) clearListener.onClear(); });
        add(clear);

        JButton help = new JButton("? Ayuda");
        help.addActionListener(e -> { if (helpListener != null) helpListener.onHelp(); });
        add(help);
    }

    /** Selecciona el toggle de modo (AFD/AFN) sin disparar el listener (uso programático). */
    public void selectKind(Automaton.Kind kind) {
        if (kind == Automaton.Kind.NFA) {
            nfaBtn.setSelected(true);
            convertBtn.setEnabled(true);
        } else {
            dfaBtn.setSelected(true);
            convertBtn.setEnabled(false);
        }
    }

    private void addModeButton(String text, Mode m, boolean selected) {
        JToggleButton b = new JToggleButton(text, selected);
        b.addActionListener(e -> {
            currentMode = m;
            if (modeListener != null) modeListener.onModeChanged(m);
        });
        group.add(b);
        add(b);
    }

    public void setModeListener(ModeListener l) { this.modeListener = l; }
    public void setClearListener(ClearListener l) { this.clearListener = l; }
    public void setUndoListener(UndoListener l) { this.undoListener = l; }
    public void setHelpListener(HelpListener l) { this.helpListener = l; }
    public void setKindListener(KindListener l) { this.kindListener = l; }
    public void setConvertListener(ConvertListener l) { this.convertListener = l; }
    public Mode getCurrentMode() { return currentMode; }
}
