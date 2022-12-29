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

build_name=`git describe --tags --dirty=M --always --long`

enable_cea=$1
enable_license=$2

echo cea_enable=$enable_cea >> node-commons/src/assembly/resources/conf/iotdb-common.properties
echo cea_memory=$enable_license >> node-commons/src/assembly/resources/conf/iotdb-common.properties

mvn clean package install -pl distribution -am -Drat.skip=true -Dspotless.check.skip=true -DskipTests

echo ``````````````````````
echo Starting Rename
echo ``````````````````````

cd distribution/target/apache-iotdb-1.*-SNAPSHOT-all-bin

package_name='CirroData-TimeS-'${build_name}
tar_package_name=${package_name}'.tar.gz'
inner_tar_package_name=${package_name}'-inner.tar.gz'
if [ -f ${package_name} ] || [ -d ${package_name} ];then
  rm ${package_name}
fi

if [ -f ${tar_package_name} ]; then
  rm ${tar_package_name}
fi

if [ -f "version.json" ]; then
  rm version.json
fi

if [ -d "CirroDataTimeS" ]; then
  rm CirroDataTimeS
fi

scp -r apache-iotdb-1.*-SNAPSHOT-all-bin ${package_name}

if [ "$1" == "true"  ]; then
  echo '[' >> version.json
  echo '{' >> version.json
  echo '"packageName" : "'${inner_tar_package_name}'",' >> version.json
  echo '"name" : "CirroDataTimeS",' >> version.json
  echo '"version": "'$build_name'"' >> version.json
  echo '}' >> version.json
  echo ']' >> version.json

  mkdir CirroDataTimeS
  cd CirroDataTimeS
  mkdir config
  cd ..
  scp -r ${package_name}/conf/configuration-cea.xml CirroDataTimeS/config/
  tar -zcvf ${inner_tar_package_name} ${package_name}
  tar -zcvf ${tar_package_name} CirroDataTimeS ${inner_tar_package_name} version.json
else
  tar -zcvf ${tar_package_name} ${package_name}
fi

