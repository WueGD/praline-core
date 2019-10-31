package de.uniwue.informatik.praline.datastructure.shapes;

import java.awt.*;

public interface Shape extends Cloneable {

    /*==========
     * Default values
     *==========*/

    Color DEFAULT_COLOR = Color.BLACK;
    double UNDEFINED_LENGTH = java.lang.Double.NaN;
    double UNDEFINED_POSITION = java.lang.Double.NaN;


    /*==========
     * Methods to be implemented
     *==========*/

    double getXPosition();
    double getYPosition();
    Color getColor();
    void setColor(Color c);
    Shape clone();
}
