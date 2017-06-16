#!/bin/bash

cd ..
path=`pwd`
log_dir=$path"/logs/"

if [ ! -d $log_dir ]; then
      mkdir $log_dir
fi
nohup java -jar -Xmn128m -Xms512m -Xmx512m  smys-platform-rpcservice.jar >logs/out 2>&1 &