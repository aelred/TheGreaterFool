javac -classpath . com/botbox/util/*.java
javac -classpath . se/sics/tac/util/*.java
javac -classpath . se/sics/tac/aw/*.java
javac -classpath . agent/*.java
javac -classpath . agent/hotel/*.java
javac -classpath . agent/flight/*.java
javac -classpath . agent/entertainment/*.java
javac -classpath . agent/logging/*.java
javac -classpath hamcrest.jar:junit.jar:. agent/test/*.java
jar cfm tacagent.jar AWManifest.txt com/botbox/util/*.class se/sics/tac/aw/*.class se/sics/tac/util/*.class agent/*.class agent/hotel/*.class agent/flight/*.class agent/entertainment/*.class agent/logging/*.class
