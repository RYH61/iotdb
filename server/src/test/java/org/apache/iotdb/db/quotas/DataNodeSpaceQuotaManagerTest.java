/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.quotas;

import org.apache.iotdb.common.rpc.thrift.TSetSpaceQuotaReq;
import org.apache.iotdb.common.rpc.thrift.TSpaceQuota;
import org.apache.iotdb.db.utils.EnvironmentUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataNodeSpaceQuotaManagerTest {

  private final Map<String, TSpaceQuota> spaceQuotaLimit = new HashMap<>();
  private final Map<String, TSpaceQuota> spaceQuotaUsage = new HashMap<>();
  DataNodeSpaceQuotaManager dataNodeSpaceQuotaManager =
      new DataNodeSpaceQuotaManager(spaceQuotaLimit, spaceQuotaUsage);
  private static final String ROOT_SG0 = "root.sg0";
  private static final String ROOT_SG1 = "root.sg1";
  private static final String ROOT_SG2 = "root.sg2";
  private static final String ROOT_SG3 = "root.sg3";
  private static final String SG0 = "sg0";
  private static final String SG1 = "sg1";
  private static final String SG2 = "sg2";
  private static final String SG3 = "sg3";

  @Before
  public void setUp() throws Exception {
    EnvironmentUtils.envSetUp();
  }

  @After
  public void tearDown() throws Exception {
    EnvironmentUtils.cleanEnv();
  }

  @Test
  public void testDataNodeSpaceQuotaManager() {
    TSpaceQuota spaceQuota = new TSpaceQuota();
    spaceQuota.setDeviceNum(5);
    spaceQuota.setTimeserieNum(10);
    spaceQuota.setDiskSize(1024);
    List<String> storageGroups1 = new ArrayList<>();
    List<String> storageGroups2 = new ArrayList<>();
    List<String> storageGroups3 = new ArrayList<>();
    storageGroups1.add(ROOT_SG0);
    storageGroups2.add(ROOT_SG1);
    storageGroups3.add(ROOT_SG2);
    TSetSpaceQuotaReq setSpaceQuotaReq = new TSetSpaceQuotaReq();
    setSpaceQuotaReq.setSpaceLimit(spaceQuota);
    setSpaceQuotaReq.setStorageGroup(storageGroups1);
    dataNodeSpaceQuotaManager.setSpaceQuota(setSpaceQuotaReq);
    spaceQuota.setDeviceNum(0);
    spaceQuota.setTimeserieNum(0);
    spaceQuota.setDiskSize(0);
    setSpaceQuotaReq.setSpaceLimit(spaceQuota);
    setSpaceQuotaReq.setStorageGroup(storageGroups2);
    dataNodeSpaceQuotaManager.setSpaceQuota(setSpaceQuotaReq);
    spaceQuota.setDeviceNum(-1);
    spaceQuota.setTimeserieNum(-1);
    spaceQuota.setDiskSize(-1);
    setSpaceQuotaReq.setSpaceLimit(spaceQuota);
    setSpaceQuotaReq.setStorageGroup(storageGroups3);
    dataNodeSpaceQuotaManager.setSpaceQuota(setSpaceQuotaReq);

    Assert.assertTrue(dataNodeSpaceQuotaManager.checkDeviceLimit(SG0));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkDeviceLimit(SG1));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkDeviceLimit(SG2));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkDeviceLimit(SG3));

    Assert.assertTrue(dataNodeSpaceQuotaManager.checkTimeSeriesNum(SG0));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkTimeSeriesNum(SG1));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkTimeSeriesNum(SG2));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkTimeSeriesNum(SG3));

    Assert.assertTrue(dataNodeSpaceQuotaManager.checkRegionDisk(ROOT_SG0));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkRegionDisk(ROOT_SG1));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkRegionDisk(ROOT_SG2));
    Assert.assertTrue(dataNodeSpaceQuotaManager.checkRegionDisk(ROOT_SG3));

    Map<String, TSpaceQuota> spaceQuotaUsage = new HashMap<>();
    spaceQuota.setDeviceNum(5);
    spaceQuota.setTimeserieNum(10);
    spaceQuota.setDiskSize(1024);
    spaceQuotaUsage.put(ROOT_SG0, spaceQuota);

    dataNodeSpaceQuotaManager.updateSpaceQuotaUsage(spaceQuotaUsage);

    Assert.assertFalse(dataNodeSpaceQuotaManager.checkDeviceLimit(SG0));
    Assert.assertFalse(dataNodeSpaceQuotaManager.checkTimeSeriesNum(SG0));
    Assert.assertFalse(dataNodeSpaceQuotaManager.checkRegionDisk(ROOT_SG0));
  }
}
