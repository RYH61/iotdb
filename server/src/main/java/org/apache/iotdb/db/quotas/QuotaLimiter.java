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

import org.apache.iotdb.common.rpc.thrift.TTimedQuota;
import org.apache.iotdb.common.rpc.thrift.ThrottleType;
import org.apache.iotdb.commons.conf.CommonConfig;
import org.apache.iotdb.commons.conf.CommonDescriptor;

import java.util.Map;

public class QuotaLimiter {

  private CommonConfig config = CommonDescriptor.getInstance().getConfig();
  private RateLimiter reqsLimiter = null;
  private RateLimiter reqSizeLimiter = null;
  private RateLimiter writeReqsLimiter = null;
  private RateLimiter writeSizeLimiter = null;
  private RateLimiter readReqsLimiter = null;
  private RateLimiter readSizeLimiter = null;

  private QuotaLimiter() {
    if (config.getRateLimiterType().equals(FixedIntervalRateLimiter.class.getName())) {
      reqsLimiter = new FixedIntervalRateLimiter();
      reqSizeLimiter = new FixedIntervalRateLimiter();
      writeReqsLimiter = new FixedIntervalRateLimiter();
      writeSizeLimiter = new FixedIntervalRateLimiter();
      readReqsLimiter = new FixedIntervalRateLimiter();
      readSizeLimiter = new FixedIntervalRateLimiter();
    } else {
      reqsLimiter = new AverageIntervalRateLimiter();
      reqSizeLimiter = new AverageIntervalRateLimiter();
      writeReqsLimiter = new AverageIntervalRateLimiter();
      writeSizeLimiter = new AverageIntervalRateLimiter();
      readReqsLimiter = new AverageIntervalRateLimiter();
      readSizeLimiter = new AverageIntervalRateLimiter();
    }
  }

  public static QuotaLimiter fromThrottle(Map<ThrottleType, TTimedQuota> throttleLimit) {
    QuotaLimiter limiter = new QuotaLimiter();
    TTimedQuota timedQuota;
    if (throttleLimit.containsKey(ThrottleType.REQUEST_NUMBER)) {
      timedQuota = throttleLimit.get(ThrottleType.REQUEST_NUMBER);
      limiter.reqsLimiter.set(timedQuota.getSoftLimit(), timedQuota.getTimeUnit());
    }

    if (throttleLimit.containsKey(ThrottleType.REQUEST_SIZE)) {
      timedQuota = throttleLimit.get(ThrottleType.REQUEST_SIZE);
      limiter.reqSizeLimiter.set(timedQuota.getSoftLimit(), timedQuota.getTimeUnit());
    }

    if (throttleLimit.containsKey(ThrottleType.WRITE_NUMBER)) {
      timedQuota = throttleLimit.get(ThrottleType.WRITE_NUMBER);
      limiter.writeReqsLimiter.set(timedQuota.getSoftLimit(), timedQuota.getTimeUnit());
    }

    if (throttleLimit.containsKey(ThrottleType.WRITE_SIZE)) {
      timedQuota = throttleLimit.get(ThrottleType.WRITE_SIZE);
      limiter.writeSizeLimiter.set(timedQuota.getSoftLimit(), timedQuota.getTimeUnit());
    }

    if (throttleLimit.containsKey(ThrottleType.READ_NUMBER)) {
      timedQuota = throttleLimit.get(ThrottleType.READ_NUMBER);
      limiter.readReqsLimiter.set(timedQuota.getSoftLimit(), timedQuota.getTimeUnit());
    }

    if (throttleLimit.containsKey(ThrottleType.READ_SIZE)) {
      timedQuota = throttleLimit.get(ThrottleType.READ_SIZE);
      limiter.readSizeLimiter.set(timedQuota.getSoftLimit(), timedQuota.getTimeUnit());
    }
    return limiter;
  }
}
