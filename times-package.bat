@REM
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM

@echo off & setlocal enabledelayedexpansion
echo ``````````````````````
echo Starting Package Times
echo ``````````````````````

@REM cea:%1    license:%2
echo cea_enable=%1 >> node-commons/src/assembly/resources/conf/iotdb-common.properties
echo enable_cea=%2 >> node-commons/src/assembly/resources/conf/iotdb-common.properties

call mvn clean package -pl distribution -am -DskipTests

echo ``````````````````````
echo Starting Rename
echo ``````````````````````

cd distribution/target/apache-iotdb-1.0.1-SNAPSHOT-all-bin

if exist CirroData-Times-1.0 del CirroData-Times-1.0
if exist CirroData-TimeS-1.0.tar.gz del CirroData-TimeS-1.0.tar.gz

ren apache-iotdb-1.0.1-SNAPSHOT-all-bin CirroData-Times-1.0

tar -czvf CirroData-TimeS-1.0.tar.gz CirroData-TimeS-1.0

