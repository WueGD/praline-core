package de.uniwue.informatik.praline.datastructure.utils;

import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;

import java.awt.geom.Point2D;

public class ArithmeticOperation {
    /*==========
     * Constants
     *==========*/

    public static final double ARITHMETIC_PRECISION = 0.000001;


    /*==========
     * Methods
     *==========*/

    public static boolean precisionEqual(Point2D.Double p0, Point2D.Double p1) {
        return precisionEqual(p0.x, p1.x) && precisionEqual(p0.y, p1.y);
    }

    public static boolean precisionEqual(double val0, double val1) {
        return precisionInRange(val0, val1, val1);
    }

    public static boolean precisionInRange(double value, double rangeStart, double rangeEnd) {
        return rangeStart - ARITHMETIC_PRECISION <= value && value <= rangeEnd + ARITHMETIC_PRECISION;
    }
}
