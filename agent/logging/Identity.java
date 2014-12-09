package agent.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Identity implements Serializable {

	private static final long serialVersionUID = 6961061800526377314L;

	private int maxMessageNum;
	protected int getMaxMessageNum() {
		int mmn = maxMessageNum;
		for (Identity child : children) {
			mmn = Math.max(mmn, child.getMaxMessageNum());
		}
		return mmn;
	}
	
	private final String name;
	private Identity parent;
	private List<Identity> children;
	private List<LogEntry> infoLog, warningLog, errorLog;

	public Identity(String name, Identity parent) {
		this.name = name;
		this.parent = parent;
		children = new ArrayList<Identity>();
		infoLog = new ArrayList<LogEntry>();
		warningLog = new ArrayList<LogEntry>();
		errorLog = new ArrayList<LogEntry>();
		maxMessageNum = 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Identity))
			return false;
		if (!name.equals(((Identity) o).getName()))
			return false;
		if (parent == ((Identity) o).getParent())
			return true;
		return false;
	}

	public String getName() {
		return name;
	}

	public Identity getParent() {
		return parent;
	}

	public List<Identity> getChildren() {
		return children;
	}

	private Identity getChild(String childName) {
		for (Identity i : children) {
			if (i.getName().equals(childName))
				return i;
		}
		return null;
	}

	public Identity getDescendent(Queue<String> childPath)
			throws NoSuchChildException {
		if (childPath.isEmpty())
			return this;
		Identity child = getChild(childPath.peek());
		if (child == null) {
			throw new NoSuchChildException(this);
		}
		childPath.remove();
		return child.getDescendent(childPath);
	}

	public Identity getDescendent(String childPath) throws NoSuchChildException {
		return getDescendent(AgentLogger.parseQueue(childPath));
	}

	private Identity createChild(Queue<String> childPath) {
		if (childPath.isEmpty())
			return this;
		Identity newChild = new Identity(childPath.remove(), this);
		children.add(newChild);
		return newChild.createChild(childPath);
	}

	public Identity getChildWithCreation(Queue<String> childPath) {
		try {
			return getDescendent(childPath);
		} catch (NoSuchChildException e) {
			Identity furthestChild = e.getFurthestChild();
			return furthestChild.createChild(childPath);
		}
	}

	@Override
	public String toString() {
		if (parent == null)
			return name;
		return parent.toString() + "." + name;
	}

	public String relativePath(Identity ancestor)
			throws NoSuchAncestorException {
		if (parent == null)
			throw new NoSuchAncestorException();
		if (parent.equals(ancestor))
			return "." + name;
		return parent.relativePath(ancestor) + "." + name;
	}

	public void logMessage(LogEntry m, int importance) {
		m.setAuthor(this);
		String message = m.getMessage();
		message = message.replaceAll("\n", "\n\t");
		m.setMessage(message);
		if (importance < 2)
			infoLog.add(m);
		else if (importance == 2)
			warningLog.add(m);
		else
			errorLog.add(m);
		maxMessageNum = Math.max(m.getMessageNum(),maxMessageNum);
	}

	public void setParent(Identity i) {
		parent = i;
	}

	public ArrayList<LogEntry> getAllLogs(int importance) {
		ArrayList<LogEntry> log = new ArrayList<LogEntry>();
		log.addAll(getOwnLogs(importance));
		for (Identity child : children) {
			log.addAll(child.getAllLogs(importance));
		}
		return log;
	}

	public void printIDTreeString() {
		System.out.println("Printing tree of Identity objects:");
		printIDTTreeString_(0);
		System.out.println("Tree complete");
	}
	
	private void printIDTTreeString_(int numTabs) {
		for (int nt = 1; nt <= numTabs; nt++) System.out.print("\t");
		System.out.println(name);
		for (Identity child : children) {
			child.printIDTTreeString_(numTabs + 1);
		}
	}

	public ArrayList<LogEntry> getDescLogs(int importance) {
		ArrayList<LogEntry> log = new ArrayList<LogEntry>();
		for (Identity child : children) {
			log.addAll(child.getAllLogs(importance));
		}
		return log;
	}
	
	public ArrayList<LogEntry> getOwnLogs(int importance) {
		ArrayList<LogEntry> log = new ArrayList<LogEntry>();
		if (importance <= AgentLogger.INFO)
			log.addAll(infoLog);
		if (importance <= AgentLogger.WARNING)
			log.addAll(warningLog);
		if (importance <= AgentLogger.ERROR)
			log.addAll(errorLog);
		return log;
	}
}
