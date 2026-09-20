package afdpainter.model;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Representa un estado (nodo) del AFD. El usuario lo dibuja a mano
 * (trazo libre); si el trazo forma una figura reconocible, se guarda
 * su contorno real (outline) relativo al centro, para que se dibuje
 * tal cual el usuario lo trazó y se mueva junto con el estado.
 */
public class State {
    public static final double DEFAULT_RADIUS = 34;

    private String name;
    private int x, y;
    private double radius = DEFAULT_RADIUS;
    private boolean initial;
    private boolean finalState;

    /** Puntos del contorno dibujado a mano, relativos al centro (x,y). Puede ser null. */
    private List<Point2D> outline;

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

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }

    public boolean isInitial() { return initial; }
    public void setInitial(boolean initial) { this.initial = initial; }

    public boolean isFinalState() { return finalState; }
    public void setFinalState(boolean finalState) { this.finalState = finalState; }

    public List<Point2D> getOutline() { return outline; }
    public void setOutline(List<Point2D> outline) { this.outline = outline; }
    public boolean hasCustomOutline() { return outline != null && outline.size() >= 3; }

    @Override
    public String toString() { return name; }
}
