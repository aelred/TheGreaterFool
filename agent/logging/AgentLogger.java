package agent.logging;

import java.util.LinkedList;
import java.util.Queue;

public class AgentLogger {

	private static volatile int messageNum = 0;
	private int nextNum;
	
	public static final int INFO = 1;
	public static final int WARNING = 2;
	public static final int ERROR = 3;
	public static final String fileName = "Agent.log";
	public static final int MAX_IMPORTANCE = 3;
	
	private Identity root;
	
	public AgentLogger() {
		if (LogFileManager.exists(fileName))
			root = LogFileManager.readFile(fileName);
		else
			root = new Identity("Agent",null);
		messageNum = Math.max(messageNum, root.getMaxMessageNum());
	}
	
	private AgentLogger(Identity root) {
		this.root = root;
	}
	
	public AgentLogger getSublogger(String subPath) {
		Identity i = root.getChildWithCreation(parseQueue(subPath));
		return new AgentLogger(i);
	}
	
	public static Queue<String> parseQueue(String path) {
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
		nextNum = ++messageNum;
		agent.Agent.logMessage(root.toString(), message);
		LogEntry m = new LogEntry(message,nextNum);
		root.logMessage(m,importance);
	}
	
	public void logStack(int importance) {
		StackTraceElement[] stack = Thread.getAllStackTraces().get(Thread.currentThread());
		String message = "Stack dump:";
		for (StackTraceElement ste : stack) {
			message += "\n\t" + ste.toString() ;
		}
		log(message,importance);
	}
	
	public void logExceptionStack(Exception e, int importance) {
		StackTraceElement[] stack = e.getStackTrace();
		String message = "Exception stack dump:";
		for (StackTraceElement ste : stack) {
			message += "\n\t" + ste.toString() ;
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
