if [ $# -ge 1 ];
then 
    java -jar tacagent.jar 2>&1>/dev/null | grep --line-buffered $1
else
    java -jar tacagent.jar
fi
