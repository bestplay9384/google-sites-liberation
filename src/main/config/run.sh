#!/bin/bash
_base=$(dirname $0)
java -cp ${_base}/google-sites-liberation-1.0.7-SNAPSHOT-jar-with-dependencies.jar:${_base}/config com.google.sites.liberation.util.Main $@
