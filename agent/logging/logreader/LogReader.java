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
	
	public static void main(String[] args) throws NoSuchChildException {
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
			i.printIDTreeString();
			break;
		default:
			root.i = i;
			ArrayList<LogEntry> log;
			log = root.getLogs();
			String message;
			for (LogEntry le : log) {
				message = le.getTimeString() + "\t";
				message += le.getAuthorString() + "\t";
				message += le.getMessage();
				System.out.println(message);
			}
			break;
		}		
	}
	
}

class Tree {
	public static final int NO_OUTPUT = 1000;
	
	public static int numTabs = 0;
	
	public List<Tree> children = new ArrayList<Tree>();
	public int importance = NO_OUTPUT;
	public int descImportance = NO_OUTPUT;
	public String name;
	public Identity i;
	
	public Tree(String name) {
		this.name = name;
	}
	
	public ArrayList<LogEntry> getLogs() throws NoSuchChildException {
		System.out.println("Getting logs from:");
		System.out.println(name);
		ArrayList<LogEntry> log = getLogs_();
		Collections.sort(log);
		return log;
	}
	
	private ArrayList<LogEntry> getLogs_() throws NoSuchChildException {
		ArrayList<LogEntry> log = new ArrayList<LogEntry>();
		if (importance <= AgentLogger.MAX_IMPORTANCE)
			log.addAll(i.getOwnLogs(importance));
		if (descImportance <= AgentLogger.MAX_IMPORTANCE)
			log.addAll(i.getDescLogs(importance));
		numTabs++;
		for (Tree child : children) {
			for (int nt = 1; nt <= numTabs; nt++) System.out.print("\t");
			System.out.println();
			System.out.println(name);
			child.i = i.getDescendent(child.name);
			log.addAll(child.getLogs_());
		}
		numTabs--;
		return log;
	}
	
	public void updateChild(String path, int importance) {
		updateChild(path, importance, NO_OUTPUT);
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
			importance = NO_OUTPUT;
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
