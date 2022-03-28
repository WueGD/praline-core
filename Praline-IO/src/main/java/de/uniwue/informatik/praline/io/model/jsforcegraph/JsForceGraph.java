
package de.uniwue.informatik.praline.io.model.jsforcegraph;

import java.util.LinkedList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class JsForceGraph
{

    @SerializedName("nodes")
    @Expose
    private List<Node> nodes = new LinkedList<>();
    @SerializedName("links")
    @Expose
    private List<Link> links = new LinkedList<>();
    @SerializedName("groups")
    @Expose
    private List<Group> groups = new LinkedList<>();

    public List<Node> getNodes() {
        return this.nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public List<Link> getLinks() {
        return this.links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<Group> getGroups()
    {
        return groups;
    }

    public void setGroups(List<Group> groups)
    {
        this.groups = groups;
    }
}
