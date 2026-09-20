package afdpainter.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Representa una arista (transición) entre dos estados, con uno o
 * varios símbolos asociados (p.ej. una arista puede llevar "0,1").
 */
public class Transition {
    private final State from;
    private final State to;
    private final Set<Character> symbols = new LinkedHashSet<>();

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

    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        for (char c : symbols) {
            if (sb.length() > 0) sb.append(',');
            sb.append(c);
        }
        return sb.toString();
    }
}
