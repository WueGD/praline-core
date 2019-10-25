package de.uniwue.informatik.praline.datastructure.labels;

import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.ArrowHeadTriangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;

public class LeaderedLabel extends Label {

    /*==========
     * Default values
     *==========*/

    public static final Shape DEFAULT_SHAPE_TO_BE_CLONED = new ArrowHeadTriangle();
    public static final double UNSPECIFIED_THICKNESS = -1;

    /*==========
     * Instance variables
     *==========*/

    private Shape arrowHead;
    private double pathThickness;
    private Path path;

    /*==========
     * Constructors
     *==========*/

    public LeaderedLabel() {
        this(LeaderedLabel.DEFAULT_SHAPE_TO_BE_CLONED.clone(), LeaderedLabel.UNSPECIFIED_THICKNESS, Placement.FREE,
                HorizontalPlacement.FREE, VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public LeaderedLabel(Shape arrowHead) {
        this(arrowHead, LeaderedLabel.UNSPECIFIED_THICKNESS, Placement.FREE, HorizontalPlacement.FREE,
                VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public LeaderedLabel(double pathThickness) {
        this(LeaderedLabel.DEFAULT_SHAPE_TO_BE_CLONED.clone(), pathThickness, Placement.FREE, HorizontalPlacement.FREE,
                VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public LeaderedLabel(Shape arrowHead, double pathThickness) {
        this(arrowHead, pathThickness, Placement.FREE, HorizontalPlacement.FREE,
                VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public LeaderedLabel(Shape arrowHead, double pathThickness, Placement placement,
                         HorizontalPlacement horizontalPlacement, VerticalPlacement verticalPlacement,
                         boolean showLabel, Shape shape) {
        super(placement, horizontalPlacement, verticalPlacement, showLabel, shape);
        this.arrowHead = arrowHead;
        this.pathThickness = pathThickness;
    }

    /*==========
     * Getters & Setters
     *==========*/

    public Shape getArrowHead() {
        return arrowHead;
    }

    public void setArrowHead(Shape arrowHead) {
        this.arrowHead = arrowHead;
    }

    public double getPathThickness() {
        return pathThickness;
    }

    public void setPathThickness(double pathThickness) {
        this.pathThickness = pathThickness;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
