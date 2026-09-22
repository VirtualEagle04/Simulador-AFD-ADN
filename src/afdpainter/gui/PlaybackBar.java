package afdpainter.gui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class PlaybackBar extends JPanel {

    private final JButton btnRestart = new JButton("\u23EE Reiniciar");
    private final JButton btnStepBack = new JButton("\u25C0 Atr\u00e1s");
    private final JButton btnPlayPause = new JButton("\u25B6 Reproducir");
    private final JButton btnStepFwd = new JButton("Siguiente \u25B6");
    private final JButton btnStop = new JButton("\u23F9 Detener");
    private final JSlider speedSlider = new JSlider(1, 10, 5);

    public PlaybackBar() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 8));
        add(btnRestart);
        add(btnStepBack);
        add(btnPlayPause);
        add(btnStepFwd);
        add(btnStop);
        add(new JLabel("   Velocidad:"));
        speedSlider.setPreferredSize(new Dimension(140, speedSlider.getPreferredSize().height));
        add(speedSlider);
        setControlsEnabled(false);
    }

    public JButton getBtnRestart() { return btnRestart; }
    public JButton getBtnStepBack() { return btnStepBack; }
    public JButton getBtnPlayPause() { return btnPlayPause; }
    public JButton getBtnStepFwd() { return btnStepFwd; }
    public JButton getBtnStop() { return btnStop; }
    public JSlider getSpeedSlider() { return speedSlider; }

    public void setControlsEnabled(boolean b) {
        btnRestart.setEnabled(b);
        btnStepBack.setEnabled(b);
        btnPlayPause.setEnabled(b);
        btnStepFwd.setEnabled(b);
        btnStop.setEnabled(b);
    }
}
