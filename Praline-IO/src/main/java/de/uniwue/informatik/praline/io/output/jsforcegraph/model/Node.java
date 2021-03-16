
package de.uniwue.informatik.praline.io.output.jsforcegraph.model;

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
}
