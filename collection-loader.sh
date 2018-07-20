#!/bin/bash

if type -p java; then
    java -cp .:lib/* CreateCollections $@
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    $JAVA_HOME/bin/java -cp .:lib/* CreateCollections $@
else
    echo "No Java found in Path"
fi


