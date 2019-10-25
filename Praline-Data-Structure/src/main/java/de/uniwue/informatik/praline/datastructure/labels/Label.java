package de.uniwue.informatik.praline.datastructure.labels;

import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;

public abstract class Label implements ShapedObject {

    /*==========
     * Default values
     *==========*/

    public static final boolean DEFAULT_SHOW_LABEL = true;

    /*==========
     * Instance variables
     *==========*/

    private LabeledObject associatedObject;
    private boolean showLabel;
    private Placement placement;
    private HorizontalPlacement horizontalPlacement;
    private VerticalPlacement verticalPlacement;
    private Shape shape;

    /*==========
     * Constructors
     *==========*/

    public Label() {
        this(Placement.FREE, HorizontalPlacement.FREE, VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public Label(Shape shape) {
        this(Placement.FREE, HorizontalPlacement.FREE, VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, shape);
    }

    public Label(Placement placement, HorizontalPlacement horizontalPlacement, VerticalPlacement verticalPlacement) {
        this(placement, horizontalPlacement, verticalPlacement, Label.DEFAULT_SHOW_LABEL, null);
    }

    public Label(Placement placement, HorizontalPlacement horizontalPlacement, VerticalPlacement verticalPlacement,
                 boolean showLabel, Shape shape) {
        this.shape = shape;
        this.showLabel = showLabel;
        this.placement = placement;
        this.horizontalPlacement = horizontalPlacement;
        this.verticalPlacement = verticalPlacement;
    }

    /*==========
     * Getters & Setters
     *==========*/

    /**
     * This value should be changed from an instance of {@link LabeledObject}
     * whenever a {@link Label} is added to a {@link LabeledObject}
     * via {@link LabeledObject#addLabel(Label)}
     * or {@link LabeledObject#removeLabel(Label)}
     */
    public LabeledObject getAssociatedObject() {
        return associatedObject;
    }

    /**
     * This value should be changed from an instance of {@link LabeledObject}
     * whenever a {@link Label} is added to a {@link LabeledObject}
     * via {@link LabeledObject#addLabel(Label)}
     * or {@link LabeledObject#removeLabel(Label)}.
     * This is the reason this method is "protected".
     *
     * @param associatedObject
     */
    protected void setAssociatedObject(LabeledObject associatedObject) {
        this.associatedObject = associatedObject;
    }

    public boolean isShowLabel() {
        return showLabel;
    }

    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
    }

    public Placement getPlacement() {
        return placement;
    }

    public void setPlacement(Placement placement) {
        this.placement = placement;
    }

    public HorizontalPlacement getHorizontalPlacement() {
        return horizontalPlacement;
    }

    public void setHorizontalPlacement(HorizontalPlacement horizontalPlacement) {
        this.horizontalPlacement = horizontalPlacement;
    }

    public VerticalPlacement getVerticalPlacement() {
        return verticalPlacement;
    }

    public void setVerticalPlacement(VerticalPlacement verticalPlacement) {
        this.verticalPlacement = verticalPlacement;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }
}
