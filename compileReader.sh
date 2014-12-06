javac -classpath . agent/logging/logreader/*.java
javac -classpath . agent/logging/*.java
jar cfm logreader.jar AWManifest.txt agent/logging/logreader/*.class agent/logging/*.class
