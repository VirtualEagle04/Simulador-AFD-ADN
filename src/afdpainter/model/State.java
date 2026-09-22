package afdpainter.model;

import java.awt.Color;
import java.awt.geom.GeneralPath;

public class State {
    public static final double DEFAULT_RADIUS = 34;

    private String name;
    private double x, y;
    private double radius = DEFAULT_RADIUS;
    private boolean initial;
    private boolean finalState;
    private GeneralPath outline;
    private Color strokeColor;

    public State(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getX() { return x; }
    public double getY() { return y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }

    public boolean isInitial() { return initial; }
    public void setInitial(boolean initial) { this.initial = initial; }

    public boolean isFinalState() { return finalState; }
    public void setFinalState(boolean finalState) { this.finalState = finalState; }

    public GeneralPath getOutline() { return outline; }
    public void setOutline(GeneralPath outline) { this.outline = outline; }
    public boolean hasCustomOutline() { return outline != null; }
    
    public Color getStrokeColor() { return strokeColor; }
    public void setStrokeColor(Color strokeColor) { this.strokeColor = strokeColor; }

    @Override
    public String toString() { return name; }
}
