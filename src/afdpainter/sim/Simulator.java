package afdpainter.sim;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de simulación: NO usa ninguna librería de autómatas.
 * Simplemente recorre la función de transición del AFD dibujado
 * por el usuario, símbolo a símbolo.
 */
public class Simulator {

    public enum Verdict { ACCEPTED, REJECTED, STUCK, NO_INITIAL }

    public static class Result {
        /** path.get(i) = estado en el que se está tras consumir los primeros i símbolos. */
        public final List<State> path = new ArrayList<>();
        public String input;
        /** Índice del símbolo que no tuvo transición (-1 si no quedó atascado). */
        public int stuckAtIndex = -1;
        public Verdict verdict;
    }

    public static Result simulate(Automaton dfa, String input) {
        Result r = new Result();
        r.input = input;
        State current = dfa.getInitialState();
        if (current == null) {
            r.verdict = Verdict.NO_INITIAL;
            return r;
        }
        r.path.add(current);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            Transition t = dfa.findTransition(current, c);
            if (t == null) {
                r.stuckAtIndex = i;
                r.verdict = Verdict.STUCK;
                return r;
            }
            current = t.getTo();
            r.path.add(current);
        }
        r.verdict = current.isFinalState() ? Verdict.ACCEPTED : Verdict.REJECTED;
        return r;
    }
}
