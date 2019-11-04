package de.uniwue.informatik.praline.datastructure.labels;

import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.ArrowHeadTriangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;

import java.util.Collection;

public class LeaderedLabel extends Label implements LabeledObject {

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
    private final LabelManager labelManager;


    /*==========
     * Constructors
     *==========*/

    public LeaderedLabel() {
        this(LeaderedLabel.DEFAULT_SHAPE_TO_BE_CLONED.clone(), LeaderedLabel.UNSPECIFIED_THICKNESS, Placement.FREE,
                HorizontalPlacement.FREE, VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null, null, null);
    }

    public LeaderedLabel(Shape arrowHead) {
        this(arrowHead, LeaderedLabel.UNSPECIFIED_THICKNESS, Placement.FREE, HorizontalPlacement.FREE,
                VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null, null, null);
    }

    public LeaderedLabel(double pathThickness) {
        this(LeaderedLabel.DEFAULT_SHAPE_TO_BE_CLONED.clone(), pathThickness, Placement.FREE, HorizontalPlacement.FREE,
                VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null, null, null);
    }

    public LeaderedLabel(Shape arrowHead, double pathThickness) {
        this(arrowHead, pathThickness, Placement.FREE, HorizontalPlacement.FREE,
                VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null, null, null);
    }

    public LeaderedLabel(Shape arrowHead, double pathThickness, Placement placement,
                         HorizontalPlacement horizontalPlacement, VerticalPlacement verticalPlacement,
                         boolean showLabel, Shape shape, Collection<Label> labels, Label mainLabel) {
        super(placement, horizontalPlacement, verticalPlacement, showLabel, shape);
        this.arrowHead = arrowHead;
        this.pathThickness = pathThickness;
        this.labelManager = new LabelManager(this, labels, mainLabel);
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

    @Override
    public LabelManager getLabelManager() {
        return labelManager;
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return labelManager.getStringForLabeledObject();
    }
}
