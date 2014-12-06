package agent.logging;

import java.util.LinkedList;
import java.util.Queue;

public class AgentLogger {

	public static final int INFO = 1;
	public static final int WARNING = 2;
	public static final int ERROR = 3;
	public static final String fileName = "Agent.log";
	
	private Identity root;
	
	public AgentLogger() {
		if (LogFileManager.exists(fileName))
			root = LogFileManager.readFile(fileName);
		else
			root = new Identity("Agent",null);
	}
	
	private AgentLogger(Identity root) {
		this.root = root;
	}
	
	public AgentLogger getSublogger(String subPath) {
		Identity i = root.getChildWithCreation(parseQueue(subPath));
		return new AgentLogger(i);
	}
	
	private static Queue<String> parseQueue(String path) {
		Queue<String> q = new LinkedList<String>();
		if (path.isEmpty())
			return q;
		for (String s : path.split("\\.")) {
			q.add(s);
		}
		return q;
	}
	
	public void log(String message) {
		log(message,AgentLogger.INFO);
	}
	
	public void log(String message, int importance) {
		agent.Agent.logMessage(root.toString(), message);
		LogEntry m = new LogEntry(message);
		root.logMessage(m,importance);
	}
	
	public void logStack(int importance) {
		StackTraceElement[] stack = Thread.getAllStackTraces().get(Thread.currentThread());
		String message = "Stack dump:";
		for (StackTraceElement ste : stack) {
			message += "\n" + ste.toString() ;
		}
		log(message,importance);
	}
	
	public void logExceptionStack(Exception e, int importance) {
		StackTraceElement[] stack = e.getStackTrace();
		String message = "Exception stack dump:";
		for (StackTraceElement ste : stack) {
			message += "\n" + ste.toString() ;
		}
		log(message,importance);
	}
	
	public void save() {
		Identity parent = root.getParent();
		root.setParent(null);
		LogFileManager.writeFile(root.toString() + ".log",root);
		root.setParent(parent);
	}
}
