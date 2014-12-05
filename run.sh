if [ $# -ge 1 ];
then 
    java -jar tacagent.jar 2>&1>/dev/null | grep --line-buffered $*
else
    java -jar tacagent.jar
fi
