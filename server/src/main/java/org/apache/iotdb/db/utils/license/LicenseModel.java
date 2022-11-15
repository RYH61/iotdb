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

package org.apache.iotdb.db.utils.license;

import java.util.List;

public class LicenseModel {

  private String productApplyId;

  private String startDate;

  private String tenantId;

  private List<ProductInfo> productInfoList;

  public String getProductApplyId() {
    return productApplyId;
  }

  public void setProductApplyId(String productApplyId) {
    this.productApplyId = productApplyId;
  }

  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public List<ProductInfo> getProductInfoList() {
    return productInfoList;
  }

  public void setProductInfoList(List<ProductInfo> productInfoList) {
    this.productInfoList = productInfoList;
  }

  @Override
  public String toString() {
    return "LicenseModel{"
        + " productApplyId='"
        + productApplyId
        + '\''
        + ", startDate='"
        + startDate
        + '\''
        + ", tenantId='"
        + tenantId
        + '\''
        + ", productInfoList="
        + productInfoList
        + "}";
  }

  public class ProductInfo {

    private List<String> macList;
    private List<String> ipList;

    public List<String> getMacList() {
      return macList;
    }

    public void setMacList(List<String> macList) {
      this.macList = macList;
    }

    public List<String> getIpList() {
      return ipList;
    }

    public void setIpList(List<String> ipList) {
      this.ipList = ipList;
    }

    @Override
    public String toString() {
      return "ProductInfo{" + " macList=" + macList + ", ipList=" + ipList + "}";
    }
  }
}
