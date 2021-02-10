package de.uniwue.informatik.praline.datastructure.labels.styles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class LabelStyle {

    /*==========
     * Default values
     *==========*/

    public static final String DEFAULT_DESCRIPTION = "no description";
    public static final boolean DEFAULT_SHOW_LABEL = true;
    public static final Placement DEFAULT_PLACEMENT = Placement.FREE;
    public static final HorizontalPlacement DEFAULT_HORIZONTAL_PLACEMENT = HorizontalPlacement.FREE;
    public static final VerticalPlacement DEFAULT_VERTICAL_PLACEMENT = VerticalPlacement.FREE;

    /*==========
     * Instance variables
     *==========*/

    private String description;
    private boolean showLabel;
    private Placement placement;
    private HorizontalPlacement horizontalPlacement;
    private VerticalPlacement verticalPlacement;


    /*==========
     * Constructors
     *==========*/

    public LabelStyle() {
        this (DEFAULT_DESCRIPTION, DEFAULT_SHOW_LABEL, DEFAULT_PLACEMENT, DEFAULT_HORIZONTAL_PLACEMENT,
                DEFAULT_VERTICAL_PLACEMENT);
    }

    @JsonCreator
    public LabelStyle(
            @JsonProperty("description") final String description,
            @JsonProperty("showLabel") final boolean showLabel,
            @JsonProperty("placement") final Placement placement,
            @JsonProperty("horizontalPlacement") final HorizontalPlacement horizontalPlacement,
            @JsonProperty("verticalPlacement") final VerticalPlacement verticalPlacement
    ) {
        this.description = description == null ? DEFAULT_DESCRIPTION : description;
        this.showLabel = showLabel;
        this.placement = placement == null ? DEFAULT_PLACEMENT : placement;
        this.horizontalPlacement = horizontalPlacement == null ? DEFAULT_HORIZONTAL_PLACEMENT : horizontalPlacement;
        this.verticalPlacement = verticalPlacement == null ? DEFAULT_VERTICAL_PLACEMENT : verticalPlacement;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
