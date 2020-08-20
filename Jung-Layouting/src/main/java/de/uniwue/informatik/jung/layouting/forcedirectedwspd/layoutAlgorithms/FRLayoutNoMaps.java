package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify.FRLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.*;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify.UndirectedSparseGraph;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.graph.Graph;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * {@link FRLayout} is based essentially on {@link Map}s, especially the locatians as map V->points
 * and {@link FRVertexData} as map V->points (needed computation-intern)
 * 
 * Those two are replaced by {@link VertexTriple}s and {@link EdgeTriple}.
 * See there for more.
 * <p>
 * Similarly also JUNG-graphs are based on those {@link Map}s. Thus these are not
 * used for computation-intern applications any more.
 * Instead lists for the vertices and edges are used.
 * The savings in time by changing the datastructures seem to be some not so large constant factor in practice.
 * So the difference is not that big.
 * <p>
 * Warning! JUNG allows a "locking" of vertices in its {@link Layout}-algorithms.
 * This is not implemented in this class and the classes inheriting from this class.
 * Lockings are ignored.
 */
public class FRLayoutNoMaps<V, E> extends FRLayout<VertexTriple<V>, EdgeTriple<V,E>>{
	
	protected List<VertexTriple<V>> vertices;
	protected List<EdgeTriple<V,E>> edges;
	protected int numberOfVertices;
	
	/**
	 * This map should not be used, it was only introduced to answer
	 * {@link FRLayoutNoMaps#transformVertexData(Object)} faster
	 * and is needed in the constructor
	 */
	protected Map<V, VertexTriple<V>> vertexData2VertexTriple;
	
	/*
     * Constructors
	 */
	
	
	/**
	 * Do not use this constructor but the other one!
	 * This constructor is only for the classes inheriting from this class.
	 */
	protected FRLayoutNoMaps(){
		super(new UndirectedSparseGraph<VertexTriple<V>, EdgeTriple<V,E>>());
	}
	
	/**
	 * In this constructor a normal JUNG-{@link Graph} is read and {@link VertexTriple}s and
	 * {@link EdgeTriple}s are created.
	 * 
	 * @param graph
	 * @param d
	 */
	public FRLayoutNoMaps(Graph<V,E> graph, Dimension d) {
		this(graph, d, Constants.random.nextLong());
	}

	/**
	 * In this constructor a normal JUNG-{@link Graph} is read and {@link VertexTriple}s and
	 * {@link EdgeTriple}s are created.
	 *
	 * @param graph
	 * @param d
	 * @param seed
	 */
	public FRLayoutNoMaps(Graph<V,E> graph, Dimension d, long seed) {
		//there locations is touched -> must be changed
		//<Replace that call in AbstractLayout  (= line "super(g, new RandomLocationTransformer<V>(d), d);" in FRLayout)>
		//The new created graph is then saved at this.graph
		super(new UndirectedSparseGraph<VertexTriple<V>, EdgeTriple<V,E>>());
		
		//Read graph. This is the only step that works with the JUNG-graph-structure
		vertexData2VertexTriple = new LinkedHashMap<V, VertexTriple<V>>(graph.getVertexCount());
		vertices = new ArrayList<VertexTriple<V>>(graph.getVertexCount());
		RandomLocationTransformer<V> randomLocationTransformer = new RandomLocationTransformer<V>(d, seed);
		for(V v: graph.getVertices()){
			VertexTriple<V> newVTriple =
					new VertexTriple<V>(v, randomLocationTransformer.apply(v), new FRVertexData());
			vertices.add(newVTriple);
			vertexData2VertexTriple.put(v, newVTriple);
		}
		edges = new ArrayList<EdgeTriple<V, E>>(graph.getEdgeCount());
		for(E e: graph.getEdges()){
			edges.add(new EdgeTriple<V, E>
					(e, vertexData2VertexTriple.get(graph.getEndpoints(e).getFirst()), vertexData2VertexTriple.get(graph.getEndpoints(e).getSecond())));
		}
		
		
		this.size = d;
		this.numberOfVertices = graph.getVertexCount();
		//</Replace that call in AbstractLayoutt>
		//<Replace that call in FRLayout>
        initialize();
        max_dimension = Math.max(d.height, d.width);
		//</Replace that call in FRLayout>
		
        //But still set the variables not wanted to be used to null.
        //If they are used one can see it when a NullPointerException is thrown.
		this.locations = null;
		this.frVertexData = null;
//		this.graph = null;
		
		//do not set graph to null but to an extra for that created graph-object
		this.graph = new TripleSetGraph<V, E>(vertices, edges);
	}
	
	/*
	 * New Getters
	 */
	public List<VertexTriple<V>> getVertices(){
		return vertices;
	}
	
	public List<EdgeTriple<V,E>> getEdges(){
		return edges;
	}
	
	public int getVertexCount(){
		return numberOfVertices;
	}
	
	
	/**
	 * Creates from the saved vertex- and edge-lists again a JUNG-{@link Graph}.
	 * <p>
	 * But care!
	 * This graph is not the graph you would expect to get via getGraph().
	 * There the graph would have to be of type
	 * Graph&lt;VertexTriple&lt;V>, EdgeTriple&lt;V,E>>.
	 * This would thus include positions of the vertices and could be interpreted as a drawing.
	 * Instead here a {@link UndirectedGraph} is created only of the vertex-set V and edge-set E
	 * where the location-information is removed.
	 */
	public Graph<V,E> getUndirectedGraph_V_E(){
		UndirectedSparseGraph<V, E> newGraph = new UndirectedSparseGraph<V,E>();
		for(VertexTriple<V> t: vertices){
			newGraph.addVertex(t.get1());
		}
		for(EdgeTriple<V,E> e: edges){
			newGraph.addEdge(e.get1(), e.get2().get1(), e.get3().get1());
		}
		return newGraph;
	}
	
	/**
	 * Returns the corresponding {@link FRLayout}&lt;V,E>.
	 * A new layout is created and the values of the locations of the vertices are taken.
	 * <p>
	 * This serves primary as method to create a layout with properties like a classical
	 * Layout&lt;V,E> to make it easier to visualize/compare/evaluate this layout.
	 * 
	 * @return
	 */
	public FRLayout<V,E> getCorresponding_V_E_Layout(){
		FRLayout<V,E> newLayout = new FRLayout<V, E>(this.getUndirectedGraph_V_E(), this.getSize());
		for(VertexTriple<V> t: vertices){
			newLayout.apply(t.get1()).setLocation(t.get2());
		}
		newLayout.setMaxIterations(-1); //so that the new layout is not changed (recalculated)
		return newLayout;
	}
	
	
	/*
	 * new help-method
	 */
	public static <V> Tuple<Tuple<Double,Double>, Tuple<Double,Double>> getXMinXMax_yMinYMax(
			Collection<VertexTriple<V>> tripelmenge){
    	double minX = Double.POSITIVE_INFINITY;
    	double maxX = Double.NEGATIVE_INFINITY;
    	double minY = Double.POSITIVE_INFINITY;
    	double maxY = Double.NEGATIVE_INFINITY;
    	for(VertexTriple<V> t: tripelmenge){
    		if(t.get2().getX() < minX){minX = t.get2().getX();}
    		if(t.get2().getX() > maxX){maxX = t.get2().getX();}
    		if(t.get2().getY() < minY){minY = t.get2().getY();}
    		if(t.get2().getY() > maxY){maxY = t.get2().getY();}
    	}
    	return new Tuple<Tuple<Double,Double>,Tuple<Double,Double>>(new Tuple<Double,Double>(minX, maxX), new Tuple<Double,Double>(minY, maxY));
	}
	
	/*
	 * All methods, in which locations or frVertexData or getGraph() appear, must to be replaced
	 */
	protected Point2D getCoordinates(VertexTriple<V> t){
		return t.get2();
	}
	@Override
	public Point2D apply(VertexTriple<V> t) {
		return getCoordinates(t);
	}
	/**
	 * For "old" use of the transform function (with vertex data as input parameter
	 * instead of a {@link VertexTriple}).
	 * One should prefer to call {@link FRLayoutNoMaps#transform(VertexTriple)}
	 * instead of this.
	 * 
	 * @param v
	 * @return
	 */
	public Point2D transformVertexData(V v){
		if(!vertexData2VertexTriple.containsKey(v)){
			return null;
		}
		return vertexData2VertexTriple.get(v).getLocation();
	}
	@Override
	public double getX(VertexTriple<V> t) {
        assert getCoordinates(t) != null : "Cannot getX for an unmapped vertex "+t;
        return getCoordinates(t).getX();
	}
	@Override
	public double getY(VertexTriple<V> t) {
        assert getCoordinates(t) != null : "Cannot getY for an unmapped vertex "+t;
        return getCoordinates(t).getY();
	}
	@Override
	protected void offsetVertex(VertexTriple<V> t, double xOffset, double yOffset) {
		Point2D c = getCoordinates(t);
        c.setLocation(c.getX()+xOffset, c.getY()+yOffset);
		setLocation(t, c);
	}
	@Override
	public void setLocation(VertexTriple<V> picked, double x, double y) {
		Point2D coord = getCoordinates(picked);
		coord.setLocation(x, y);
	}
	@Override
	public void setLocation(VertexTriple<V> picked, Point2D p) {
		Point2D coord = getCoordinates(picked);
		coord.setLocation(p);
	}
	@Override
	public void initialize() {
    	doInit();
    }
    private void doInit() {
//    	Graph<V,E> graph = getGraph(); //this line is removed and every other use of "graph", too
    	Dimension d = getSize();
    	if(/*graph != null &&*/ d != null) {
    		currentIteration = 0;
    		temperature = d.getWidth() / 10;

    		forceConstant =
    			Math
    			.sqrt(d.getHeight()
    					* d.getWidth()
    					/ /*graph.getVertexCount()*/ vertices.size()); //Replacement

    		attraction_constant = attraction_multiplier * forceConstant;
    		repulsion_constant = repulsion_multiplier * forceConstant;
    	}
    }
    
    @Deprecated
    @Override
    public void setGraph(Graph<VertexTriple<V>,EdgeTriple<V, E>> graph) {
    	System.err.println("Execution of setGraph() in "+this.getClass().getName()+" shall not be done any more!"
    			+ "\nIf wanted adapt source-code.");
    };
    
    @Override
    public void setSize(Dimension size) {
//    	if(initialized == false) {
//			setInitializer(new RandomLocationTransformer<V>(size));
//		}
    	if(size != null /*&& graph != null*/) { //Removement
			
			Dimension oldSize = this.size;
			this.size = size;
			initialize();
			
			if(oldSize != null) {
				adjustLocations(oldSize, size);
			}
		}
        max_dimension = Math.max(size.height, size.width);
    }
    private void adjustLocations(Dimension oldSize, Dimension size) {

		int xOffset = (size.width - oldSize.width) / 2;
		int yOffset = (size.height - oldSize.height) / 2;

		// now, move each vertex to be at the new screen center
		while(true) {
		    try {
		    	for(VertexTriple<V> t: vertices){
		            offsetVertex(t, xOffset, yOffset);
		        }
		        break;
		    } catch(ConcurrentModificationException cme) {
		    }
		}
	}
	
	
	/*
	 * Methods in which frVertexData from FrLayout is used
	 */
	@Override
	protected FRVertexData getFRData(VertexTriple<V> t) {
		return t.get3();
    }
	
	/*
	 * Methods that were overwritten to replace the use of v by the use of VertexTriples
	 */
	/**
     * Moves the iteration forward one notch, calculation attraction and
     * repulsion between vertices and edges and cooling the temperature.
     */
	@Override
    public synchronized void step() {
        currentIteration++;

        /**
         * Calculate repulsion
         */
        while(true) {

            try {
                for(VertexTriple<V> t : vertices) {
                    calcRepulsion(t);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }

        /**
         * Calculate attraction
         */
        while(true) {
            try {
                for(EdgeTriple<V,E> e : edges) {

                    calcAttraction(e);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }


        while(true) {
            try {
                for(VertexTriple<V> t : vertices) {
//                    if (isLocked(t)) continue;
                    calcPositions(t);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }
        cool();
    }
	
	@Override
    protected synchronized void calcPositions(VertexTriple<V> t) {
        FRVertexData fvd = getFRData(t);
        if(fvd == null) return;
        Point2D xyd = apply(t);
        double deltaLength = Math.max(EPSILON, fvd.norm());

        double newXDisp = fvd.getX() / deltaLength
                * Math.min(deltaLength, temperature);

        if (Double.isNaN(newXDisp)) {
        	throw new IllegalArgumentException(
                "Unexpected mathematical result in FRLayout:calcPositions [xdisp]"); }

        double newYDisp = fvd.getY() / deltaLength
                * Math.min(deltaLength, temperature);
        xyd.setLocation(xyd.getX()+newXDisp, xyd.getY()+newYDisp);

        double borderWidth = getSize().getWidth() / 50.0;
        double newXPos = xyd.getX();
        if (newXPos < borderWidth) {
            newXPos = borderWidth + Randomness.random.nextDouble() * borderWidth * 2.0;
        } else if (newXPos > (getSize().getWidth() - borderWidth)) {
            newXPos = getSize().getWidth() - borderWidth - Randomness.random.nextDouble()
                    * borderWidth * 2.0;
        }

        double newYPos = xyd.getY();
        if (newYPos < borderWidth) {
            newYPos = borderWidth + Randomness.random.nextDouble() * borderWidth * 2.0;
        } else if (newYPos > (getSize().getHeight() - borderWidth)) {
            newYPos = getSize().getHeight() - borderWidth
                    - Randomness.random.nextDouble() * borderWidth * 2.0;
        }

        xyd.setLocation(newXPos, newYPos);
    }

	@Override
    protected void calcAttraction(EdgeTriple<V,E> e) {
    	VertexTriple<V> v1 = e.getVertexA();
    	VertexTriple<V> v2 = e.getVertexB();
//        boolean v1_locked = isLocked(v1);
//        boolean v2_locked = isLocked(v2);

//        if(v1_locked && v2_locked) {
//        	// both locked, do nothing
//        	return;
//        }
        Point2D p1 = apply(v1);
        Point2D p2 = apply(v2);
        if(p1 == null || p2 == null) return;
        double xDelta = p1.getX() - p2.getX();
        double yDelta = p1.getY() - p2.getY();

        double deltaLength = Math.max(EPSILON, Math.sqrt((xDelta * xDelta)
                + (yDelta * yDelta)));

        double force = (deltaLength * deltaLength) / attraction_constant;

        if (Double.isNaN(force)) { force = 1; }
        if (Double.isInfinite(force)) { force = 1; }

        double dx = (xDelta / deltaLength) * force;
        double dy = (yDelta / deltaLength) * force;
//        if(v1_locked == false) {
        	FRVertexData fvd1 = getFRData(v1);
        	fvd1.offset(-dx, -dy);
//        }
//        if(v2_locked == false) {
        	FRVertexData fvd2 = getFRData(v2);
        	fvd2.offset(dx, dy);
//        }
    }

    protected void calcRepulsion(VertexTriple<V> v1) {
        FRVertexData fvd1 = getFRData(v1);
        if(fvd1 == null)
            return;
        fvd1.setLocation(0, 0);

        try {
            for(VertexTriple<V> v2 : vertices) {

//                if (isLocked(v2)) continue;
                if (v1 != v2) {
                    Point2D p1 = apply(v1);
                    Point2D p2 = apply(v2);
                    if(p1 == null || p2 == null) continue;
                    double xDelta = p1.getX() - p2.getX();
                    double yDelta = p1.getY() - p2.getY();

                    double deltaLength = Math.max(EPSILON, Math
                            .sqrt((xDelta * xDelta) + (yDelta * yDelta)));

                    double force = (repulsion_constant * repulsion_constant) / deltaLength;

                    if (Double.isNaN(force)) { throw new RuntimeException(
                    "Unexpected mathematical result in FRLayout:calcPositions [repulsion]"); }

                    fvd1.offset((xDelta / deltaLength) * force,
                            (yDelta / deltaLength) * force);
                }
            }
        } catch(ConcurrentModificationException cme) {
            calcRepulsion(v1);
        }
    }
}
