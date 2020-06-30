package de.uniwue.informatik.praline.layouting.layered.algorithm.edgerouting;

public class ContourPoint {

    private int level;
    private double xPosition;

    public ContourPoint(int level, double xPosition) {
        this.level = level;
        this.xPosition = xPosition;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getxPosition() {
        return xPosition;
    }

    public void setxPosition(double xPosition) {
        this.xPosition = xPosition;
    }
}
