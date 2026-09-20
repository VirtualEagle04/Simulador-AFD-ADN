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
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Lienzo de dibujo LIBRE (a mano alzada, como un lápiz): el usuario
 * mantiene el mouse presionado mientras lo mueve para trazar la figura
 * de un estado (un contorno cerrado) o de una transición (una curva
 * entre dos estados, o un lazo sobre el mismo estado). El trazo real
 * del usuario es lo que se guarda y se dibuja; no se colocan figuras
 * preestablecidas de una "biblioteca".
 *
 * Marcar estado inicial/final y eliminar siguen siendo acciones de un
 * clic (son banderas/booleanos, no figuras que tenga sentido dibujar).
 */
public class DrawingPanel extends JPanel {

    private static final double MIN_POINT_DIST = 4.0;      // muestreo del trazo
    private static final double TRANSITION_TOLERANCE = 18; // holgura para enganchar un estado al dibujar una transición
    private static final double CLICK_TOLERANCE = 4;       // holgura para mover/marcar/eliminar

    private final Automaton automaton;
    private ToolBar.Mode mode = ToolBar.Mode.MOVE;

    // Arrastre para mover nodos
    private State dragState;

    // Trazo libre en curso (para estado nuevo o transición nueva)
    private List<Point> stroke;
    private State transitionSource;

    // Resaltado de simulación
    private State currentSimState;
    private Transition activeEdge;
    private Color finishedColor;

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
        stroke = null;
        transitionSource = null;
        dragState = null;
        repaint();
    }

    public Automaton getAutomaton() { return automaton; }

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
        Point p = e.getPoint();
        switch (mode) {
            case ADD_STATE:
                stroke = new ArrayList<>();
                stroke.add(p);
                break;
            case ADD_TRANSITION: {
                State src = automaton.findStateAt(p.x, p.y, TRANSITION_TOLERANCE);
                if (src != null) {
                    transitionSource = src;
                    stroke = new ArrayList<>();
                    stroke.add(p);
                }
                break;
            }
            case MOVE:
                dragState = automaton.findStateAt(p.x, p.y, CLICK_TOLERANCE);
                break;
            case SET_INITIAL: {
                State clicked = automaton.findStateAt(p.x, p.y, CLICK_TOLERANCE);
                if (clicked != null) {
                    automaton.setInitialState(clicked);
                    repaint();
                }
                break;
            }
            case TOGGLE_FINAL: {
                State clicked = automaton.findStateAt(p.x, p.y, CLICK_TOLERANCE);
                if (clicked != null) {
                    clicked.setFinalState(!clicked.isFinalState());
                    repaint();
                }
                break;
            }
            case DELETE: {
                State clicked = automaton.findStateAt(p.x, p.y, CLICK_TOLERANCE);
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
    }

    private void handleDragged(MouseEvent e) {
        Point p = e.getPoint();
        if (mode == ToolBar.Mode.MOVE && dragState != null) {
            dragState.setPosition(p.x, p.y);
            repaint();
        } else if (stroke != null && (mode == ToolBar.Mode.ADD_STATE || mode == ToolBar.Mode.ADD_TRANSITION)) {
            Point last = stroke.get(stroke.size() - 1);
            if (p.distance(last) >= MIN_POINT_DIST) {
                stroke.add(p);
                repaint();
            }
        }
    }

    private void handleReleased(MouseEvent e) {
        Point p = e.getPoint();
        if (mode == ToolBar.Mode.MOVE) {
            dragState = null;
        } else if (mode == ToolBar.Mode.ADD_STATE) {
            if (stroke != null) {
                finalizeNewState(stroke);
                stroke = null;
                repaint();
            }
        } else if (mode == ToolBar.Mode.ADD_TRANSITION) {
            if (stroke != null && transitionSource != null) {
                State target = automaton.findStateAt(p.x, p.y, TRANSITION_TOLERANCE);
                State source = transitionSource;
                List<Point> finishedStroke = stroke;
                stroke = null;
                transitionSource = null;
                if (target != null) {
                    finalizeNewTransition(source, target, finishedStroke);
                }
                repaint();
            }
        }
    }

    // ---------------------------- Creación a partir del trazo ----------------------------

    private void finalizeNewState(List<Point> pts) {
        Point first = pts.get(0);
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point p : pts) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
        }
        double diag = Math.hypot(maxX - minX, maxY - minY);

        // Evita crear un estado encima de uno ya existente con un mal clic
        if (diag < 12 && automaton.findStateAt(first.x, first.y, 6) != null) return;

        double cx, cy, radius;
        List<Point2D> outline = null;

        if (pts.size() < 5 || diag < 18) {
            // Prácticamente un clic (sin trazo real): círculo estándar en ese punto
            cx = first.x;
            cy = first.y;
            radius = State.DEFAULT_RADIUS;
        } else {
            cx = 0; cy = 0;
            for (Point p : pts) { cx += p.x; cy += p.y; }
            cx /= pts.size(); cy /= pts.size();
            double avgR = 0;
            for (Point p : pts) avgR += Math.hypot(p.x - cx, p.y - cy);
            avgR /= pts.size();
            radius = Math.max(22, Math.min(95, avgR));
            outline = new ArrayList<>();
            for (Point p : pts) outline.add(new Point2D.Double(p.x - cx, p.y - cy));
        }

        State s = new State(automaton.nextStateName(), (int) Math.round(cx), (int) Math.round(cy));
        s.setRadius(radius);
        s.setOutline(outline);
        if (automaton.getStates().isEmpty()) s.setInitial(true);
        automaton.addState(s);
    }

    private void finalizeNewTransition(State source, State target, List<Point> pts) {
        List<Point2D> loopShape = null;
        List<double[]> curveShape = null;

        if (source == target) {
            loopShape = new ArrayList<>();
            for (Point p : pts) loopShape.add(new Point2D.Double(p.x - source.getX(), p.y - source.getY()));
            if (loopShape.size() < 3) loopShape = defaultLoopShape();
        } else if (pts.size() < 4) {
            // trazo casi un clic: curva automática suave (no hay figura real que reutilizar)
            curveShape = defaultCurveShape(source, target);
        } else {
            double sx = source.getX(), sy = source.getY(), tx = target.getX(), ty = target.getY();
            double bx = tx - sx, by = ty - sy;
            double L = Math.hypot(bx, by);
            if (L < 1) L = 1;
            double ux = bx / L, uy = by / L;
            double nx = -uy, ny = ux;
            curveShape = new ArrayList<>();
            for (Point p : pts) {
                double vx = p.x - sx, vy = p.y - sy;
                double t = (vx * ux + vy * uy) / L;
                double offset = vx * nx + vy * ny;
                curveShape.add(new double[]{t, offset});
            }
        }
        openSymbolDialogAndStore(source, target, loopShape, curveShape);
    }

    private List<double[]> defaultCurveShape(State source, State target) {
        double sign = 1;
        Transition opposite = automaton.getTransitionBetween(target, source);
        if (opposite != null && opposite.hasCurveShape()) {
            double avg = 0;
            for (double[] rp : opposite.getCurveShape()) avg += rp[1];
            avg /= opposite.getCurveShape().size();
            if (avg > 0) sign = -1;
        }
        List<double[]> rel = new ArrayList<>();
        rel.add(new double[]{0.12, 0});
        rel.add(new double[]{0.5, 34 * sign});
        rel.add(new double[]{0.88, 0});
        return rel;
    }

    private List<Point2D> defaultLoopShape() {
        List<Point2D> pts = new ArrayList<>();
        pts.add(new Point2D.Double(-16, -30));
        pts.add(new Point2D.Double(-14, -58));
        pts.add(new Point2D.Double(16, -58));
        pts.add(new Point2D.Double(20, -28));
        return pts;
    }

    private void openSymbolDialogAndStore(State source, State target, List<Point2D> loopShape, List<double[]> curveShape) {
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
            return;
        }

        if (loopShape != null) t.setLoopShape(loopShape);
        if (curveShape != null) t.setCurveShape(curveShape);

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

    // ---------------------------- Reconstrucción geométrica ----------------------------

    /** Reconstruye los puntos absolutos de una transición A->B (A!=B) según las posiciones ACTUALES de los estados. */
    private List<Point2D> reconstructCurvePoints(Transition t) {
        State s = t.getFrom(), tgt = t.getTo();
        List<double[]> shape = t.hasCurveShape() ? t.getCurveShape() : defaultCurveShape(s, tgt);
        double sx = s.getX(), sy = s.getY(), tx = tgt.getX(), ty = tgt.getY();
        double bx = tx - sx, by = ty - sy;
        double L = Math.hypot(bx, by);
        if (L < 1) L = 1;
        double ux = bx / L, uy = by / L;
        double nx = -uy, ny = ux;
        List<Point2D> pts = new ArrayList<>();
        for (double[] rp : shape) {
            double tt = rp[0], off = rp[1];
            double px = sx + ux * tt * L + nx * off;
            double py = sy + uy * tt * L + ny * off;
            pts.add(new Point2D.Double(px, py));
        }
        return pts;
    }

    /** Reconstruye los puntos absolutos de un self-loop según la posición ACTUAL del estado. */
    private List<Point2D> reconstructLoopPoints(Transition t) {
        State s = t.getFrom();
        List<Point2D> shape = t.hasLoopShape() ? t.getLoopShape() : defaultLoopShape();
        List<Point2D> pts = new ArrayList<>();
        for (Point2D rp : shape) pts.add(new Point2D.Double(s.getX() + rp.getX(), s.getY() + rp.getY()));
        return pts;
    }

    private Transition findTransitionNear(Point p) {
        double best = 10.0;
        Transition bestT = null;
        for (Transition t : automaton.getTransitions()) {
            List<Point2D> pts = (t.getFrom() == t.getTo()) ? reconstructLoopPoints(t) : reconstructCurvePoints(t);
            double d = distanceToPolyline(pts, p);
            if (d < best) { best = d; bestT = t; }
        }
        return bestT;
    }

    private double distanceToPolyline(List<Point2D> pts, Point p) {
        if (pts.isEmpty()) return Double.MAX_VALUE;
        if (pts.size() == 1) return pts.get(0).distance(p.x, p.y);
        double min = Double.MAX_VALUE;
        for (int i = 0; i < pts.size() - 1; i++) {
            double d = Line2D.ptSegDist(pts.get(i).getX(), pts.get(i).getY(),
                    pts.get(i + 1).getX(), pts.get(i + 1).getY(), p.x, p.y);
            if (d < min) min = d;
        }
        return min;
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

        for (State s : automaton.getStates()) {
            drawState(g2, s);
        }

        // Trazo en progreso (feedback visual del lápiz)
        if (stroke != null && stroke.size() > 1) {
            g2.setColor(Palette.SELECTED_HINT);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D preview = new Path2D.Double();
            preview.moveTo(stroke.get(0).x, stroke.get(0).y);
            for (int i = 1; i < stroke.size(); i++) preview.lineTo(stroke.get(i).x, stroke.get(i).y);
            g2.draw(preview);
        }
    }

    private void drawState(Graphics2D g2, State s) {
        double r = s.getRadius();
        int x = s.getX(), y = s.getY();

        if (s.isInitial()) {
            g2.setColor(Palette.STATE_BORDER);
            g2.setStroke(new BasicStroke(1.8f));
            int startX = (int) (x - r - 40);
            g2.drawLine(startX, y, (int) (x - r), y);
            drawArrowHead(g2, x - r, y, 0);
        }

        Color fill = Palette.STATE_FILL;
        if (finishedColor != null && s == currentSimState) fill = finishedColor;
        else if (s == currentSimState) fill = Palette.CURRENT_STATE;

        Shape outerShape;
        if (s.hasCustomOutline()) {
            List<Point2D> abs = new ArrayList<>();
            for (Point2D rp : s.getOutline()) abs.add(new Point2D.Double(x + rp.getX(), y + rp.getY()));
            outerShape = smoothPath(abs, true);
        } else {
            outerShape = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
        }

        g2.setColor(fill);
        g2.fill(outerShape);
        g2.setColor(Palette.STATE_BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(outerShape);

        if (s.isFinalState()) {
            Shape innerShape;
            if (s.hasCustomOutline()) {
                List<Point2D> abs = new ArrayList<>();
                for (Point2D rp : s.getOutline()) abs.add(new Point2D.Double(x + rp.getX() * 0.78, y + rp.getY() * 0.78));
                innerShape = smoothPath(abs, true);
            } else {
                double inset = 6;
                innerShape = new Ellipse2D.Double(x - r + inset, y - r + inset, (r - inset) * 2, (r - inset) * 2);
            }
            g2.draw(innerShape);
        }

        g2.setColor(Palette.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(s.getName());
        g2.drawString(s.getName(), (int) (x - tw / 2.0), (int) (y + fm.getAscent() / 2.0 - 2));
    }

    private void drawCurvedTransition(Graphics2D g2, Transition t) {
        List<Point2D> pts = reconstructCurvePoints(t);
        if (pts.size() < 2) return;
        boolean active = t == activeEdge;
        Path2D path = smoothPath(pts, false);

        g2.setColor(active ? Palette.ACTIVE_EDGE : Palette.TRANSITION);
        g2.setStroke(new BasicStroke(active ? 3.2f : 1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);

        Point2D last = pts.get(pts.size() - 1);
        Point2D prev = pts.get(Math.max(0, pts.size() - 2));
        double angle = Math.atan2(last.getY() - prev.getY(), last.getX() - prev.getX());
        drawArrowHead(g2, last.getX(), last.getY(), angle);

        String label = t.getLabel();
        if (!label.isEmpty()) {
            Point2D mid = pts.get(pts.size() / 2);
            drawLabel(g2, label, mid.getX(), mid.getY(), active);
        }
    }

    private void drawSelfLoop(Graphics2D g2, Transition t) {
        List<Point2D> pts = reconstructLoopPoints(t);
        if (pts.size() < 2) return;
        boolean active = t == activeEdge;
        Path2D path = smoothPath(pts, false);

        g2.setColor(active ? Palette.ACTIVE_EDGE : Palette.TRANSITION);
        g2.setStroke(new BasicStroke(active ? 3.2f : 1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);

        Point2D last = pts.get(pts.size() - 1);
        Point2D prev = pts.get(Math.max(0, pts.size() - 2));
        double angle = Math.atan2(last.getY() - prev.getY(), last.getX() - prev.getX());
        drawArrowHead(g2, last.getX(), last.getY(), angle);

        String label = t.getLabel();
        if (!label.isEmpty()) {
            Point2D top = pts.get(pts.size() / 2);
            drawLabel(g2, label, top.getX(), top.getY() - 6, active);
        }
    }

    private void drawLabel(Graphics2D g2, String label, double cx, double cy, boolean active) {
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(label);
        g2.setColor(Palette.CANVAS_BG);
        g2.fillRect((int) cx - tw / 2 - 2, (int) cy - fm.getAscent() - 1, tw + 4, fm.getHeight());
        g2.setColor(active ? Palette.ACTIVE_EDGE : Palette.TEXT);
        g2.drawString(label, (int) cx - tw / 2, (int) cy);
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

    /** Suaviza una polilínea (técnica de curvas cuadráticas por los puntos medios). */
    private Path2D smoothPath(List<Point2D> pts, boolean closed) {
        Path2D.Double path = new Path2D.Double();
        int n = pts.size();
        if (n == 0) return path;
        if (n == 1) {
            Point2D p0 = pts.get(0);
            path.moveTo(p0.getX(), p0.getY());
            return path;
        }
        path.moveTo(pts.get(0).getX(), pts.get(0).getY());
        int limit = closed ? n : n - 1;
        for (int i = 0; i < limit; i++) {
            Point2D cur = pts.get(i);
            Point2D next = pts.get((i + 1) % n);
            double midX = (cur.getX() + next.getX()) / 2.0;
            double midY = (cur.getY() + next.getY()) / 2.0;
            path.quadTo(cur.getX(), cur.getY(), midX, midY);
        }
        if (closed) {
            path.closePath();
        } else {
            Point2D lastPt = pts.get(n - 1);
            path.lineTo(lastPt.getX(), lastPt.getY());
        }
        return path;
    }
}
