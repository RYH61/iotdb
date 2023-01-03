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

CONFIGNODE_CONF="$(dirname "$0")/../conf"
cn_internal_port=$(sed '/^cn_internal_port=/!d;s/.*=//' ${CONFIGNODE_CONF}/iotdb-confignode.properties)
cn_internal_address=$(sed '/^cn_internal_address=/!d;s/.*=//' ${CONFIGNODE_CONF}/iotdb-confignode.properties)

if type lsof >/dev/null 2>&1; then
  CN_PID=$(lsof -t -i:${cn_internal_port} -sTCP:LISTEN)
elif type netstat >/dev/null 2>&1; then
  CN_PID=$(netstat -anp 2>/dev/null | grep ":${cn_internal_port} " | grep ' LISTEN ' | awk '{print $NF}' | sed "s|/.*||g")
else
  echo "{"
  echo -e "\t\"result\": false,"
  echo -e "\t\"message\": {"
  echo -e "\t\t\"ip\": \""$cn_internal_address"\","
  echo -e "\t\t\"port\": \""$cn_internal_port"\""
  echo -e "\t}"
  echo "}"
fi

if [ -z "$CN_PID" ]; then
  echo "{"
  echo -e "\t\"result\": false,"
  echo -e "\t\"message\": {"
  echo -e "\t\t\"ip\": \""$cn_internal_address"\","
  echo -e "\t\t\"port\": \""$cn_internal_port"\""
  echo -e "\t}"
  echo "}"
else
  cn_pwd_path=$(pwd)
  cn_cwd_path=$(ls -l /proc/$CN_PID | grep "cwd ->" | grep -v grep | awk '{print $NF}')
  if [[ "$cn_pwd_path" =~ "$cn_cwd_path" ]]; then
    if  type lsof > /dev/null 2>&1 ; then
        DN_PID=$(lsof -t -i:${dn_rpc_port} -sTCP:LISTEN)
      elif type netstat > /dev/null 2>&1 ; then
        DN_PID=$(netstat -anp 2>/dev/null | grep ":${dn_rpc_port} " | grep ' LISTEN ' | awk '{print $NF}' | sed "s|/.*||g" )
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
      if [ -z "$DN_PID" ]; then
        echo "{"
        echo -e "\t\"result\": false,"
        echo -e "\t\"message\": {"
        echo -e "\t\t\"ip\": \""$dn_rpc_address"\","
        echo -e "\t\t\"port\": \""$dn_rpc_port"\""
        echo -e "\t}"
        echo "}"
      elif [[ "${PIDS}" =~ "${DN_PID}" ]]; then
        dn_pwd_path=$(pwd)
        dn_cwd_path=$(ls -l /proc/$DN_PID | grep "cwd ->" | grep -v grep | awk '{print $NF}')
        if [[ "$dn_pwd_path" =~ "$dn_cwd_path" ]]; then
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
  else
    echo "{"
    echo -e "\t\"result\": false,"
    echo -e "\t\"message\": {"
    echo -e "\t\t\"ip\": \""$cn_internal_address"\","
    echo -e "\t\t\"port\": \""$cn_internal_port"\""
    echo -e "\t}"
    echo "}"
  fi
fi