
package de.uniwue.informatik.praline.io.model.jsforcegraph;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Link {

    @SerializedName("source")
    @Expose
    private String source;
    @SerializedName("target")
    @Expose
    private String target;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("tags")
    @Expose
    private Map<String, String> tags;
    @SerializedName("state")
    @Expose
    private Map<String, String> state;
    @SerializedName("metrics")
    @Expose
    private Map<String, Double> metrics;
    @SerializedName("properties")
    @Expose
    private Map<String, String> properties;

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, String> properties)
    {
        this.properties = properties;
    }
}
