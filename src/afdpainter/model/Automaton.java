package afdpainter.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Automaton {

    public enum Kind { DFA, NFA }

    private final List<State> states = new ArrayList<>();
    private final List<Transition> transitions = new ArrayList<>();
    private Set<Character> alphabet = new LinkedHashSet<>();
    private int nameCounter = 0;
    private Kind kind = Kind.DFA;

    public List<State> getStates() { return states; }
    public List<Transition> getTransitions() { return transitions; }
    public Set<Character> getAlphabet() { return alphabet; }
    public void setAlphabet(Set<Character> alphabet) { this.alphabet = alphabet; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }

    public String nextStateName() { return "q" + (nameCounter++); }

    public void addState(State s) { states.add(s); }

    public void removeState(State s) {
        states.remove(s);
        transitions.removeIf(t -> t.getFrom() == s || t.getTo() == s);
    }

    public void removeTransition(Transition t) { transitions.remove(t); }

    public State getInitialState() {
        for (State s : states) if (s.isInitial()) return s;
        return null;
    }

    public void setInitialState(State s) {
        for (State st : states) st.setInitial(false);
        s.setInitial(true);
    }

    public Transition findTransition(State from, char symbol) {
        for (Transition t : transitions) {
            if (t.getFrom() == from && t.getSymbols().contains(symbol)) return t;
        }
        return null;
    }

    public Transition getTransitionBetween(State from, State to) {
        for (Transition t : transitions) {
            if (t.getFrom() == from && t.getTo() == to) return t;
        }
        return null;
    }

    public List<Transition> getTransitions(State from, char symbol) {
        List<Transition> result = new ArrayList<>();
        for (Transition t : transitions) {
            if (t.getFrom() == from && t.getSymbols().contains(symbol)) result.add(t);
        }
        return result;
    }

    public Set<State> epsilonClosure(Set<State> from) {
        Set<State> closure = new LinkedHashSet<>(from);
        Deque<State> pending = new ArrayDeque<>(from);
        while (!pending.isEmpty()) {
            State s = pending.poll();
            for (Transition t : transitions) {
                if (t.getFrom() == s && t.getSymbols().contains(Transition.EPSILON)) {
                    if (closure.add(t.getTo())) pending.add(t.getTo());
                }
            }
        }
        return closure;
    }

    public State findStateAt(double x, double y) {
        for (int i = states.size() - 1; i >= 0; i--) {
            State s = states.get(i);
            double d = Math.hypot(s.getX() - x, s.getY() - y);
            if (d <= s.getRadius() * 1.15) return s;
        }
        return null;
    }

    public void clear() {
        states.clear();
        transitions.clear();
        nameCounter = 0;
    }
}
