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

echo "``````````````````````"
echo Starting Package Times
echo "``````````````````````"

enable_cea=$1
enable_license=$2

echo cea_enable=$enable_cea >> node-commons/src/assembly/resources/conf/iotdb-common.properties
echo cea_memory=$enable_license >> node-commons/src/assembly/resources/conf/iotdb-common.properties

#mvn clean package install -pl distribution -am -Drat.skip=true -Dspotless.check.skip=true -DskipTests

echo ``````````````````````
echo Starting Rename
echo ``````````````````````

cd distribution/target/apache-iotdb-1.*-SNAPSHOT-all-bin

if [ -f "CirroData-TimeS-1.0" ] || [ -d "CirroData-TimeS-1.0" ];then
  rm CirroData-TimeS-1.0
fi

if [ -f "CirroData-TimeS-1.0.tar.gz" ]; then
  rm CirroData-TimeS-1.0.tar.gz
fi

if [ -f "version.json" ]; then
  rm version.json
fi

if [ -d "CirroDataTimeS" ]; then
  rm CirroDataTimeS
fi

scp -r apache-iotdb-1.*-SNAPSHOT-all-bin CirroData-TimeS-1.0


if [ "$1" == "true"  ]; then
  echo '[' >> version.json
  echo '{' >> version.json
  echo '"packageName" : "CirroData-TimeS-1.0.tar.gz",' >> version.json
  echo '"name" : "CirroDataTimeS",' >> version.json
  echo '"version": "1.0"' >> version.json
  echo '}' >> version.json
  echo ']' >> version.json

  mkdir CirroDataTimeS
  cd CirroDataTimeS
  mkdir config
  cd ..
  scp -r CirroData-Times-1.0/conf/configuration-cea.xml CirroDataTimeS/config/
  tar -zcvf CirroData-TimeS-1.0.tar.gz CirroData-TimeS-1.0
  tar -czvf CirroData-TimeS-1.0.0.tar.gz CirroDataTimeS CirroData-TimeS-1.0.tar.gz version.json
else
  tar -zcvf CirroData-TimeS-1.0.tar.gz CirroData-TimeS-1.0
fi

