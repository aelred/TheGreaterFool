package agent.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Identity implements Serializable {

	private static final long serialVersionUID = 6961061800526377314L;

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
	
	public Identity getChild(Queue<String> childPath) throws NoSuchChildException {
		if (childPath.isEmpty())
			return this;
		Identity child = getChild(childPath.peek());
		if (child == null) {
			throw new NoSuchChildException(this);
		}
		childPath.remove();
		return child.getChild(childPath);
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
			return getChild(childPath);
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
	
	public String relativePath(Identity ancestor) throws NoSuchAncestorException {
		if (parent == null) 
			throw new NoSuchAncestorException();
		if (parent.equals(ancestor))
			return "." + name;
		return parent.relativePath(ancestor) + "." + name;
	}
	
	public void logMessage(LogEntry m, int importance) {
		m.setAuthor(this);
		if (importance < 2)
			infoLog.add(m);
		else if (importance == 2)
			warningLog.add(m);
		else
			errorLog.add(m);
	}
	
	public void setParent(Identity i) {
		parent = i;
	}

	public ArrayList<LogEntry> getLog() {
		ArrayList<LogEntry> log = new ArrayList<LogEntry>();
		log.addAll(infoLog);
		log.addAll(warningLog);
		log.addAll(errorLog);
		for (Identity child : children) {
			log.addAll(child.getLog());
		}
		return log;
	}
}
