package de.uniwue.informatik.praline.datastructure.paths;

public abstract class Path {

    /*==========
     * Default values
     *==========*/

    public static final double UNSPECIFIED_THICKNESS = -1;

    /*==========
     * Instance variables
     *==========*/

    /**
     * -1 for unspecified
     */
    private double thickness;

    /*==========
     * Constructors
     *==========*/

    public Path() {
        this(Path.UNSPECIFIED_THICKNESS);
    }

    public Path(double thickness) {
        this.thickness = thickness;
    }

    /*==========
     * Getters & Setters
     *==========*/

    public double getThickness() {
        return thickness;
    }

    public void setThickness(double thickness) {
        this.thickness = thickness;
    }
}
