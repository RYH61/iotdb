#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

DATANODE_CONF="`dirname "$0"`/../conf"
dn_rpc_port=`sed '/^dn_rpc_port=/!d;s/.*=//' ${DATANODE_CONF}/iotdb-datanode.properties`
dn_rpc_address=`sed '/^dn_rpc_address=/!d;s/.*=//' ${DATANODE_CONF}/iotdb-datanode.properties`

if  type lsof > /dev/null 2>&1 ; then
  PID=$(lsof -t -i:${dn_rpc_port} -sTCP:LISTEN)
elif type netstat > /dev/null 2>&1 ; then
  PID=$(netstat -anp 2>/dev/null | grep ":${dn_rpc_port} " | grep ' LISTEN ' | awk '{print $NF}' | sed "s|/.*||g" )
else
  echo "{"
  echo -e "\t\"result\": false,"
  echo -e "\t\"message\": {"
  echo -e "\t\t\"ip\": \""$dn_rpc_address"\","
  echo -e "\t\t\"port\": \""$dn_rpc_port"\""
  echo -e "\t}"
  echo "}"
fi

PIDS=$(ps ax | grep -i 'DataNode' | grep java | grep -v grep | awk '{print $1}')
if [ -z "$PID" ]; then
  echo "{"
  echo -e "\t\"result\": false,"
  echo -e "\t\"message\": {"
  echo -e "\t\t\"ip\": \""$dn_rpc_address"\","
  echo -e "\t\t\"port\": \""$dn_rpc_port"\""
  echo -e "\t}"
  echo "}"
elif [[ "${PIDS}" =~ "${PID}" ]]; then
  pwd_path=$(pwd)
  cwd_path=$(ls -l /proc/$PID | grep "cwd ->" | grep -v grep | awk '{print $NF}')
  if [[ "$pwd_path" =~ "$cwd_path" ]]; then
    echo "{"
    echo -e "\t\"result\": true,"
    echo -e "\t\"message\": {"
    echo -e "\t\t\"ip\": \""$dn_rpc_address"\","
    echo -e "\t\t\"port\": \""$dn_rpc_port"\""
    echo -e "\t}"
    echo "}"
  else
    echo "{"
    echo -e "\t\"result\": false,"
    echo -e "\t\"message\": {"
    echo -e "\t\t\"ip\": \""$dn_rpc_address"\","
    echo -e "\t\t\"port\": \""$dn_rpc_port"\""
    echo -e "\t}"
    echo "}"
  fi
else
  echo "{"
  echo -e "\t\"result\": false,"
  echo -e "\t\"message\": {"
  echo -e "\t\t\"ip\": \""$dn_rpc_address"\","
  echo -e "\t\t\"port\": \""$dn_rpc_port"\""
  echo -e "\t}"
  echo "}"
fi