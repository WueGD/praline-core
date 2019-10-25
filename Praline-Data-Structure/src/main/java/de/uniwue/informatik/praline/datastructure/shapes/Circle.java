package de.uniwue.informatik.praline.datastructure.shapes;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Circle extends Ellipse2D.Double implements Shape {


    //TODO: this class!

    private Color color;

    @Override
    public double getXPosition() {
        return getXPosition();
    }

    @Override
    public double getYPosition() {
        return getYPosition();
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color c) {
        this.color = c;
    }

    @Override
    public Shape clone() {
        //TODO
        return new Rectangle();
    }
}
