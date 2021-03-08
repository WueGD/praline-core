
package de.uniwue.informatik.praline.io.output.jsforcegraph.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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

}
