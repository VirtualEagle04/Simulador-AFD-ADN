package afdpainter.model;

/**
 * Representa un estado (nodo) del AFD dibujado por el usuario.
 */
public class State {
    private String name;
    private int x, y;
    private boolean initial;
    private boolean finalState;

    public State(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public boolean isInitial() { return initial; }
    public void setInitial(boolean initial) { this.initial = initial; }

    public boolean isFinalState() { return finalState; }
    public void setFinalState(boolean finalState) { this.finalState = finalState; }

    @Override
    public String toString() { return name; }
}
