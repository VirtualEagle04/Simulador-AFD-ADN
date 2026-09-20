package afdpainter.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Representa una arista (transición) entre dos estados, con uno o
 * varios símbolos asociados.
 *
 * La geometría se guarda como parámetros simples que se recalculan
 * siempre con la posición ACTUAL de los estados (así la curva sigue
 * uniendo los nodos aunque el usuario los mueva):
 *  - from != to: "bow" = qué tanto se abomba el arco hacia un lado.
 *    El LADO (arriba/abajo) se decide de forma canónica según el orden
 *    de los estados en la lista, así A->B y B->A quedan en lados
 *    opuestos y nunca se sobreponen.
 *  - from == to (self-loop): ángulo hacia donde apunta el lazo y qué
 *    tanto se abomba hacia afuera.
 */
public class Transition {

    /** Símbolo especial que representa una transición vacía (épsilon), usado solo en AFN. */
    public static final char EPSILON = '\u03B5';

    private final State from;
    private final State to;
    private final Set<Character> symbols = new LinkedHashSet<>();

    private double bow = 34;
    private double loopAngle = -Math.PI / 2; // hacia arriba por defecto
    private double loopSize = 30;

    public Transition(State from, State to) {
        this.from = from;
        this.to = to;
    }

    public State getFrom() { return from; }
    public State getTo() { return to; }
    public boolean isLoop() { return from == to; }
    public Set<Character> getSymbols() { return symbols; }

    public void addSymbol(char c) { symbols.add(c); }
    public boolean isEmpty() { return symbols.isEmpty(); }
    public boolean isEpsilonOnly() { return symbols.size() == 1 && symbols.contains(EPSILON); }

    public double getBow() { return bow; }
    public void setBow(double bow) { this.bow = bow; }

    public double getLoopAngle() { return loopAngle; }
    public void setLoopAngle(double loopAngle) { this.loopAngle = loopAngle; }

    public double getLoopSize() { return loopSize; }
    public void setLoopSize(double loopSize) { this.loopSize = loopSize; }

    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        for (char c : symbols) {
            if (sb.length() > 0) sb.append(',');
            sb.append(c);
        }
        return sb.toString();
    }
}
