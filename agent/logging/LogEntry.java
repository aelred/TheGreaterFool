package agent.logging;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogEntry implements Comparable<LogEntry>, Serializable {

	private static final long serialVersionUID = -1919166776707829718L;

	private static final SimpleDateFormat formatter = new SimpleDateFormat("EE dd-MMM-yy HH:mm:ss");
	
	private Date timeOfMessage;
	private String message;
	private Identity author;
	
	public LogEntry(String message) {
		timeOfMessage = new Date();
		this.message = message;
	}
	
	public void setAuthor(Identity a) {
		author = a;
	}
	
	public String getAuthorString() {
		return author.toString();
	}
	
	@Override
	public int compareTo(LogEntry lm2) {
		return timeOfMessage.compareTo(lm2.getDate());
	}
	
	public String getMessage() {
		return message;
	}
	
	protected void setMessage(String message) {
		this.message = message;
	}
	
	protected Date getDate() {
		return timeOfMessage;
	}
	
	public String getTimeString() {
		return getTimeString(formatter);
	}
	
	public String getTimeString(String format) {
		return getTimeString(new SimpleDateFormat(format));
	}
	
	private String getTimeString(SimpleDateFormat formatter) {
		return formatter.format(timeOfMessage);
	}

}
