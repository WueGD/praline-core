
package de.uniwue.informatik.praline.io.model.jsforcegraph;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Node {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("val")
    @Expose
    private Integer val;
    @SerializedName("tags")
    @Expose
    private Map<String, String> tags;
    @SerializedName("state")
    @Expose
    private Map<String, String> state;
    @SerializedName("metrics")
    @Expose
    private Map<String, Double> metrics;
    @SerializedName("fx")
    @Expose
    private Double fx;
    @SerializedName("fy")
    @Expose
    private Double fy;
    @SerializedName("fz")
    @Expose
    private Double fz;
    @SerializedName("icon")
    @Expose
    private String icon;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVal() {
        return this.val;
    }

    public void setVal(Integer val) {
        this.val = val;
    }

    public Map<String, String> getTags()
    {
        return tags;
    }

    public void setTags(Map<String, String> tags)
    {
        this.tags = tags;
    }

    public Map<String, String> getState()
    {
        return state;
    }

    public void setState(Map<String, String> state)
    {
        this.state = state;
    }

    public Map<String, Double> getMetrics()
    {
        return metrics;
    }

    public void setMetrics(Map<String, Double> metrics)
    {
        this.metrics = metrics;
    }

    public Double getFx()
    {
        return fx;
    }

    public void setFx(Double fx)
    {
        this.fx = fx;
    }

    public Double getFy()
    {
        return fy;
    }

    public void setFy(Double fy)
    {
        this.fy = fy;
    }

    public Double getFz()
    {
        return fz;
    }

    public void setFz(Double fz)
    {
        this.fz = fz;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }
}
