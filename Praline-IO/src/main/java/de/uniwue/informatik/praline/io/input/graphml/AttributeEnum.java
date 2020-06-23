package de.uniwue.informatik.praline.io.input.graphml;

public enum AttributeEnum
{
    NODE_LABEL("label", "node"),
    LINK_LABEL("LinkLabel", "edge");

    private final String attrName;
    private final String _for;

    AttributeEnum(String attrName, String _for)
    {
        this.attrName = attrName;
        this._for = _for;
    }

    public String getAttrName()
    {
        return this.attrName;
    }

    public String getFor()
    {
        return this._for;
    }
}
