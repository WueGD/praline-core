package de.uniwue.informatik.praline.datastructure.shapes;

import java.awt.*;

public interface Shape {
    double getXPosition();
    double getYPosition();
    Color getColor();
    void setColor(Color c);
    Shape clone();
}
