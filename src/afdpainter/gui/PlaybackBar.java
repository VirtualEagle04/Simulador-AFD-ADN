package afdpainter.gui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.FlowLayout;

/**
 * Panel "multimedia" para reproducir la simulación paso a paso:
 * inicio, retroceder, play/pausa, avanzar, detener, y velocidad.
 */
public class PlaybackBar extends JPanel {

    private final JButton btnStart = new JButton("|\u25C0\u25C0");
    private final JButton btnStepBack = new JButton("\u25C0");
    private final JButton btnPlayPause = new JButton("\u25B6");
    private final JButton btnStepFwd = new JButton("\u25B6");
    private final JButton btnStop = new JButton("\u25A0");
    private final JSlider speedSlider = new JSlider(200, 2000, 800);
    private final JLabel progressLabel = new JLabel("Sin simulación");

    public PlaybackBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        add(new JLabel("Reproducción:"));
        add(btnStart);
        add(btnStepBack);
        add(btnPlayPause);
        add(btnStepFwd);
        add(btnStop);
        add(new JLabel("  Velocidad:"));
        speedSlider.setInverted(true);
        speedSlider.setPreferredSize(new java.awt.Dimension(120, speedSlider.getPreferredSize().height));
        add(speedSlider);
        add(new JLabel("   "));
        add(progressLabel);
        setControlsEnabled(false);
    }

    public JButton getBtnStart() { return btnStart; }
    public JButton getBtnStepBack() { return btnStepBack; }
    public JButton getBtnPlayPause() { return btnPlayPause; }
    public JButton getBtnStepFwd() { return btnStepFwd; }
    public JButton getBtnStop() { return btnStop; }
    public JSlider getSpeedSlider() { return speedSlider; }

    public void setProgressText(String html) { progressLabel.setText(html); }

    public void setControlsEnabled(boolean b) {
        btnStart.setEnabled(b);
        btnStepBack.setEnabled(b);
        btnPlayPause.setEnabled(b);
        btnStepFwd.setEnabled(b);
        btnStop.setEnabled(b);
    }
}
