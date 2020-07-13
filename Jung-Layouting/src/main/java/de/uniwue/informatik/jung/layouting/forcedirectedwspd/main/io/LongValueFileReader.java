package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

public class LongValueFileReader{
	/**
	 * A file is read that has as content only a integer-number (long-value).
	 * 
	 * An example is the .cpuZeitInNs-file from the C++-integration of this project.
	 */
	public static long readLongValueFile(String path) throws IOException,NumberFormatException {
		File file = new File(path);
		
		FileInputStream fis = null;
		fis = new FileInputStream(file);
		Scanner sc = new Scanner(fis);
		
		String content = sc.nextLine();
		
		long value = Long.parseLong(content);
		
		sc.close();
		fis.close();
		
		return value;
	}
}
