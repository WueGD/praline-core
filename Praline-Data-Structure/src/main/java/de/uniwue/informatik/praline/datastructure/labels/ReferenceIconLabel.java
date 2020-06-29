package de.uniwue.informatik.praline.datastructure.labels;

import de.uniwue.informatik.praline.datastructure.ReferenceObject;

public class ReferenceIconLabel extends IconLabel implements ReferenceObject
{
    private String reference;

    public ReferenceIconLabel(String reference)
    {
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
