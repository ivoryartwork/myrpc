#!/bin/bash

exec 3<>/dev/tcp/127.0.0.1/9600
echo -e "shutdown" >&3
exec 3>&-