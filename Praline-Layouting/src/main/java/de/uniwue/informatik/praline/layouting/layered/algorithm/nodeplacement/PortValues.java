package de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement;

import de.uniwue.informatik.praline.datastructure.graphs.Port;

import java.util.ArrayList;
import java.util.List;

public class PortValues {

    private Port port;
    private PortValues align;
    private PortValues root;
    private PortValues predecessor;
    private PortValues sink;
    private double shift;
    private double x; //x position in the current run; these are 1 of 4 intermediate run and in the end the final x pos
    private List<Double> xValues; //all x positions in the intermediate runs.
    private int position;
    private int layer;

    public PortValues(Port port) {
        this(port, null, -1, -1);
    }

    public PortValues(Port port, PortValues predecessor, int layer, int position) {
        this.port = port;
        this.predecessor = predecessor;
        this.layer = layer;
        this.position = position;
        this.xValues = new ArrayList<>(4);
        resetValues();
    }

    public void lateInit(PortValues predecessor, int layer, int position) {
        this.predecessor = predecessor;
        this.layer = layer;
        this.position = position;
    }

    public void resetValues() {
        this.align = this;
        this.root = this;
        this.sink = this;
        this.shift = Double.POSITIVE_INFINITY;
        this.x = Double.MIN_VALUE;
    }

    public Port getPort() {
        return port;
    }

    public PortValues getAlign() {
        return align;
    }

    public void setAlign(PortValues align) {
        this.align = align;
    }

    public PortValues getRoot() {
        return root;
    }

    public void setRoot(PortValues root) {
        this.root = root;
    }

    public PortValues getSink() {
        return sink;
    }

    public void setSink(PortValues sink) {
        this.sink = sink;
    }

    public double getShift() {
        return shift;
    }

    public void setShift(double newShift) {
        shift = newShift;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public List<Double> getXValues() {
        return xValues;
    }

    public void addToXValues(double xValue) {
        this.xValues.add(xValue);
    }

    public PortValues getPredecessor() {
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
