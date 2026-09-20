package afdpainter.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa una arista (transición) entre dos estados, con uno o
 * varios símbolos asociados.
 *
 * La forma visual es la que el usuario dibujó a mano (a lápiz), guardada
 * de forma RELATIVA para que la curva siga uniendo los dos estados aunque
 * el usuario los mueva después:
 *  - Si from != to: se guarda una lista de pares (t, offset) respecto a la
 *    línea base entre los centros de los dos estados (t = posición a lo
 *    largo de la línea, offset = distancia perpendicular en píxeles).
 *  - Si from == to (self-loop): se guarda una lista de puntos (dx,dy)
 *    relativos al centro del propio estado.
 */
public class Transition {
    private final State from;
    private final State to;
    private final Set<Character> symbols = new LinkedHashSet<>();

    private List<double[]> curveShape;   // pares {t, offset} para from != to
    private List<Point2D> loopShape;     // puntos (dx,dy) para self-loop

    public Transition(State from, State to) {
        this.from = from;
        this.to = to;
    }

    public State getFrom() { return from; }
    public State getTo() { return to; }
    public Set<Character> getSymbols() { return symbols; }

    public void addSymbol(char c) { symbols.add(c); }
    public void removeSymbol(char c) { symbols.remove(c); }
    public boolean isEmpty() { return symbols.isEmpty(); }

    public List<double[]> getCurveShape() { return curveShape; }
    public void setCurveShape(List<double[]> curveShape) { this.curveShape = curveShape; }
    public boolean hasCurveShape() { return curveShape != null && curveShape.size() >= 2; }

    public List<Point2D> getLoopShape() { return loopShape; }
    public void setLoopShape(List<Point2D> loopShape) { this.loopShape = loopShape; }
    public boolean hasLoopShape() { return loopShape != null && loopShape.size() >= 2; }

    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        for (char c : symbols) {
            if (sb.length() > 0) sb.append(',');
            sb.append(c);
        }
        return sb.toString();
    }
}
