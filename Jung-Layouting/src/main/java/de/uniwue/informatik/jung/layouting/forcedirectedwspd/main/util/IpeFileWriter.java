package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class IpeFileWriter {
	
	/** 
	 * @param pathWithoutFilename
	 * @param fileName (endingless)
	 * Ohne .ipe Ending is added automatically
	 * @param layout
	 */
	public static <V, E> void writeFile(String pathWithoutFilename, String fileName, AbstractLayout<V, E> layout){
		try {
			FileWriter fw = new FileWriter(pathWithoutFilename+File.separator+fileName+".ipe", false);
			
			fw.append(IpeDraw.getIpePreamble());
			fw.append(IpeDraw.getIpeConf());
			
			//Values for Scaling from the unknown Dimension of layout to 1000
			double xSkal = 1000/layout.getSize().getWidth();
			double ySkal = 1000/layout.getSize().getHeight();
			
			for(V v: layout.getGraph().getVertices()){
				fw.append(drawIpeMark(layout.getX(v)*xSkal, layout.getY(v)*ySkal));
			}
			for(E e: layout.getGraph().getEdges()){
				Pair<V> endpoints = layout.getGraph().getEndpoints(e);
				V v1 = endpoints.getFirst();
				V v2 = endpoints.getSecond();
				
				fw.append(drawIpeEdge(layout.getX(v1)*xSkal, layout.getY(v1)*ySkal,
						layout.getX(v2)*xSkal, layout.getY(v2)*ySkal));
			}
			
			fw.append(IpeDraw.getIpeEnd());
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	/*
	 * The following methods are edits from IpeDraw:
	 * 
	 * There it is only possible to pass an Int-Value for the locations of the vertices and edges.
	 * This was changed so that double-values can be passed and used.
	 */
	
	
	/**
	 * Draws a mark of shape "disk" with color "black" and size "normal".
	 * 
	 * @param x
	 *            x-coordinate
	 * @param y
	 *            y-coordinate
	 * @return
	 */
	public static String drawIpeMark(double x, double y) {
		return drawIpeMark(x, y, "disk", "black", "normal");
	}
	/**
	 * Draws a mark.
	 * 
	 * @param x
	 *            x-coordinate
	 * @param y
	 *            y-coordinate
	 * @param shape
	 * 			  shape: disk, fdisk, circle, box, square, fsquare, cross
	 * @param color
	 *            color
	 * @param size
	 *            size: tiny, small, normal, large
	 * @return
	 */
	public static String drawIpeMark(double x, double y, String shape, String color, String size) {
		return "<use name=\"mark/" + shape + "(sx)\" pos=\"" + x + " " + y
				+ "\" size=\"" + size + "\" stroke=\"" + color + "\"/>\n";
	}
	
	/**
	 * Draws an undashed edge between two points with pen width "normal" and
	 * color "black".
	 * 
	 * @param x1
	 *            x-coordinate of point 1
	 * @param y1
	 *            y-coordinate of point 1
	 * @param x2
	 *            x-coordinate of point 2
	 * @param y2
	 *            y-coordinate of point 2
	 * @return
	 */
	public static String drawIpeEdge(double x1, double y1, double x2, double y2) {
		return drawIpePath(new double[] { x1, x2 }, new double[] { y1, y2 });
	}
	
	/**
	 * Draws an undashed path between points with pen width "normal" and color
	 * "black".
	 * 
	 * @param x
	 *            x-coordinates of the points
	 * @param y
	 *            y-coordinates of the points
	 * @return
	 */
	public static String drawIpePath(double[] x, double[] y) {
		return drawIpePath(x, y, "black", "normal", "normal");
	}
	/**
	 * Draws a path between points.
	 * 
	 * @param x
	 *            x-coordinates of the points
	 * @param y
	 *            y-coordinates of the points
	 * @param color
	 *            color
	 * @param pen
	 *            pen width: normal, heavier, fat, ultrafat
	 * @param dash
	 *            dash style: normal, dashed, dotted, dash dotted, dash dot
	 *            dotted
	 * @return
	 */
	public static String drawIpePath(double[] x, double[] y, String color,
			String pen, String dash) {
		String s = "<path stroke=\"" + color + "\" pen=\"" + pen + "\" dash=\""
				+ dash + "\">\n " + x[0] + " " + y[0] + " m\n ";
		for (int i = 1; i < x.length; i++) {
			s += x[i] + " " + y[i] + " l\n ";
		}
		s += "</path>\n";
		return s;
	}
}
