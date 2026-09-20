package afdpainter.gui;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Lienzo de dibujo estilo "Paint" para construir el AFD a mano:
 * agregar estados, moverlos, conectarlos con transiciones curvas,
 * marcar estado inicial/final y borrar. También pinta el resaltado
 * de la simulación paso a paso.
 */
public class DrawingPanel extends JPanel {

    public static final int RADIUS = 34;

    private final Automaton automaton;
    private ToolBar.Mode mode = ToolBar.Mode.MOVE;

    // Estado de arrastre para mover nodos
    private State dragState;

    // Estado de creación de transición
    private State transitionSource;
    private Point tempPoint;

    // Resaltado de simulación
    private State currentSimState;
    private Transition activeEdge;
    private Color finishedColor; // verde/rojo cuando termina la simulación

    public DrawingPanel(Automaton automaton) {
        this.automaton = automaton;
        setBackground(Palette.CANVAS_BG);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handlePressed(e); }
            @Override public void mouseReleased(MouseEvent e) { handleReleased(e); }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) { handleDragged(e); }
        });
    }

    public void setMode(ToolBar.Mode mode) {
        this.mode = mode;
        transitionSource = null;
        tempPoint = null;
        repaint();
    }

    public Automaton getAutomaton() { return automaton; }

    // ---------- Resaltado de simulación (usado por el controlador de reproducción) ----------

    public void setCurrentSimState(State s) { this.currentSimState = s; }
    public void setActiveEdge(Transition t) { this.activeEdge = t; }
    public void setFinishedColor(Color c) { this.finishedColor = c; }

    public void clearSimulationHighlight() {
        currentSimState = null;
        activeEdge = null;
        finishedColor = null;
        repaint();
    }

    // ---------------------------- Manejo de mouse ----------------------------

    private void handlePressed(MouseEvent e) {
        Point p = e.getPoint();
        State clicked = automaton.findStateAt(p.x, p.y, RADIUS);

        switch (mode) {
            case ADD_STATE:
                if (clicked == null) {
                    State s = new State(automaton.nextStateName(), p.x, p.y);
                    if (automaton.getStates().isEmpty()) s.setInitial(true);
                    automaton.addState(s);
                    repaint();
                }
                break;
            case MOVE:
                dragState = clicked;
                break;
            case ADD_TRANSITION:
                if (clicked != null) {
                    transitionSource = clicked;
                    tempPoint = p;
                }
                break;
            case SET_INITIAL:
                if (clicked != null) {
                    automaton.setInitialState(clicked);
                    repaint();
                }
                break;
            case TOGGLE_FINAL:
                if (clicked != null) {
                    clicked.setFinalState(!clicked.isFinalState());
                    repaint();
                }
                break;
            case DELETE:
                if (clicked != null) {
                    automaton.removeState(clicked);
                    repaint();
                } else {
                    Transition t = findTransitionNear(p);
                    if (t != null) {
                        automaton.removeTransition(t);
                        repaint();
                    }
                }
                break;
        }
    }

    private void handleDragged(MouseEvent e) {
        Point p = e.getPoint();
        if (mode == ToolBar.Mode.MOVE && dragState != null) {
            dragState.setPosition(p.x, p.y);
            repaint();
        } else if (mode == ToolBar.Mode.ADD_TRANSITION && transitionSource != null) {
            tempPoint = p;
            repaint();
        }
    }

    private void handleReleased(MouseEvent e) {
        Point p = e.getPoint();
        if (mode == ToolBar.Mode.MOVE) {
            dragState = null;
        } else if (mode == ToolBar.Mode.ADD_TRANSITION && transitionSource != null) {
            State target = automaton.findStateAt(p.x, p.y, RADIUS);
            State source = transitionSource;
            transitionSource = null;
            tempPoint = null;
            if (target != null) {
                handleNewTransition(source, target);
            }
            repaint();
        }
    }

    private void handleNewTransition(State source, State target) {
        String input = JOptionPane.showInputDialog(this,
                "Símbolo(s) para la transición " + source.getName() + " \u2192 " + target.getName()
                        + " (separe con comas si hay varios, ej: 0,1):",
                "Nueva transición", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        input = input.trim();
        if (input.isEmpty()) return;

        List<Character> symbols = parseSymbols(input);
        Transition t = automaton.getOrCreateTransition(source, target);
        boolean createdNew = t.isEmpty();
        StringBuilder rejected = new StringBuilder();

        for (char c : symbols) {
            if (!automaton.getAlphabet().isEmpty() && !automaton.getAlphabet().contains(c)) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "El símbolo '" + c + "' no está en el alfabeto \u03A3. ¿Agregarlo?",
                        "Símbolo fuera del alfabeto", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    automaton.getAlphabet().add(c);
                } else {
                    continue;
                }
            }
            if (!automaton.isSymbolFree(source, c, t)) {
                rejected.append(c).append(' ');
                continue;
            }
            t.addSymbol(c);
        }

        if (t.isEmpty() && createdNew) {
            automaton.getTransitions().remove(t);
        }
        if (rejected.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Símbolo(s) '" + rejected.toString().trim() + "' ya usados en otra transición saliente de "
                            + source.getName() + ".\nUn AFD debe ser determinista (un solo destino por símbolo).",
                    "Conflicto de determinismo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private List<Character> parseSymbols(String input) {
        List<Character> result = new ArrayList<>();
        if (input.contains(",")) {
            for (String part : input.split(",")) {
                part = part.trim();
                if (!part.isEmpty()) result.add(part.charAt(0));
            }
        } else {
            for (char c : input.toCharArray()) {
                if (!Character.isWhitespace(c)) result.add(c);
            }
        }
        return result;
    }

    private Transition findTransitionNear(Point p) {
        double best = 10.0;
        Transition bestT = null;
        for (Transition t : automaton.getTransitions()) {
            double d = t.getFrom() == t.getTo() ? distanceToSelfLoop(t, p) : distanceToCurve(t, p);
            if (d < best) { best = d; bestT = t; }
        }
        return bestT;
    }

    private double distanceToCurve(Transition t, Point p) {
        QuadCurve2D curve = buildCurve(t);
        if (curve == null) return Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        for (double dt = 0; dt <= 1.0; dt += 0.05) {
            Point2D pt = pointOnQuad(curve, dt);
            double d = pt.distance(p.x, p.y);
            if (d < min) min = d;
        }
        return min;
    }

    private double distanceToSelfLoop(Transition t, Point p) {
        State s = t.getFrom();
        Ellipse2D loop = selfLoopEllipse(s);
        double cx = loop.getCenterX(), cy = loop.getCenterY();
        double rx = loop.getWidth() / 2.0, ry = loop.getHeight() / 2.0;
        double dx = p.x - cx, dy = p.y - cy;
        double angle = Math.atan2(dy, dx);
        double ex = cx + rx * Math.cos(angle);
        double ey = cy + ry * Math.sin(angle);
        return Point2D.distance(p.x, p.y, ex, ey);
    }

    // ---------------------------- Dibujo ----------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

        for (Transition t : automaton.getTransitions()) {
            if (t.getFrom() == t.getTo()) {
                drawSelfLoop(g2, t);
            } else {
                drawCurvedTransition(g2, t);
            }
        }

        if (mode == ToolBar.Mode.ADD_TRANSITION && transitionSource != null && tempPoint != null) {
            g2.setColor(Palette.TRANSITION);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{6f, 5f}, 0f));
            g2.drawLine(transitionSource.getX(), transitionSource.getY(), tempPoint.x, tempPoint.y);
        }

        for (State s : automaton.getStates()) {
            drawState(g2, s);
        }
    }

    private void drawState(Graphics2D g2, State s) {
        int r = RADIUS;
        int x = s.getX(), y = s.getY();

        // Flecha de estado inicial (entra desde la izquierda)
        if (s.isInitial()) {
            g2.setColor(Palette.STATE_BORDER);
            g2.setStroke(new BasicStroke(1.8f));
            int startX = x - r - 40;
            g2.drawLine(startX, y, x - r, y);
            drawArrowHead(g2, x - r, y, 0);
        }

        Color fill = Palette.STATE_FILL;
        Color border = Palette.STATE_BORDER;
        if (finishedColor != null && s == currentSimState) {
            fill = finishedColor;
        } else if (s == currentSimState) {
            fill = Palette.CURRENT_STATE;
        }

        g2.setColor(fill);
        g2.fillOval(x - r, y - r, r * 2, r * 2);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x - r, y - r, r * 2, r * 2);

        if (s.isFinalState()) {
            int inset = 6;
            g2.drawOval(x - r + inset, y - r + inset, (r - inset) * 2, (r - inset) * 2);
        }

        g2.setColor(Palette.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(s.getName());
        g2.drawString(s.getName(), x - tw / 2, y + fm.getAscent() / 2 - 2);
    }

    private QuadCurve2D buildCurve(Transition t) {
        State a = t.getFrom(), b = t.getTo();
        int ax = a.getX(), ay = a.getY(), bx = b.getX(), by = b.getY();
        double dx = bx - ax, dy = by - ay;
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return null;
        double ux = dx / dist, uy = dy / dist;
        double px = -uy, py = ux;

        int ia = automaton.getStates().indexOf(a);
        int ib = automaton.getStates().indexOf(b);
        double sign = ia < ib ? 1 : -1;
        double curveAmount = Math.max(30, dist * 0.18) * sign;

        double mx = (ax + bx) / 2.0 + px * curveAmount;
        double my = (ay + by) / 2.0 + py * curveAmount;

        Point2D start = pointOnCircleTowards(ax, ay, RADIUS, mx, my);
        Point2D end = pointOnCircleTowards(bx, by, RADIUS, mx, my);

        return new QuadCurve2D.Double(start.getX(), start.getY(), mx, my, end.getX(), end.getY());
    }

    private void drawCurvedTransition(Graphics2D g2, Transition t) {
        QuadCurve2D curve = buildCurve(t);
        if (curve == null) return;
        boolean active = t == activeEdge;

        g2.setColor(active ? Palette.ACTIVE_EDGE : Palette.TRANSITION);
        g2.setStroke(new BasicStroke(active ? 3.2f : 1.6f));
        g2.draw(curve);

        double angle = Math.atan2(curve.getY2() - curve.getCtrlY(), curve.getX2() - curve.getCtrlX());
        drawArrowHead(g2, curve.getX2(), curve.getY2(), angle);

        String label = t.getLabel();
        if (!label.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(label);
            g2.setColor(Palette.CANVAS_BG);
            g2.fillRect((int) curve.getCtrlX() - tw / 2 - 2, (int) curve.getCtrlY() - fm.getAscent() - 1,
                    tw + 4, fm.getHeight());
            g2.setColor(active ? Palette.ACTIVE_EDGE : Palette.TEXT);
            g2.drawString(label, (int) curve.getCtrlX() - tw / 2, (int) curve.getCtrlY());
        }
    }

    private Ellipse2D selfLoopEllipse(State s) {
        int loopW = 56, loopH = 44;
        int cx = s.getX();
        int topY = s.getY() - RADIUS;
        return new Ellipse2D.Double(cx - loopW / 2.0, topY - loopH + 10, loopW, loopH);
    }

    private void drawSelfLoop(Graphics2D g2, Transition t) {
        boolean active = t == activeEdge;
        Ellipse2D loop = selfLoopEllipse(t.getFrom());

        g2.setColor(active ? Palette.ACTIVE_EDGE : Palette.TRANSITION);
        g2.setStroke(new BasicStroke(active ? 3.2f : 1.6f));
        g2.draw(loop);

        double tipX = loop.getMaxX() - 6;
        double tipY = loop.getCenterY() + loop.getHeight() * 0.28;
        drawArrowHead(g2, tipX, tipY, Math.toRadians(70));

        String label = t.getLabel();
        if (!label.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(label);
            g2.setColor(active ? Palette.ACTIVE_EDGE : Palette.TEXT);
            g2.drawString(label, (int) loop.getCenterX() - tw / 2, (int) loop.getMinY() - 4);
        }
    }

    private void drawArrowHead(Graphics2D g2, double tipX, double tipY, double angle) {
        int len = 11;
        double a1 = angle + Math.toRadians(150);
        double a2 = angle - Math.toRadians(150);
        Path2D p = new Path2D.Double();
        p.moveTo(tipX, tipY);
        p.lineTo(tipX + len * Math.cos(a1), tipY + len * Math.sin(a1));
        p.moveTo(tipX, tipY);
        p.lineTo(tipX + len * Math.cos(a2), tipY + len * Math.sin(a2));
        g2.draw(p);
    }

    private Point2D pointOnCircleTowards(int cx, int cy, int r, double towardX, double towardY) {
        double dx = towardX - cx, dy = towardY - cy;
        double d = Math.hypot(dx, dy);
        if (d < 0.0001) return new Point2D.Double(cx, cy);
        return new Point2D.Double(cx + dx / d * r, cy + dy / d * r);
    }

    private Point2D pointOnQuad(QuadCurve2D c, double t) {
        double x = (1 - t) * (1 - t) * c.getX1() + 2 * (1 - t) * t * c.getCtrlX() + t * t * c.getX2();
        double y = (1 - t) * (1 - t) * c.getY1() + 2 * (1 - t) * t * c.getCtrlY() + t * t * c.getY2();
        return new Point2D.Double(x, y);
    }
}
