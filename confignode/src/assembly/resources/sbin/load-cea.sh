#!/bin/sh
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

loadCea() {
  echo ---------------------
  echo Starting Load CEA
  echo ---------------------

  if [ -z "${CONFIGNODE_HOME}" ]; then
    export CONFIGNODE_HOME="$(dirname "$0")/.."
  fi

  CONFIGNODE_CONF=${CONFIGNODE_HOME}/conf

  if [ -z "${CONFIGNODE_LOG_CONFIG}" ]; then
    export CONFIGNODE_LOG_CONFIG="${CONFIGNODE_CONF}/logback-confignode.xml"
  fi

  if [ -n "$JAVA_HOME" ]; then
    for java in "$JAVA_HOME"/bin/amd64/java "$JAVA_HOME"/bin/java; do
      if [ -x "$java" ]; then
        JAVA="$java"
        break
      fi
    done
  else
    JAVA=java
  fi

  if [ -z $JAVA ]; then
    echo Unable to find java executable. Check JAVA_HOME and PATH environment variables. >/dev/stderr
    exit 1
  fi

  CLASSPATH=""
  for f in "${CONFIGNODE_HOME}"/lib/*.jar; do
    CLASSPATH=${CLASSPATH}":"$f
  done

  class="org.apache.iotdb.confignode.service.ConfigNodeLoadCEA"
  iotdb_parms="-Dlogback.configurationFile=${CONFIGNODE_LOG_CONFIG}"
  iotdb_parms="$iotdb_parms -DCONFIGNODE_HOME=${CONFIGNODE_HOME}"
  iotdb_parms="$iotdb_parms -DCONFIGNODE_CONF=${CONFIGNODE_CONF}"
  exec "$JAVA" $iotdb_parms -cp "$CLASSPATH" "$class"
  return $?
}

loadCea
