package agent.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import agent.logging.*;

public class LogReader {

	public static final int STANDARD = 0;
	public static final int EXPECTING_FILE = 1;
	public static final int EXPECTING_IMPORTANCE = 2;
	public static final int PRINT_IDS = 3;

	public static void main(String[] args) throws NoSuchChildException,
			InvalidPathException {
		if (args.length == 0 || args[0].equals("--help")
				|| args[0].equals("-h")) {
			showUsage();
			return;
		}

		int state = STANDARD;
		int importance = 1;
		String fileName = "Agent.log";
		Tree root = new Tree("agent");

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
						System.err.println("Invalid argument at '" + arg
								+ "'. Was expecting integer.");
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

	private static void showUsage() {
		System.out.println("Usage:");
		System.out.println("java LogReader [flag value]* [path ]+");
		System.out.println("flags, values & descriptions:");
		System.out.println("-h or --help\t\t(no value required) Shows this help screen");
		System.out.println("-f or --file\t\tA log file path. Determines which log file to read. Default is './Agent.log'");
		System.out.println("-i or --importance\t\tInteger between 1 and 3 or the name of an output level. Sets what level output to display for the identity paths that follow");
		System.out.println("-t or --id-tags\t\t(no value required) Shows the tree of identity values in the log file. This should be stated last if specifying a file");
		System.out.println("paths:");
		System.out.println("'*' or regex Agent[.subID]*[.*]? where subID is the name of any identifier (print ids with -t). Using path.* will only include all children of path, not messages within path itself. Specify with two different statements if you require both (e.g. 'path.* path')");
		System.out.println("Caution: if using '*' in an argument, quote or escape it to get desired behaviour");
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
		ArrayList<LogEntry> log = getLogs_();
		Collections.sort(log);
		return log;
	}

	private ArrayList<LogEntry> getLogs_() throws NoSuchChildException {
		ArrayList<LogEntry> log = new ArrayList<LogEntry>();
		if (importance <= AgentLogger.MAX_IMPORTANCE)
			log.addAll(i.getOwnLogs(importance));
		if (descImportance <= AgentLogger.MAX_IMPORTANCE)
			log.addAll(i.getDescLogs(descImportance));
		numTabs++;
		for (Tree child : children) {
			child.i = i.getDescendent(child.name);
			log.addAll(child.getLogs_());
		}
		numTabs--;
		return log;
	}

	public void updateChild(String path, int importance)
			throws InvalidPathException {
		int nameLength = name.length();
		if (path.length() >= nameLength
				&& path.substring(0, nameLength).equals(name))
			if (path.equals(name)) {
				if (this.importance > importance)
					this.importance = importance;
			} else {
				updateChild(path.substring(nameLength + 1), importance,
						NO_OUTPUT);
			}
		else if (path.equals("*")) {
			if (this.importance > importance)
				this.importance = importance;
			updateChild(path, importance, NO_OUTPUT);
		} else {
			System.out.println("References must either be '*' or begin with '"
					+ name + ".'");
			throw new InvalidPathException();
		}
	}

	private void updateChild(String path, int importance, int defaultImportance) {
		if (defaultImportance < importance)
			return;
		String[] split = path.split("\\.", 2);
		if (split[0].equals("*")) {
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
			requestedChild.updateChild(split[1], importance,
					Math.min(defaultImportance, descImportance));
	}

	private void wipeHigherThan(int importance) {
		if (importance < this.importance)
			importance = NO_OUTPUT;
		if (descImportance < importance)
			return;
		descImportance = NO_OUTPUT;
		for (Tree t : children) {
			t.wipeHigherThan(importance);
			if (t.children.isEmpty() && t.descImportance == 0
					&& t.importance == 0)
				children.remove(t);
		}
	}
}

class InvalidPathException extends Exception {
}
