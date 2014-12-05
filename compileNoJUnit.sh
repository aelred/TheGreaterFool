javac -classpath . com/botbox/util/*.java
javac -classpath . se/sics/tac/util/*.java
javac -classpath . se/sics/tac/aw/*.java
javac -classpath . agent/*.java
javac -classpath . agent/entertainment/*.java
javac -classpath . agent/hotel/*.java
jar cfm tacagent.jar AWManifest.txt com/botbox/util/*.class se/sics/tac/aw/*.class se/sics/tac/util/*.class agent/*.class agent/entertainment/*.class agent/hotel/*.class
