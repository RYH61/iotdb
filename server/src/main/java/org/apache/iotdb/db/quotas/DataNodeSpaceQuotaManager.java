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

import org.apache.iotdb.common.rpc.thrift.TSStatus;
import org.apache.iotdb.common.rpc.thrift.TSetSpaceQuotaReq;
import org.apache.iotdb.common.rpc.thrift.TSpaceQuota;
import org.apache.iotdb.commons.conf.IoTDBConstant;
import org.apache.iotdb.confignode.rpc.thrift.TSpaceQuotaResp;
import org.apache.iotdb.db.mpp.plan.execution.config.executor.ClusterConfigTaskExecutor;
import org.apache.iotdb.rpc.RpcUtils;
import org.apache.iotdb.rpc.TSStatusCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataNodeSpaceQuotaManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataNodeSpaceQuotaManager.class);

  private Map<String, TSpaceQuota> spaceQuotaLimit;
  private Map<String, TSpaceQuota> spaceQuotaUsage;
  private DataNodeSizeStore dataNodeSizeStore;

  public DataNodeSpaceQuotaManager() {
    spaceQuotaLimit = new HashMap<>();
    spaceQuotaUsage = new HashMap<>();
    dataNodeSizeStore = new DataNodeSizeStore();
    recover();
  }

  public DataNodeSpaceQuotaManager(
      Map<String, TSpaceQuota> spaceQuotaLimit, Map<String, TSpaceQuota> spaceQuotaUsage) {
    this.spaceQuotaLimit = spaceQuotaLimit;
    this.spaceQuotaUsage = spaceQuotaUsage;
  }

  /** SingleTone */
  private static class DataNodeSpaceQuotaManagerHolder {
    private static final DataNodeSpaceQuotaManager INSTANCE = new DataNodeSpaceQuotaManager();

    private DataNodeSpaceQuotaManagerHolder() {}
  }

  public static DataNodeSpaceQuotaManager getInstance() {
    return DataNodeSpaceQuotaManager.DataNodeSpaceQuotaManagerHolder.INSTANCE;
  }

  public TSStatus setSpaceQuota(TSetSpaceQuotaReq req) {
    for (String storageGroup : req.getStorageGroup()) {
      spaceQuotaLimit.put(storageGroup, req.getSpaceLimit());
      spaceQuotaUsage.put(storageGroup, new TSpaceQuota());
    }
    return RpcUtils.getStatus(TSStatusCode.SUCCESS_STATUS);
  }

  private void recover() {
    TSpaceQuotaResp spaceQuota = ClusterConfigTaskExecutor.getInstance().getSpaceQuota();
    if (spaceQuota.getStatus().getCode() == TSStatusCode.SUCCESS_STATUS.getStatusCode()
        && spaceQuota.getSpaceQuota() != null) {
      for (String storageGroup : spaceQuota.getSpaceQuota().keySet()) {
        spaceQuotaLimit.put(storageGroup, spaceQuota.getSpaceQuota().get(storageGroup));
        spaceQuotaUsage.put(storageGroup, new TSpaceQuota());
      }
    }
    LOGGER.info("Space quota limit restored successfully. " + spaceQuotaLimit.toString());
  }

  public boolean checkDeviceLimit(String storageGroup) {
    storageGroup = IoTDBConstant.PATH_ROOT + IoTDBConstant.PATH_SEPARATOR + storageGroup;
    TSpaceQuota spaceQuota = spaceQuotaLimit.get(storageGroup);
    if (spaceQuota == null) {
      return true;
    } else if (spaceQuota.getDeviceNum() == 0 || spaceQuota.getDeviceNum() == -1) {
      return true;
    }
    int deviceNum = spaceQuotaUsage.get(storageGroup).getDeviceNum();
    if (spaceQuota.getDeviceNum() - deviceNum > 0) {
      return true;
    }
    return false;
  }

  public void updateSpaceQuotaUsage(Map<String, TSpaceQuota> spaceQuotaUsage) {
    this.spaceQuotaUsage = spaceQuotaUsage;
  }

  public boolean checkTimeSeriesNum(String storageGroup) {
    storageGroup = IoTDBConstant.PATH_ROOT + IoTDBConstant.PATH_SEPARATOR + storageGroup;
    TSpaceQuota spaceQuota = spaceQuotaLimit.get(storageGroup);
    if (spaceQuota == null) {
      return true;
    } else if (spaceQuota.getTimeserieNum() == 0 || spaceQuota.getTimeserieNum() == -1) {
      return true;
    }
    int timeSeriesNum = spaceQuotaUsage.get(storageGroup).getTimeserieNum();
    if (spaceQuota.getTimeserieNum() - timeSeriesNum > 0) {
      return true;
    }
    return false;
  }

  public boolean checkRegionDisk(String storageGroup) {
    TSpaceQuota spaceQuota = spaceQuotaLimit.get(storageGroup);
    if (spaceQuota == null) {
      return true;
    } else if (spaceQuota.getDiskSize() == 0 || spaceQuota.getDiskSize() == -1) {
      return true;
    }
    long diskSize = spaceQuotaUsage.get(storageGroup).getDiskSize();
    if (spaceQuota.getDiskSize() - diskSize > 0) {
      return true;
    }
    return false;
  }

  public void setDataRegionIds(List<Integer> dataRegionIds) {
    dataNodeSizeStore.setDataRegionIds(dataRegionIds);
  }

  public Map<Integer, Long> getRegionDisk() {
    return dataNodeSizeStore.getDataRegionDisk();
  }

  public void setSpaceQuotaLimit(Map<String, TSpaceQuota> spaceQuotaLimit) {
    this.spaceQuotaLimit = spaceQuotaLimit;
  }
}
