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

public abstract class RateLimiter {

  // Timeunit factor for translating to ms.
  private long tunit = 1000;
  // The max value available resource units can be refilled to.
  private long limit = Long.MAX_VALUE;
  // Currently available resource units
  private long avail = Long.MAX_VALUE;

  /**
   * Set the RateLimiter max available resources and refill period.
   *
   * @param limit The max value available resource units can be refilled to.
   */
  public synchronized void set(final long limit, final long tunit) {
    this.tunit = tunit;
    this.limit = limit;
    this.avail = limit;
  }
}
