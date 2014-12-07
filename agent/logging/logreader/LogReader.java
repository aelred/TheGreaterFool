package agent.logging.logreader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import agent.logging.*;

public class LogReader {

	public static final int STANDARD = 0;
	public static final int EXPECTING_FILE = 1;
	public static final int EXPECTING_IMPORTANCE = 2;
	public static final int PRINT_IDS = 3;
	
	public static void main(String[] args) {
		int state = STANDARD;
		int importance = 1;
		String fileName = "Agent.log";
		Tree root = new Tree("Agent");
		
		for (String arg : args) {
			switch (state) {
			case STANDARD:
				switch (arg) {
				case "-f":
				case "--file":
					state = EXPECTING_FILE;
					break;
				case "-i":
				case "--importance":
					state = EXPECTING_IMPORTANCE;
					break;
				case "-t":
				case "--id-tags":
					state = PRINT_IDS;
					break;
				default:
					root.updateChild(arg, importance);
					break;
				}
				break;
			case EXPECTING_FILE:
				fileName = arg;
				state = STANDARD;
				break;
			case EXPECTING_IMPORTANCE:
				switch (arg) {
				case "INFO":
				case "info":
					importance = 1;
					break;
				case "WARNING":
				case "warning":
					importance = 2;
					break;
				case "ERROR":
				case "error":
					importance = 3;
					break;
				default:
					try {
						importance = Integer.parseInt(arg);
						state = STANDARD;
					} catch (NumberFormatException e) {
						System.err.println("Invalid argument at '" + arg + "'. Was expecting integer.");
						return;
					}
					break;
				}
				break;
			}
		}
		Identity i = LogFileManager.readFile(fileName);
		switch (state) {
		case PRINT_IDS:
			System.out.println(i.getIDTreeString());
			break;
		default:
			
			break;
		}
		
		
		ArrayList<LogEntry> fullLog = i.getLog();
		Collections.sort(fullLog);
		String message;
		for (LogEntry le : fullLog) {
			message = "";
			message += le.getTimeString() + "\t";
			message += le.getAuthorString() + "\t";
			message += le.getMessage();
			System.out.println(message);
		}
	}
	
}

class Tree {
	public List<Tree> children = new ArrayList<Tree>();
	public int importance = 100;
	public int descImportance = 100;
	public String name;
	
	public Tree(String name) {
		this.name = name;
	}
	
	public void updateChild(String path, int importance) {
		updateChild(path, importance, 100);
	}
	
	private void updateChild(String path, int importance, int defaultImportance) {
		if (defaultImportance < importance)
			return;
		String[] split = path.split("\\.",2);
		if (split[0].equals("*")){
			descImportance = importance;
			wipeHigherThan(importance);
			return;
		}
		Tree requestedChild = null;
		for (Tree t : children) {
			if (t.name.equals(split[0]))
				requestedChild = t;
		}
		if (requestedChild == null) {
			requestedChild = new Tree(split[0]);
			children.add(requestedChild);
		}
		if (split.length == 1)
			requestedChild.importance = importance;
		else
			requestedChild.updateChild(split[1], importance, Math.min(defaultImportance,descImportance));
	}
	
	private void wipeHigherThan(int importance) {
		if (importance < this.importance)
			importance = 100;
		if (descImportance < importance)
			return;
		descImportance = 0;
		for (Tree t : children) {
			t.wipeHigherThan(importance);
			if (t.children.isEmpty() && t.descImportance==0 && t.importance==0)
				children.remove(t);
		}
	}
}
