package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DataAsJSonFile {	
	
	/**
	 * Creates a ".json"-file at path.
	 * 
	 * @param filecontent
	 * Content to be saved
	 * @param path
	 * path of the file to be written incl. ending of the file
	 * @return
	 * successful?
	 */
	public static boolean writeFile(Object filecontent, String path) {
		GsonBuilder gb = new GsonBuilder();
		
		gb = gb.serializeSpecialFloatingPointValues();
		
		Gson gson = gb.create();
		String json = gson.toJson(filecontent);
		
		try {
			FileWriter writer = new FileWriter(path);
			writer.write(json);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param path
	 * path of the file to be read (incl. ending of the file)
	 * @param classOfTheObjectSavedInThisFile
	 * Which class was is instance saved there from?
	 * @return
	 * The instance saved in this file as Java-class-instance
	 */
	public static <T> T getFileContent(String path, Class<T> classOfTheObjectSavedInThisFile) {
		GsonBuilder gb = new GsonBuilder();

		gb = gb.serializeSpecialFloatingPointValues();

		gb.create();

		Gson gson = new Gson();
		
		T object = null;

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			object = gson.fromJson(br, classOfTheObjectSavedInThisFile);

		} catch (IOException e) {
			return null;
		}
		return object;
	}
}
