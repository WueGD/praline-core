package de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement;

import de.uniwue.informatik.praline.datastructure.graphs.Port;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PortValues {

    private Port port;
    private Port align;
    private Port root;
    private Port predecessor;
    private Port sink;
    private double globalShift;
    private Map<Port, Double> shift;
    private double x;
    private int position;
    private int layer;

    public PortValues(Port port, Port predecessor, int position, int layer) {
        this.port = port;
        this.align = port;
        this.root = port;
        this.predecessor = predecessor;
        this.sink = port;
        this.shift = new LinkedHashMap<>();
        this.globalShift = Double.MAX_VALUE;
        this.position = position;
        this.layer = layer;
        this.x = Double.MIN_VALUE;
    }

    public Port getAlign() {
        return align;
    }

    public void setAlign(Port align) {
        this.align = align;
    }

    public Port getRoot() {
        return root;
    }

    public void setRoot(Port root) {
        this.root = root;
    }

    public Port getSink() {
        return sink;
    }

    public void setSink(Port sink) {
        this.sink = sink;
    }

    public Map<Port, Double> getShift() {
        return Collections.unmodifiableMap(shift);
    }

    public void addShift(double shift, Port target) {
        if (this.shift.containsKey(target)) {
            this.shift.replace(target, Math.min(shift, this.shift.get(target)));
        } else {
            this.shift.put(target, shift);
        }
    }

    public double getGlobalShift () {
        return globalShift;
    }

    public void setGlobalShift(double globalShift) {
        this.globalShift = globalShift;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public Port getPredecessor() {
        return predecessor;
    }

    public int getPosition() {
        return position;
    }

    public int getLayer() {
        return layer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(port.toString()).append(": ");
        sb.append(" align: ").append(align.toString());
        sb.append(" root: ").append(root.toString());
        sb.append(" pred: ").append(predecessor.toString());
        sb.append(" sink: ").append(sink.toString());
        sb.append(" shift: ").append(shift);
        sb.append(" x: ").append(x);
        sb.append(" pos: ").append(position);
        sb.append(" layer: ").append(layer);
        return sb.toString();
    }
}
