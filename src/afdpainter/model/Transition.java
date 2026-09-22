package afdpainter.model;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Set;

public class Transition {

    public static final char EPSILON = '\u03B5';

    private final State from;
    private final State to;
    private final Set<Character> symbols = new LinkedHashSet<>();
    private Color color;

    private double bow = 34;
    private double loopAngle = -Math.PI / 2;
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
    
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        for (char c : symbols) {
            if (sb.length() > 0) sb.append(',');
            sb.append(c);
        }
        return sb.toString();
    }
}
