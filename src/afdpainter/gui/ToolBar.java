package afdpainter.gui;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
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

    private final ButtonGroup group = new ButtonGroup();
    private Mode currentMode = Mode.ADD_STATE;
    private ModeListener modeListener;
    private ClearListener clearListener;
    private UndoListener undoListener;
    private HelpListener helpListener;

    public ToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
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
    public Mode getCurrentMode() { return currentMode; }
}
