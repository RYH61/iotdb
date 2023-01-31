<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->



# Multi-Tenancy

IoTDB provides multi tenant operations, mainly to limit the resources of the database or the database when users are using it.

## Space Quota

### Basic Concepts

 Space quota refers to the restriction on the use space of a database, which mainly includes the following types: 

| Type           | Explain                                           | Unit                               |
| -------------- | ------------------------------------------------- | ---------------------------------- |
| Device num     | Limit on the number of devices in a database      | number                             |
| TimeSeries num | Limit on the number of time series in a database  | number                             |
| disk           | Restrictions on the use of space under a database | M（MB）、G（GB）、T（TB）、P（PB） |

### Open Quota

 If quota needs to be used, you need to open the following configuration  items in iotdb-commons.properties under the conf folder in the root  directory: 

```
quota_enable=true
```

### Set Space Quota

We can limit the available space of the database by setting a space quota for the database.

Example: For database root.sg1, limit the number of devices to 5, the number of timeseries to 10, and the available space to 100g. 

```SQL
set space quota devices=5,timeseries=10,disk=100g on root.sg1;
```

 You can set the same quota for multiple databases at the same time. 

```SQL
set space quota devices=5,timeseries=10,disk=100g on root.sg1, root.sg2;
```

If we want to cancel a certain quota, we can set the quota as unlimited, for example, to cancel the quota of time series quantity of database root.sg1:

```SQL
set space quota timeseries=unlimited on root.sg1;
```

### Show Quota Information

-  Show the space quota information of all databases 

```SQL
IoTDB> set space quota devices=5,timeseries=10,disk=100g on root.sg1, root.sg2;
Msg: The statement is executed successfully.
IoTDB> show space quota;
+--------+-------------+-------+----+
|database|    quotaType|  limit|used|
+--------+-------------+-------+----+
|root.sg1|     diskSize|102400M|  0M|
|root.sg1|    deviceNum|      5|   0|
|root.sg1|timeSeriesNum|     10|   0|
|root.sg2|     diskSize|102400M|  0M|
|root.sg2|    deviceNum|      5|   0|
|root.sg2|timeSeriesNum|     10|   0|
+--------+-------------+-------+----+
Total line number = 6
It costs 0.067s
```

- Show the space quota information of the specified database

```SQL
IoTDB> show space quota root.sg1;
+--------+-------------+-------+----+
|database|    quotaType|  limit|used|
+--------+-------------+-------+----+
|root.sg1|     diskSize|102400M|  0M|
|root.sg1|    deviceNum|      5|   0|
|root.sg1|timeSeriesNum|     10|   0|
+--------+-------------+-------+----+
Total line number = 3
It costs 0.007s
```



## Throttle Quota

### Basic Concepts

The number of times or amount of data that a user can access resources within a limited unit.

This restriction currently includes the following types:

| Type    | Explain                                                     | Unit      | read/write limit |
| ------- | ----------------------------------------------------------- | --------- | ---------------- |
| type    | Set the read/write limit. When not set, it defaults to all. |           |                  |
| request | Number of requests per unit time                            | req/time  | read/write       |
| size    | Request size per unit time                                  | size/time | read/write       |

The time range unit and request size unit are as follows:

1. Time range unit（time）：sec、min、hour、day
2. Request size unit（size）：B（字节）、K（千字节）、M（兆字节）、G（千兆字节）、T（TB）、P（PB）



### Set Throttle Quota

We can set throttle quota for users to limit their use of resources;

Example 1: Set user user1 to request only 1G per minute:

```
set throttle quota size='1G/min' on user1;
```

Example 2: Set the maximum query frequency of user user1 to 10 times per minute:

```
set throttle quota request='10/min', type='read' on user1;
```

The way to cancel a certain throttle quota is the same as the way to cancel space quota above. Set quota to unlimited.



### Show Quota Information

We support viewing quota information. The main syntax is as follows:

- View the throttle quota information of all users

```SQL
show throttle quota
```

- View the throttle quota information of the specified user

```SQL
show throttle quota user1;
```

Explain the following results:


Set the number of reads for user usera to 10 per minute.


Set the read and write request size for user usera to 100MB in 1 minute.


Set the write operation for user userb to 5 times per hour.


If you do not specify the read/write type when setting the throttle quota for the user, the default is all.

| **user** | **Throttle type** | **Throttle quota** | **read/write** |
| -------- | ----------------- | ------------------ | -------------- |
| usera    | request           | 10req/min          | read           |
| usera    | size              | 100M/min           |                |
| userb    | request           | 5req/hour          | write          |