package de.uniwue.informatik.praline.datastructure.labels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.labels.styles.LabelStyle;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;

import java.util.Objects;

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
            @JsonProperty("labelStyle") final LabelStyle labelStyle,
            @JsonProperty("shape") final Shape shape
    ) {
        super(labelStyle, shape);
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

    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return "reference=" + reference;
    }


    /*==========
     * equalLabeling
     *==========*/

    @Override
    public boolean equalLabeling(Label o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferenceIconLabel that = (ReferenceIconLabel) o;
        return Objects.equals(reference, that.reference);
    }
}
