package agent.logging.logreader;

import java.util.ArrayList;
import java.util.Collections;

import agent.logging.*;

public class LogReader {

	public static void main(String[] args) {
		Identity i = LogFileManager.readFile("Agent.log");
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
