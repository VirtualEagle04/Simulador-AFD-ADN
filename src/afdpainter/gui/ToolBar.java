package afdpainter.gui;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.FlowLayout;

/**
 * Barra de herramientas con los modos de edición estilo "Paint".
 */
public class ToolBar extends JPanel {

    public enum Mode { MOVE, ADD_STATE, ADD_TRANSITION, SET_INITIAL, TOGGLE_FINAL, DELETE }

    public interface ModeListener { void onModeChanged(Mode m); }
    public interface ClearListener { void onClear(); }

    private final ButtonGroup group = new ButtonGroup();
    private Mode currentMode = Mode.MOVE;
    private ModeListener modeListener;
    private ClearListener clearListener;

    public ToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
        addModeButton("Mover", Mode.MOVE, true);
        addModeButton("+ Estado", Mode.ADD_STATE, false);
        addModeButton("+ Transición", Mode.ADD_TRANSITION, false);
        addModeButton("Marcar Inicial", Mode.SET_INITIAL, false);
        addModeButton("Alternar Final", Mode.TOGGLE_FINAL, false);
        addModeButton("Eliminar", Mode.DELETE, false);

        JButton clear = new JButton("Limpiar Todo");
        clear.addActionListener(e -> {
            if (clearListener != null) clearListener.onClear();
        });
        add(clear);
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
    public Mode getCurrentMode() { return currentMode; }
}
