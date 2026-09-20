package afdpainter.gui;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lienzo de dibujo LIBRE (a mano alzada, como un lápiz): el usuario
 * mantiene el mouse presionado mientras lo mueve para trazar la figura
 * de un estado (un contorno cerrado) o de una transición (una curva
 * entre dos estados, o un lazo sobre el mismo estado).
 *
 * La geometría (arcos, self-loops, flechas rellenas, etiquetas) sigue
 * el mismo esquema de referencia: el lado del arco entre dos estados se
 * decide de forma canónica según el orden de los estados en la lista,
 * así A->B y B->A jamás quedan sobrepuestas; los self-loops usan el
 * ángulo/tamaño derivados del propio trazo dibujado.
 *
 * Marcar estado inicial/final y eliminar siguen siendo un clic (son
 * banderas, no figuras que tenga sentido "dibujar"). El primer estado
 * que se dibuja se marca automáticamente como inicial.
 */
public class DrawingPanel extends JPanel {

    private final Automaton automaton;
    private ToolBar.Mode mode = ToolBar.Mode.ADD_STATE;

    private State dragState;
    private List<Point> stroke;
    private State hoverState;

    private State currentSimState;
    private Transition activeEdge;
    private Color finishedColor;

    private final Deque<Runnable> undoStack = new ArrayDeque<>();

    public DrawingPanel(Automaton automaton) {
        this.automaton = automaton;
        setBackground(Palette.CANVAS_BG);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handlePressed(e); }
            @Override public void mouseReleased(MouseEvent e) { handleReleased(e); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    State s = automaton.findStateAt(e.getX(), e.getY());
                    if (s != null) renameState(s);
                }
            }
            @Override public void mouseExited(MouseEvent e) { hoverState = null; repaint(); }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) { handleDragged(e); }
            @Override public void mouseMoved(MouseEvent e) { handleMoved(e); }
        });
    }

    public void setMode(ToolBar.Mode mode) {
        this.mode = mode;
        stroke = null;
        dragState = null;
        repaint();
    }

    public Automaton getAutomaton() { return automaton; }

    public void undo() {
        if (!undoStack.isEmpty()) {
            undoStack.pop().run();
            repaint();
        }
    }

    // ---------- Resaltado de simulación ----------

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
        double x = e.getX(), y = e.getY();
        switch (mode) {
            case ADD_STATE:
            case ADD_TRANSITION:
                stroke = new ArrayList<>();
                stroke.add(e.getPoint());
                break;
            case SET_INITIAL: {
                State s = automaton.findStateAt(x, y);
                if (s != null) {
                    automaton.setInitialState(s);
                    repaint();
                }
                break;
            }
            case TOGGLE_FINAL: {
                State s = automaton.findStateAt(x, y);
                if (s != null) {
                    s.setFinalState(!s.isFinalState());
                    repaint();
                }
                break;
            }
            case MOVE:
                dragState = automaton.findStateAt(x, y);
                break;
            case DELETE: {
                State s = automaton.findStateAt(x, y);
                if (s != null) {
                    int r = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
                            "\u00bfBorrar el estado " + s.getName() + " y sus transiciones?",
                            "Confirmar", JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.YES_OPTION) {
                        automaton.removeState(s);
                        repaint();
                    }
                } else {
                    Transition t = findTransitionNear(x, y);
                    if (t != null) {
                        automaton.removeTransition(t);
                        repaint();
                    }
                }
                break;
            }
        }
    }

    private void handleDragged(MouseEvent e) {
        double x = e.getX(), y = e.getY();
        if (mode == ToolBar.Mode.MOVE && dragState != null) {
            dragState.setPosition(x, y);
            repaint();
        } else if (stroke != null && (mode == ToolBar.Mode.ADD_STATE || mode == ToolBar.Mode.ADD_TRANSITION)) {
            Point last = stroke.get(stroke.size() - 1);
            if (Math.hypot(x - last.x, y - last.y) > 2.0) {
                stroke.add(e.getPoint());
                repaint();
            }
        }
    }

    private void handleReleased(MouseEvent e) {
        if (mode == ToolBar.Mode.ADD_STATE) {
            if (stroke != null) finalizeNewState(stroke);
        } else if (mode == ToolBar.Mode.ADD_TRANSITION) {
            if (stroke != null) finalizeNewTransition(stroke);
        } else if (mode == ToolBar.Mode.MOVE) {
            dragState = null;
        }
        stroke = null;
        repaint();
    }

    private void handleMoved(MouseEvent e) {
        State s = automaton.findStateAt(e.getX(), e.getY());
        if (s != hoverState) {
            hoverState = s;
            repaint();
        }
    }

    private void renameState(State s) {
        String newName = JOptionPane.showInputDialog(this, "Nuevo nombre para el estado:", s.getName());
        if (newName != null) {
            newName = newName.trim();
            if (!newName.isEmpty()) s.setName(newName);
            repaint();
        }
    }

    // ---------------------------- Creación a partir del trazo ----------------------------

    private void finalizeNewState(List<Point> pts) {
        if (pts.size() < 3) return;
        double sx = 0, sy = 0;
        for (Point p : pts) { sx += p.x; sy += p.y; }
        double cx = sx / pts.size(), cy = sy / pts.size();
        double sumD = 0;
        for (Point p : pts) sumD += Math.hypot(p.x - cx, p.y - cy);
        double radius = Math.max(30, Math.min(110, sumD / pts.size()));

        GeneralPath outline = new GeneralPath();
        Point first = pts.get(0);
        outline.moveTo(first.x - cx, first.y - cy);
        for (int i = 1; i < pts.size(); i++) {
            Point p = pts.get(i);
            outline.lineTo(p.x - cx, p.y - cy);
        }
        outline.closePath();

        State s = new State(automaton.nextStateName(), cx, cy);
        s.setOutline(outline);
        s.setRadius(radius);
        if (automaton.getStates().isEmpty()) s.setInitial(true); // primer estado = inicial automático
        automaton.addState(s);
        undoStack.push(() -> automaton.removeState(s));
    }

    private List<Character> askSymbols(State from, State to) {
        String prompt = "S\u00edmbolo(s) para la transici\u00f3n " + from.getName() + " \u2192 " + to.getName()
                + (from == to ? " (auto-lazo)" : "")
                + "\n(separe con comas si son varios, ej: a,b)";
        String text = JOptionPane.showInputDialog(this, prompt, "Nueva transici\u00f3n", JOptionPane.QUESTION_MESSAGE);
        if (text == null) return null;

        List<Character> result = new ArrayList<>();
        StringBuilder longs = new StringBuilder();
        StringBuilder conflicts = new StringBuilder();
        for (String tok : text.split(",")) {
            String tt = tok.trim();
            if (tt.isEmpty()) continue;
            if (tt.length() != 1) { longs.append(tt).append(' '); continue; }
            char c = tt.charAt(0);
            boolean conflict = false;
            for (Transition t : automaton.getTransitions()) {
                if (t.getFrom() == from && t.getTo() != to && t.getSymbols().contains(c)) { conflict = true; break; }
            }
            if (conflict) { conflicts.append(c).append(' '); continue; }
            if (!result.contains(c)) result.add(c);
        }
        if (longs.length() > 0) {
            JOptionPane.showMessageDialog(this, "Se ignoraron s\u00edmbolos con m\u00e1s de 1 car\u00e1cter: " + longs,
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
        if (conflicts.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Un AFD debe ser determinista: [" + conflicts + "] ya sale de " + from.getName()
                            + " hacia otro estado distinto. Se ignoraron.",
                    "Conflicto de determinismo", JOptionPane.WARNING_MESSAGE);
        }
        return result;
    }

    private void finalizeNewTransition(List<Point> pts) {
        if (pts.size() < 2) return;
        Point start = pts.get(0);
        Point end = pts.get(pts.size() - 1);
        State from = automaton.findStateAt(start.x, start.y);
        State to = automaton.findStateAt(end.x, end.y);
        if (from == null || to == null) {
            JOptionPane.showMessageDialog(this, "La transici\u00f3n debe iniciar y terminar sobre un estado.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (from == to) {
            double maxD = -1;
            Point farthest = null;
            for (Point p : pts) {
                double d = Math.hypot(p.x - from.getX(), p.y - from.getY());
                if (d > maxD) { maxD = d; farthest = p; }
            }
            double angle = farthest != null
                    ? Math.atan2(farthest.y - from.getY(), farthest.x - from.getX())
                    : -Math.PI / 2;
            double size = Math.max(22, Math.min(70, maxD - from.getRadius()));

            Transition existing = null;
            for (Transition t : automaton.getTransitions()) {
                if (t.isLoop() && t.getFrom() == from) { existing = t; break; }
            }

            List<Character> symbols = askSymbols(from, from);
            if (symbols == null || symbols.isEmpty()) return;

            if (existing != null) {
                for (char c : symbols) existing.addSymbol(c);
            } else {
                Transition t = new Transition(from, from);
                t.setLoopAngle(angle);
                t.setLoopSize(size);
                for (char c : symbols) t.addSymbol(c);
                automaton.getTransitions().add(t);
                undoStack.push(() -> automaton.getTransitions().remove(t));
            }
        } else {
            Transition existing = automaton.getTransitionBetween(from, to);
            List<Character> symbols = askSymbols(from, to);
            if (symbols == null || symbols.isEmpty()) return;

            if (existing != null) {
                for (char c : symbols) existing.addSymbol(c);
            } else {
                double ax = from.getX(), ay = from.getY(), bx = to.getX(), by = to.getY();
                double vx = bx - ax, vy = by - ay;
                double vlen = Math.max(1.0, Math.hypot(vx, vy));
                double uxp = -vy / vlen, uyp = vx / vlen;
                double maxPerp = 0;
                for (Point p : pts) {
                    double wx = p.x - ax, wy = p.y - ay;
                    double perp = Math.abs(wx * uxp + wy * uyp);
                    if (perp > maxPerp) maxPerp = perp;
                }
                double bow = Math.max(32, Math.min(140, maxPerp));

                Transition t = new Transition(from, to);
                t.setBow(bow);
                for (char c : symbols) t.addSymbol(c);
                automaton.getTransitions().add(t);
                undoStack.push(() -> automaton.getTransitions().remove(t));
            }
        }
    }

    // ---------------------------- Geometría (arcos, lazos, hit-testing) ----------------------------

    private Point2D borderTowards(State e, double tx, double ty) {
        double dx = tx - e.getX(), dy = ty - e.getY();
        double d = Math.hypot(dx, dy);
        if (d < 1e-6) return new Point2D.Double(e.getX(), e.getY() - e.getRadius());
        return new Point2D.Double(e.getX() + dx / d * e.getRadius(), e.getY() + dy / d * e.getRadius());
    }

    /**
     * Punto de control del arco entre dos estados distintos. El orden
     * canónico (según índice en la lista de estados) decide hacia qué
     * lado se abomba, para que A->B y B->A queden en lados opuestos.
     */
    private Point2D arcControlPoint(Transition t) {
        List<State> states = automaton.getStates();
        int iFrom = states.indexOf(t.getFrom());
        int iTo = states.indexOf(t.getTo());
        State p, q;
        double sign;
        if (iFrom < iTo) { p = t.getFrom(); q = t.getTo(); sign = 1.0; }
        else { p = t.getTo(); q = t.getFrom(); sign = -1.0; }

        double vx = q.getX() - p.getX(), vy = q.getY() - p.getY();
        double len = Math.max(1.0, Math.hypot(vx, vy));
        double px = -vy / len, py = vx / len;

        double midx = (p.getX() + q.getX()) / 2.0;
        double midy = (p.getY() + q.getY()) / 2.0;

        double bow = Math.max(t.getBow(), 24);
        return new Point2D.Double(midx + px * bow * sign, midy + py * bow * sign);
    }

    private Point2D[] arcPoints(Transition t) {
        Point2D ctrl = arcControlPoint(t);
        Point2D start = borderTowards(t.getFrom(), ctrl.getX(), ctrl.getY());
        Point2D end = borderTowards(t.getTo(), ctrl.getX(), ctrl.getY());
        return new Point2D[]{start, ctrl, end};
    }

    private Point2D[] loopPoints(Transition t) {
        State e = t.getFrom();
        double spread = Math.toRadians(24);
        double aOut = t.getLoopAngle() - spread;
        double aIn = t.getLoopAngle() + spread;

        Point2D p0 = new Point2D.Double(e.getX() + e.getRadius() * Math.cos(aOut), e.getY() + e.getRadius() * Math.sin(aOut));
        Point2D p3 = new Point2D.Double(e.getX() + e.getRadius() * Math.cos(aIn), e.getY() + e.getRadius() * Math.sin(aIn));

        double distCtrl = e.getRadius() + t.getLoopSize() * 1.4;
        Point2D p1 = new Point2D.Double(e.getX() + distCtrl * Math.cos(aOut), e.getY() + distCtrl * Math.sin(aOut));
        Point2D p2 = new Point2D.Double(e.getX() + distCtrl * Math.cos(aIn), e.getY() + distCtrl * Math.sin(aIn));

        return new Point2D[]{p0, p1, p2, p3};
    }

    private Point2D pointOnQuad(Point2D p0, Point2D p1, Point2D p2, double t) {
        double x = (1 - t) * (1 - t) * p0.getX() + 2 * (1 - t) * t * p1.getX() + t * t * p2.getX();
        double y = (1 - t) * (1 - t) * p0.getY() + 2 * (1 - t) * t * p1.getY() + t * t * p2.getY();
        return new Point2D.Double(x, y);
    }

    private Point2D pointOnCubic(Point2D p0, Point2D p1, Point2D p2, Point2D p3, double t) {
        double u = 1 - t;
        double x = u * u * u * p0.getX() + 3 * u * u * t * p1.getX() + 3 * u * t * t * p2.getX() + t * t * t * p3.getX();
        double y = u * u * u * p0.getY() + 3 * u * u * t * p1.getY() + 3 * u * t * t * p2.getY() + t * t * t * p3.getY();
        return new Point2D.Double(x, y);
    }

    private double distanceToTransition(double x, double y, Transition t) {
        double minD = Double.MAX_VALUE;
        int steps = 24;
        if (t.isLoop()) {
            Point2D[] p = loopPoints(t);
            for (int i = 0; i <= steps; i++) {
                Point2D pt = pointOnCubic(p[0], p[1], p[2], p[3], i / (double) steps);
                minD = Math.min(minD, Math.hypot(pt.getX() - x, pt.getY() - y));
            }
        } else {
            Point2D[] p = arcPoints(t);
            for (int i = 0; i <= steps; i++) {
                Point2D pt = pointOnQuad(p[0], p[1], p[2], i / (double) steps);
                minD = Math.min(minD, Math.hypot(pt.getX() - x, pt.getY() - y));
            }
        }
        return minD;
    }

    private Transition findTransitionNear(double x, double y) {
        Transition best = null;
        double bestD = 10;
        for (Transition t : automaton.getTransitions()) {
            double d = distanceToTransition(x, y, t);
            if (d < bestD) { bestD = d; best = t; }
        }
        return best;
    }

    // ---------------------------- Dibujo ----------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

        for (Transition t : automaton.getTransitions()) {
            drawTransition(g2, t);
        }
        for (State s : automaton.getStates()) {
            drawState(g2, s);
        }

        if (hoverState != null && (mode == ToolBar.Mode.ADD_TRANSITION || mode == ToolBar.Mode.MOVE
                || mode == ToolBar.Mode.SET_INITIAL || mode == ToolBar.Mode.TOGGLE_FINAL || mode == ToolBar.Mode.DELETE)) {
            g2.setColor(Palette.SELECTED_HINT);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{5f, 5f}, 0f));
            double r = hoverState.getRadius() + 5;
            g2.draw(new Ellipse2D.Double(hoverState.getX() - r, hoverState.getY() - r, r * 2, r * 2));
        }

        if (stroke != null && stroke.size() > 1) {
            g2.setColor(Palette.SELECTED_HINT);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{6f, 4f}, 0f));
            Path2D.Double preview = new Path2D.Double();
            Point p0 = stroke.get(0);
            preview.moveTo(p0.x, p0.y);
            for (int i = 1; i < stroke.size(); i++) {
                Point p = stroke.get(i);
                preview.lineTo(p.x, p.y);
            }
            g2.draw(preview);
        }

        g2.dispose();
    }

    private Color fillColorFor(State s) {
        if (finishedColor != null && s == currentSimState) return finishedColor;
        if (s == currentSimState) return Palette.CURRENT_STATE;
        return Palette.STATE_FILL;
    }

    private void drawState(Graphics2D g2, State s) {
        Shape localShape = s.hasCustomOutline()
                ? s.getOutline()
                : new Ellipse2D.Double(-s.getRadius(), -s.getRadius(), s.getRadius() * 2, s.getRadius() * 2);

        AffineTransform prev = g2.getTransform();
        g2.translate(s.getX(), s.getY());
        g2.setColor(fillColorFor(s));
        g2.fill(localShape);
        g2.setColor(Palette.STATE_BORDER);
        g2.setStroke(new BasicStroke(2.2f));
        g2.draw(localShape);
        g2.setTransform(prev);

        if (s.isFinalState()) {
            AffineTransform t2 = new AffineTransform();
            t2.translate(s.getX(), s.getY());
            t2.scale(1.18, 1.18);
            Shape enlarged = t2.createTransformedShape(localShape);
            g2.setColor(Palette.STATE_BORDER);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(enlarged);
        }

        g2.setColor(Palette.TEXT);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 15f));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(s.getName());
        g2.drawString(s.getName(), (float) (s.getX() - tw / 2.0), (float) (s.getY() + fm.getAscent() / 2.0 - 2));

        if (s.isInitial()) {
            double startX = s.getX() - s.getRadius() - 46;
            double startY = s.getY();
            Point2D border = borderTowards(s, startX, startY);
            g2.setColor(Palette.STATE_BORDER);
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(new Line2D.Double(startX, startY, border.getX(), border.getY()));
            drawFilledArrow(g2, border.getX(), border.getY(), border.getX() - startX, border.getY() - startY, Palette.STATE_BORDER);
        }
    }

    private void drawTransition(Graphics2D g2, Transition t) {
        boolean active = t == activeEdge;
        Color color = active ? Palette.ACTIVE_EDGE : Palette.TRANSITION;
        g2.setStroke(new BasicStroke(active ? 3.6f : 2.2f));
        g2.setColor(color);

        if (t.isLoop()) {
            Point2D[] p = loopPoints(t);
            CubicCurve2D curve = new CubicCurve2D.Double(
                    p[0].getX(), p[0].getY(), p[1].getX(), p[1].getY(), p[2].getX(), p[2].getY(), p[3].getX(), p[3].getY());
            g2.draw(curve);
            drawFilledArrow(g2, p[3].getX(), p[3].getY(), p[3].getX() - p[2].getX(), p[3].getY() - p[2].getY(), color);

            State e = t.getFrom();
            double labelDist = e.getRadius() + t.getLoopSize() + 18;
            double lx = e.getX() + labelDist * Math.cos(t.getLoopAngle());
            double ly = e.getY() + labelDist * Math.sin(t.getLoopAngle());
            drawLabel(g2, lx, ly, t.getLabel(), color);
        } else {
            Point2D[] p = arcPoints(t);
            QuadCurve2D curve = new QuadCurve2D.Double(p[0].getX(), p[0].getY(), p[1].getX(), p[1].getY(), p[2].getX(), p[2].getY());
            g2.draw(curve);
            drawFilledArrow(g2, p[2].getX(), p[2].getY(), p[2].getX() - p[1].getX(), p[2].getY() - p[1].getY(), color);

            Point2D mid = pointOnQuad(p[0], p[1], p[2], 0.5);
            drawLabel(g2, mid.getX(), mid.getY(), t.getLabel(), color);
        }
    }

    private void drawFilledArrow(Graphics2D g2, double tipX, double tipY, double dirX, double dirY, Color color) {
        double len = Math.hypot(dirX, dirY);
        if (len < 1e-6) return;
        double ux = dirX / len, uy = dirY / len;
        double size = 11;
        double angle = Math.toRadians(26);
        double baseX = tipX - ux * size, baseY = tipY - uy * size;
        double perpX = -uy, perpY = ux;
        double wing = size * Math.tan(angle);

        Path2D arrow = new Path2D.Double();
        arrow.moveTo(tipX, tipY);
        arrow.lineTo(baseX + perpX * wing, baseY + perpY * wing);
        arrow.lineTo(baseX - perpX * wing, baseY - perpY * wing);
        arrow.closePath();

        Color previous = g2.getColor();
        g2.setColor(color);
        g2.fill(arrow);
        g2.setColor(previous);
    }

    private void drawLabel(Graphics2D g2, double x, double y, String text, Color textColor) {
        if (text.isEmpty()) return;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 10;
        int h = fm.getHeight();
        g2.setColor(Palette.LABEL_BG);
        g2.fillRoundRect((int) (x - w / 2.0), (int) (y - h / 2.0), w, h, 8, 8);
        g2.setColor(textColor);
        g2.drawString(text, (float) (x - (w - 10) / 2.0), (float) (y + fm.getAscent() / 2.0 - 2));
    }
}
