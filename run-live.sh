#!/bin/bash
git pull --ff-only
./compileNoJUnit.sh
echo Checked out and compiled revision $(git rev-parse HEAD). >> ~/public_html/the-greater-fool-log.txt
java -jar tacagent.jar 2>&1>/dev/null | tee -a ~/public_html/the-greater-fool-log.txt
