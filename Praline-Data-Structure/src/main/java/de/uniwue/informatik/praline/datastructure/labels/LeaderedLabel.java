package de.uniwue.informatik.praline.datastructure.labels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.styles.LabelStyle;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.shapes.ArrowHeadTriangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.styles.PathStyle;

import java.util.Collection;
import java.util.Collections;

/**
 * Rather special version of {@link Label} (see there for more).
 *
 * A {@link LeaderedLabel} connects another {@link Label} (e. g. a {@link TextLabel}) via a {@link Path} with its
 * {@link LabeledObject}. The {@link Label} used here has this {@link LeaderedLabel} as its {@link LabeledObject}.
 * Therefore, {@link LeaderedLabel} implements {@link LabeledObject}.
 *
 * At the end of the {@link Path}, which should be set by the drawing algorithm, there is the
 * {@link LeaderedLabel#arrowHead}. This is typically a {@link ArrowHeadTriangle} but may be any {@link Shape}.
 */
public class LeaderedLabel extends Label<LabelStyle> implements LabeledObject {

    /*==========
     * Default values
     *==========*/

    public static final Shape DEFAULT_SHAPE_TO_BE_CLONED = new ArrowHeadTriangle();


    /*==========
     * Instance variables
     *==========*/

    private Shape arrowHead;
    private PathStyle pathStyle;
    private Path path;
    private final LabelManager labelManager;


    /*==========
     * Constructors
     *==========*/

    public LeaderedLabel() {
        this(LeaderedLabel.DEFAULT_SHAPE_TO_BE_CLONED.clone(), null, null, null, null,
            null);
    }

    public LeaderedLabel(Label<? extends LabelStyle> mainLabel) {
        this(LeaderedLabel.DEFAULT_SHAPE_TO_BE_CLONED.clone(), null, null, null, Collections.singleton(mainLabel),
                mainLabel);
    }

    public LeaderedLabel(Shape arrowHead) {
        this(arrowHead, null, null, null, null, null);
    }

    public LeaderedLabel(PathStyle pathStyle) {
        this(LeaderedLabel.DEFAULT_SHAPE_TO_BE_CLONED.clone(), pathStyle, null, null, null, null);
    }

    public LeaderedLabel(Shape arrowHead, PathStyle pathStyle) {
        this(arrowHead, pathStyle, null, null, null, null);
    }

    public LeaderedLabel(Shape arrowHead, PathStyle pathStyle, Label<? extends LabelStyle> mainLabel) {
        this(arrowHead, pathStyle, null, null, Collections.singleton(mainLabel), mainLabel);
    }

    @JsonCreator
    private LeaderedLabel(
            @JsonProperty("path") final Path path,
            @JsonProperty("arrowHead") final Shape arrowHead,
            @JsonProperty("pathStyle") final PathStyle pathStyle,
            @JsonProperty("labelStyle") final LabelStyle labelStyle,
            @JsonProperty("shape") final  Shape shape,
            @JsonProperty("labelManager") final LabelManager labelManager
    ) {
        this(arrowHead, pathStyle, labelStyle, shape, labelManager.getLabels(), labelManager.getMainLabel());
        this.setPath(path);
    }

    public LeaderedLabel(Shape arrowHead, PathStyle pathStyle, LabelStyle labelStyle, Shape shape,
                         Collection<Label<? extends LabelStyle>> labels, Label<? extends LabelStyle> mainLabel) {
        super(labelStyle == null ? LabelStyle.DEFAULT_LABEL_STYLE : labelStyle, shape);
        this.arrowHead = arrowHead;
        this.pathStyle = pathStyle == null ? PathStyle.DEFAULT_PATH_STYLE : pathStyle;
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

    public PathStyle getPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(PathStyle pathStyle) {
        this.pathStyle = pathStyle;
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


    /*==========
     * equalLabeling
     *==========*/

    @Override
    public boolean equalLabeling(Label<? extends LabelStyle> o) {
        return equalLabelingInternal(o);
    }

    @Override
    public boolean equalLabeling(LabeledObject o) {
        return equalLabelingInternal(o);
    }

    private boolean equalLabelingInternal(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaderedLabel that = (LeaderedLabel) o;
        return labelManager.equalLabeling(that.labelManager);
    }
}
