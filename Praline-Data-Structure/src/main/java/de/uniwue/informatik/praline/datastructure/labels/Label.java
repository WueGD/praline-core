package de.uniwue.informatik.praline.datastructure.labels;

import com.fasterxml.jackson.annotation.*;
import de.uniwue.informatik.praline.datastructure.oldUnstyledObjects.OldUnstyledTextLabel;
import de.uniwue.informatik.praline.datastructure.styles.LabelStyle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;

/**
 * Can be attached to a {@link LabeledObject} (e. g. a
 * {@link de.uniwue.informatik.praline.datastructure.graphs.Vertex}, a
 * {@link de.uniwue.informatik.praline.datastructure.graphs.Port}, an
 * {@link de.uniwue.informatik.praline.datastructure.graphs.Edge} or a
 * {@link de.uniwue.informatik.praline.datastructure.graphs.VertexGroup}) to provide additional information about
 * this object.
 *
 * A {@link LabeledObject} can have an arbitrary number of {@link Label}s, which are managed by the
 * {@link LabelManager} of the {@link LabeledObject}.
 *
 * A {@link Label} is supposed to be visualized by the drawing algorithm unless {@link LabelStyle#isShowLabel()} is set to
 * false. Therefore, a {@link Label} is a {@link ShapedObject}.
 * The position of the {@link Label} relative to its {@link LabeledObject} is specified by
 * {@link LabelStyle#getPlacement()}, {@link LabelStyle#getHorizontalPlacement()} and
 * {@link LabelStyle#getVerticalPlacement()}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OldUnstyledTextLabel.class, name = "text"),
        @JsonSubTypes.Type(value = TextLabel.class, name = "textLabel"),
        @JsonSubTypes.Type(value = IconLabel.class, name = "iconLabel"),
        @JsonSubTypes.Type(value = LeaderedLabel.class, name = "leaderedLabel"),
        @JsonSubTypes.Type(value = ReferenceIconLabel.class, name = "referenceIcon"),
})
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public abstract class Label<LS extends LabelStyle> implements ShapedObject {

    /*==========
     * Instance variables
     *==========*/

    private LabelManager associatedLabelManager;
    protected LS labelStyle;
    private Shape shape;


    /*==========
     * Constructors
     *==========*/

    protected Label() {
        this(null, null);
    }

    protected Label(Shape shape) {
        this(null, shape);
    }

    protected Label(LS labelStyle) {
        this(labelStyle, null);
    }

    protected Label(LS labelStyle, Shape shape) {
        this.labelStyle = labelStyle;
        this.shape = shape;
    }


    /*==========
     * Getters & Setters
     *==========*/

    /**
     * This value should be changed from an instance of {@link LabelManager}
     * whenever a {@link Label} is added to a {@link LabelManager} of a {@link LabeledObject}
     * via {@link LabelManager#addLabel(Label)}
     * or {@link LabelManager#removeLabel(Label)}.
     *
     * It can be used to find its associated {@link LabeledObject} via
     * {@link LabelManager#getManagedLabeledObject}.
     */
    @JsonIgnore
    public LabelManager getAssociatedLabelManager() {
        return associatedLabelManager;
    }

    /**
     * This value should be changed from an instance of {@link LabelManager}
     * whenever a {@link Label} is added to a {@link LabelManager} of a {@link LabeledObject}
     * via {@link LabelManager#addLabel(Label)}
     * or {@link LabelManager#removeLabel(Label)}.
     * This is the reason this method is "protected".
     *
     * @param associatedLabelManager
     */
    protected void setAssociatedLabelManager(LabelManager associatedLabelManager) {
        this.associatedLabelManager = associatedLabelManager;
    }

    public LS getLabelStyle() {
        return labelStyle;
    }

    public void setLabelStyle(LS labelStyle) {
        this.labelStyle = labelStyle;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public abstract boolean equalLabeling(Label o);
}
