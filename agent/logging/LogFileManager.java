package agent.logging;

import java.io.*;

public class LogFileManager {

	public static boolean exists(String filename) {
		File f = new File(filename);
		return (f.exists() && !f.isDirectory());
	}

	public static Identity readFile(String filename) {
		Identity id = null;
		try (InputStream file = new FileInputStream(filename);
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);) {
			id = (Identity) input.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}

	public static void writeFile(String filename, Identity id) {
		try (OutputStream file = new FileOutputStream(filename);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);) {
			output.writeObject(id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
