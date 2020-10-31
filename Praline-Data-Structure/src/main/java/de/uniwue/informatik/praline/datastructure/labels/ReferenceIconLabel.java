package de.uniwue.informatik.praline.datastructure.labels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;

public class ReferenceIconLabel extends IconLabel implements ReferenceObject
{
    private String reference;

    public ReferenceIconLabel(String reference) {
        super();
        this.reference = reference;
    }

    @JsonCreator
    public ReferenceIconLabel(
            @JsonProperty("reference") final String reference,
            @JsonProperty("placement") final Placement placement,
            @JsonProperty("horizontalPlacement") final HorizontalPlacement horizontalPlacement,
            @JsonProperty("verticalPlacement") final VerticalPlacement verticalPlacement,
            @JsonProperty("showLabel") final boolean showLabel,
            @JsonProperty("shape") final Shape shape
    ) {
        super(placement, horizontalPlacement, verticalPlacement, showLabel, shape);
        this.reference = reference;
    }

    @Override
    public String getReference()
    {
        return this.reference;
    }

    @Override
    public void setReference(String reference)
    {
        this.reference = reference;
    }
}
