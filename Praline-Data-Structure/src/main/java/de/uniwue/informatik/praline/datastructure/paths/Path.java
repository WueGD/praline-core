package de.uniwue.informatik.praline.datastructure.paths;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.uniwue.informatik.praline.datastructure.styles.PathStyle;

/**
 * A {@link Path} is some type of curve/course connecting two endpoints.
 * Here it is mainly used to specify the course of an {@link de.uniwue.informatik.praline.datastructure.graphs.Edge}.
 * So for an {@link de.uniwue.informatik.praline.datastructure.graphs.Edge}, a drawing algorithm should create one
 * (or more) {@link Path}s.
 *
 * A {@link Path} may also be used for {@link de.uniwue.informatik.praline.datastructure.labels.LeaderedLabel}s.
 *
 * The most typical and currently only available type/implementation is {@link PolygonalPath}.
 * Later there may be more types added, e. g. for Bezier curves.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PolygonalPath.class, name = "polygonalPath")
})
public abstract class Path {

    /*==========
     * Instance variables
     *==========*/

    private PathStyle pathStyle;


    /*==========
     * Constructors
     *==========*/

    protected Path() {
        this(null);
    }

    protected Path(PathStyle pathStyle) {
        this.pathStyle = pathStyle == null ? PathStyle.DEFAULT_PATH_STYLE : pathStyle;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public PathStyle getPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(PathStyle pathStyle) {
        this.pathStyle = pathStyle;
    }

    /*==========
     * Other methods
     *==========*/

    public abstract void translate(double xOffset, double yOffset);
}
