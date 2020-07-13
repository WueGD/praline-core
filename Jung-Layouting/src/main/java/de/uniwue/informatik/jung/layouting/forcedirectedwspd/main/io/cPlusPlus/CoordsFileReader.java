package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.cPlusPlus;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class CoordsFileReader{
	/**
	 * .coords (or .koords)-file is read.
	 * That is only a row of 2-dim. coordinates thus finding the coordinates to one vertex of the graph is only
	 * possible when the order is know.
	 * The order must be the same as in the graph-file and then the same as the
	 * numbering in the {@link Layout}-object (vertices must be of type string with in style "v<i>&lt;number></i>".
	 * So this works under the assumption that the order of vertices is the same in the file specified by path and
	 * in the layout passed.
	 * 
	 * @param path
	 * Path to the file (incl. ending)
	 * @param layout
	 * Must be a initialzied {@link AbstractLayout} to which the read coordinates are added.
	 * The graph assigned to layout must have its vertices as Strings being
	 * a "v" + a number (numbers from 0 or 1 increasing).
	 * The order of the vertices gotten by the numbering must be the same as in the .coords-file
	 * @throws IOException
	 */
	public static void readAndSaveInLayout(String path, AbstractLayout<String, ?> layout) throws IOException {
		File coordsFile = new File(path);
		
		FileInputStream fis = null;
		fis = new FileInputStream(coordsFile);
		Scanner sc = new Scanner(fis);
		
		String all = sc.nextLine();
		LinkedHashMap<Integer, String> verticesMap = new LinkedHashMap<Integer, String>();
		for(String s: layout.getGraph().getVertices()){
			verticesMap.put(Integer.parseInt(s.substring(1)), s);
		}
		double xCoord = 0;
		String coordinateString = "";
		int vertexCount = 1;
		if (verticesMap.containsKey(0)) {
			vertexCount = 0;
		}
		for(char c: all.toCharArray()){
			if(c == ','){
				xCoord = Double.parseDouble(coordinateString);
				coordinateString = "";
			}
			else if(c == ';'){
				double yCoord = Double.parseDouble(coordinateString);
				layout.setLocation(verticesMap.get(vertexCount), xCoord, yCoord);
				vertexCount++;
				coordinateString = "";
			}
			else{
				coordinateString += c;
			}
		}
		
		sc.close();
		fis.close();
	}
}
