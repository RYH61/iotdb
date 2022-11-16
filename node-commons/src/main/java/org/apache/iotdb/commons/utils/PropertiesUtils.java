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
package org.apache.iotdb.commons.utils;

import org.apache.iotdb.commons.conf.IoTDBConstant;
import org.apache.iotdb.commons.exception.PropertiesEmptyException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PropertiesUtils {

  private static final Logger logger = LoggerFactory.getLogger(PropertiesUtils.class);

  public static final String FLAG = "flag";
  public static final String NAME = "name";
  public static final String MEANING = "meaning";
  public static final String GROUP = "group";
  public static final String FILL = "fill";
  public static final String SHOW_CONSTRAINT = "showConstraint";
  public static final String CONSTRAINT = "constraint";
  public static final String FORMAT = "format";
  public static final String OPTIONS = "options";
  public static final String DEFAULTS = "default";
  public static final String DEFAULT_SHOW_VALUE = "defaultShowValue";
  public static final String SHOW_VALUE = "showValue";
  public static final String CURRENT = "current";
  public static final String VALUE_UNITS = "valueUnits";
  public static final String CURRENT_UNIT = "currentUnit";
  public static final String UNIT_TYPE = "unitType";
  public static final String MIN_UNIT = "minUnit";
  public static final String BYTE = "byte";
  public static final String TIME = "time";
  public static final String USER_DEFINED = "userDefined";
  public static final String POSITION = "position";
  public static final String DICTIONARY = "dictionary";
  public static final String DICTIONARIES = "dictionaries";
  public static final String VALUE = "value";
  public static final String MAX_HEAP_SIZE = "MAX_HEAP_SIZE";
  public static final String HEAP_NEW_SIZE = "HEAP_NEWSIZE";
  public static final String DATANODE_FILE_NAME = "iotdb-datanode.properties";
  public static final String CONFIG_FILE_NAME = "iotdb-confignode.properties";
  public static final String COMMON_FILE_NAME = "iotdb-common.properties";
  public static final String DATANODE_ENV_SH = "datanode-env.sh";
  public static final String DATANODE_ENV_BAT = "datanode-env.bat";
  public static final String CONFIGNODE_ENV_SH = "confignode-env.sh";
  public static final String CONFIGNODE_ENV_BAT = "confignode-env.bat";
  public static Iterator<Element> childrenNode = null;
  public static final String CONFIGURATION_CEA_NAME = "configuration-cea.xml";
  public static final String CONFIGNODE_CONF = "CONFIGNODE_CONF";
  public static final String CONFIGNODE_HOME = "CONFIGNODE_HOME";

  /**
   * Check if node is null
   *
   * @return
   */
  public static Iterator<Element> getChildrenNodes() {
    if (childrenNode == null) {
      return getChildrenNode();
    } else if (!childrenNode.hasNext()) {
      return getChildrenNode();
    }
    return childrenNode;
  }

  /**
   * Get all the child nodes of the root node
   *
   * @return
   */
  public static Iterator<Element> getChildrenNode() {
    File file = new File(getDataNodePropsUrl(CONFIGURATION_CEA_NAME));
    if (!file.exists()) {
      file = new File(getConfigNodePropsUrl(CONFIGURATION_CEA_NAME));
    }
    SAXReader reader = new SAXReader();
    Document doc = null;
    try {
      // Read file as document
      doc = reader.read(file);
    } catch (DocumentException e) {
      logger.error("Failed to read file", e);
    } // Get the root element of the document
    Element root = doc.getRootElement();
    // Find all child nodes according to the element and return
    childrenNode = root.elementIterator(FLAG);
    return childrenNode;
  }

  /**
   * update the contents of the file.Read the parameters in the xml file, and then change the
   * parameters in the properties file
   *
   * @return
   */
  public static SafePropertiesUtils updateProperties(String fileName, String fName)
      throws PropertiesEmptyException {
    Iterator<Element> childrenNodes = getChildrenNodes();
    SafePropertiesUtils properties = PropertiesUtils.getProperties(fileName);
    while (childrenNodes.hasNext()) {
      Element flag = childrenNodes.next();
      Element name = flag.element(PropertiesUtils.NAME);
      Element current = flag.element(PropertiesUtils.CURRENT);
      Element defaults = flag.element(PropertiesUtils.DEFAULTS);
      // Get the child node userDefined of the flag
      Iterator<Element> userDefinedIterator = flag.elementIterator(PropertiesUtils.USER_DEFINED);
      Element next = null;
      Element position = null;
      while (userDefinedIterator.hasNext()) {
        next = userDefinedIterator.next();
        position = next.element(PropertiesUtils.POSITION);
      }
      if (current.getText().equals("")
          || current.getText() == null
          || current.getText().isEmpty()) {
        throw new PropertiesEmptyException(
            "The current value of cea is empty, please add a value for cea,parameter name: "
                + name.getText());
      }
      // If the parameters of the current configuration item in the properties file are not equal to
      // the default parameters in the xml file, but the default parameters in the xml file are
      // equal to the current parameters, write back the data
      if (defaults.getText().equals(current.getText())) {
        if (properties.containsKey(name.getText())) {
          if (!properties.getProperty(name.getText()).equals(current.getText())) {
            properties = setProperties(properties, fName, position, current, name, defaults);
          }
        }
      } else {
        if (properties.containsKey(name.getText())) {
          if (!properties.getProperty(name.getText()).equals(current.getText())) {
            properties = setProperties(properties, fName, position, current, name, defaults);
          }
        } else {
          properties = setProperties(properties, fName, position, current, name, defaults);
        }
      }
    }
    logger.info(String.valueOf(properties));
    return properties;
  }

  /**
   * get props url location
   *
   * @return url object if location exit, otherwise null.
   */
  public static String getDataNodePropsUrl(String fileName) {
    // Check if a config-directory was specified first.
    String urlString = System.getProperty(IoTDBConstant.IOTDB_CONF, null);
    // If it wasn't, check if a home directory was provided (This usually contains a config)
    if (urlString == null) {
      urlString = System.getProperty(IoTDBConstant.IOTDB_HOME, null);
      if (urlString != null) {
        urlString = urlString + File.separatorChar + "conf" + File.separatorChar + fileName;
      }
    }
    // If a config location was provided, but it doesn't end with a properties file,
    // append the default location.
    else if (urlString != null) {
      urlString += (File.separatorChar + fileName);
    }
    if (urlString == null) {
      urlString = "." + File.separatorChar + fileName;
    }
    return urlString;
  }

  /**
   * get props url location
   *
   * @return url object if location exit, otherwise null.
   */
  public static String getConfigNodePropsUrl(String fileName) {
    // Check if a config-directory was specified first.
    String urlString = System.getProperty(CONFIGNODE_CONF, null);
    // If it wasn't, check if a home directory was provided (This usually contains a config)
    if (urlString == null) {
      urlString = System.getProperty(CONFIGNODE_HOME, null);
      if (urlString != null) {
        urlString = urlString + File.separatorChar + "conf" + File.separatorChar + fileName;
      }
    }
    // If a config location was provided, but it doesn't end with a properties file,
    // append the default location.
    else if (urlString != null) {
      urlString += (File.separatorChar + fileName);
    }
    if (urlString == null) {
      urlString = "." + File.separatorChar + fileName;
    }
    return urlString;
  }

  /**
   * According to the file ownership, write back the corresponding parameters
   *
   * @param properties
   * @param fName
   * @param position
   * @param current
   * @param name
   */
  public static SafePropertiesUtils setProperties(
      SafePropertiesUtils properties,
      String fName,
      Element position,
      Element current,
      Element name,
      Element defaults) {
    if (fName.equals(position.getText())) {
      String iotdb_before = properties.getProperty(name.getText());
      properties.setProperty(name.getText(), current.getText());
      logger.info(
          "name="
              + name.getText()
              + ",cea-current-value="
              + current.getText()
              + ",cea-default-value="
              + defaults.getText()
              + ",times-before-value="
              + iotdb_before
              + ",times-after-value="
              + properties.getProperty(name.getText()));
    }
    return properties;
  }

  /**
   * Get properties object
   *
   * @param fileName The path name where the file is located
   * @return
   */
  public static SafePropertiesUtils getProperties(String fileName) {
    SafePropertiesUtils properties = new SafePropertiesUtils();
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(fileName))) {
      properties.load(inputStream);
    } catch (FileNotFoundException e) {
      logger.error(fileName + "file not found", e);
    } catch (IOException e) {
      logger.error("IOException occurs", e);
    }
    return properties;
  }

  /**
   * Get the value based on the key value
   *
   * @param key
   * @param fileName
   * @return
   */
  public static String getValue(String key, String fileName) {
    SafePropertiesUtils properties = getProperties(fileName);
    String value = properties.getProperty(key);
    return value;
  }

  /**
   * set or increment value
   *
   * @param properties
   * @param fileName The path name where the file is located
   */
  public static void setValue(SafePropertiesUtils properties, String fileName) {
    logger.info("Start checking parameters and update：" + fileName);
    try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
      properties.store(fileOutputStream, null);
      logger.info("update success：" + fileName);
    } catch (FileNotFoundException e) {
      logger.error(fileName + "file not found", e);
    } catch (IOException e) {
      logger.error("IOException occurs", e);
    }
  }

  /**
   * Update the parameter information read from the xml file to iotdb-env.sh、bat
   *
   * @param fileName The path name where the file is located
   */
  public static void writeInFile(String fileName) throws PropertiesEmptyException {
    logger.info("Start checking parameters and update：" + fileName);
    Iterator<Element> childrenNodes = getChildrenNodes();
    // When the current parameter value in the configuration is equal to the default parameter
    // value. Both parameters are null
    String maxHeadSizeLine = null;
    String headNewSizeLine = null;
    while (childrenNodes.hasNext()) {
      Element flag = childrenNodes.next();
      Element name = flag.element(NAME);
      Element current = flag.element(CURRENT);
      Element defaults = flag.element(PropertiesUtils.DEFAULTS);
      if (current.getText().equals("")
          || current.getText() == null
          || current.getText().isEmpty()) {
        throw new PropertiesEmptyException(
            "The current value of cea is empty, please add a value for cea,parameter name: "
                + name.getText());
      }
      if (defaults.getText().equals(current.getText())) {
        continue;
      }
      while (MAX_HEAP_SIZE.equals(name.getText())) {
        maxHeadSizeLine = MAX_HEAP_SIZE + "=" + current.getText();
        break;
      }
      while (HEAP_NEW_SIZE.equals(name.getText())) {
        headNewSizeLine = HEAP_NEW_SIZE + "=" + current.getText();
        break;
      }
    }
    Path path = Paths.get(fileName);
    List<String> lines = null;
    try {
      lines = Files.readAllLines(path);
      // Locate where these two parameters are located
      int maxHeadLineNumber = 1;
      int headNewSizeLineNumber = 1;
      for (String line : lines) {
        if (line.contains("Maximum heap size")) {
          break;
        }
        maxHeadLineNumber++;
      }
      for (String line : lines) {
        if (line.contains("Minimum heap size")) {
          break;
        }
        headNewSizeLineNumber++;
      }
      // Whether the MAX_HEAP_SIZE parameter has been written
      boolean maxHeadSizeFlag =
          lines.get(maxHeadLineNumber + 1).contains(PropertiesUtils.MAX_HEAP_SIZE);
      // Whether the HEAP_NEWSIZE parameter has been written
      boolean headNewSizeFlag =
          lines.get(headNewSizeLineNumber + 1).contains(PropertiesUtils.HEAP_NEW_SIZE);
      if (maxHeadSizeLine == null && headNewSizeLine == null) {
        // If both have been written, remove these two configuration items
        if (maxHeadSizeFlag && headNewSizeFlag) {
          lines.remove(maxHeadLineNumber + 1);
          lines.remove(headNewSizeLineNumber);
        } else if (maxHeadSizeFlag || headNewSizeFlag) {
          // If one of the parameters exists, remove it
          if (maxHeadSizeFlag) {
            lines.remove(maxHeadLineNumber + 1);
          } else {
            lines.remove(headNewSizeLineNumber + 1);
          }
        } else {
          return;
        }
      }
      if (maxHeadSizeLine != null) {
        // If the MAX_HEAP_SIZE configuration item has parameters, remove the configuration item in
        // iotdb-env.iotdb and update it to a new configuration item.Otherwise add directly
        if (maxHeadSizeFlag) {
          lines.remove(maxHeadLineNumber + 1);
          lines.add(maxHeadLineNumber + 1, maxHeadSizeLine);
        } else {
          lines.add(maxHeadLineNumber + 1, maxHeadSizeLine);
        }
      }
      // If neither parameter is null. Then judge whether it has been configured in the
      // configuration file, if so, remove it and add a new configuration. If not, add it directly
      if (headNewSizeLine != null && maxHeadSizeLine != null) {
        if (headNewSizeFlag) {
          lines.remove(headNewSizeLineNumber + 1);
          lines.add(headNewSizeLineNumber + 1, headNewSizeLine);
        } else {
          lines.add(headNewSizeLineNumber + 2, headNewSizeLine);
        }
      }
      if (headNewSizeLine != null && maxHeadSizeLine == null) {
        // If the HEAP_NEWSIZE configuration item already exists, remove it and then add it
        if (headNewSizeFlag) {
          lines.remove(headNewSizeLineNumber + 1);
          lines.add(headNewSizeLineNumber + 1, headNewSizeLine);
          if (maxHeadSizeFlag) {
            lines.remove(maxHeadLineNumber + 1);
          }
        } else {
          lines.add(headNewSizeLineNumber + 1, headNewSizeLine);
          if (maxHeadSizeFlag) {
            lines.remove(maxHeadLineNumber + 1);
          }
        }
      }
      Files.write(path, lines);
      logger.info("update success：" + fileName);
    } catch (IOException e) {
      logger.error("file write failed", e);
    }
  }

  /**
   * Generate child nodes and their content in xml file
   *
   * @param nameText
   * @param meaningText
   * @param groupText
   * @param fillText
   * @param showConstraintText
   * @param constraintText
   * @param formatText
   * @param aDefaultText
   * @param defaultShowText
   * @param showValueText
   * @param currentText
   * @param valueUnitsText
   * @param positionText
   * @param allFlags
   */
  public static void createXml(
      String nameText,
      String meaningText,
      String groupText,
      String fillText,
      String showConstraintText,
      String constraintText,
      String formatText,
      String aDefaultText,
      String defaultShowText,
      String showValueText,
      String currentText,
      String valueUnitsText,
      String positionText,
      Element allFlags) {
    try {
      Element flag = allFlags.addElement(FLAG);
      if ("dn_rpc_port".equals(nameText)
          || "dn_internal_port".equals(nameText)
          || "dn_mpp_data_exchange_port".equals(nameText)
          || "dn_schema_region_consensus_port".equals(nameText)
          || "dn_data_region_consensus_port".equals(nameText)
          || "cn_internal_port".equals(nameText)
          || "cn_consensus_port".equals(nameText)
          || "mqtt_port".equals(nameText)
          || "influxdb_rpc_port".equals(nameText)) {
        flag.addAttribute("type", "port");
      }
      Element name = flag.addElement(NAME);
      name.setText(nameText);
      Element meaning = flag.addElement(MEANING);
      meaning.setText(meaningText);
      Element group = flag.addElement(GROUP);
      group.setText(groupText);
      Element fill = flag.addElement(FILL);
      fill.setText(fillText);
      Element showConstraint = flag.addElement(SHOW_CONSTRAINT);
      showConstraint.setText(showConstraintText);
      Element contraint = flag.addElement(CONSTRAINT);
      contraint.setText(constraintText);
      Element format1 = flag.addElement(FORMAT);
      format1.setText(formatText);
      if (formatText.equals("select")) {

        Element options = flag.addElement(OPTIONS);
        options.setText("true,false");
      }
      Element aDefault = flag.addElement(DEFAULTS);
      aDefault.setText(aDefaultText);
      Element defaultShowValue = flag.addElement(DEFAULT_SHOW_VALUE);
      defaultShowValue.setText(defaultShowText);
      Element showValue = flag.addElement(SHOW_VALUE);
      showValue.setText(showValueText);
      Element current = flag.addElement(CURRENT);
      current.setText(currentText);
      Element valueUnits = flag.addElement(VALUE_UNITS);
      if ("cn_thrift_max_frame_size".equals(nameText)
          || "cn_thrift_init_buffer_size".equals(nameText)
          || "mlog_buffer_size".equals(nameText)
          || "max_tsblock_size_in_bytes".equals(nameText)
          || "memtable_size_threshold".equals(nameText)
          || "target_compaction_file_size".equals(nameText)
          || "target_chunk_size".equals(nameText)
          || "chunk_size_lower_bound_in_compaction".equals(nameText)
          || "max_cross_compaction_candidate_file_size".equals(nameText)
          || "wal_buffer_size_in_byte".equals(nameText)
          || "wal_file_size_threshold_in_byte".equals(nameText)
          || "wal_memtable_snapshot_threshold_in_byte".equals(nameText)
          || "multi_leader_throttle_threshold_in_byte".equals(nameText)
          || "group_size_in_byte".equals(nameText)
          || "page_size_in_byte".equals(nameText)
          || "cqlog_buffer_size".equals(nameText)
          || "thrift_max_frame_size".equals(nameText)
          || "thrift_init_buffer_size".equals(nameText)
          || "config_node_ratis_log_appender_buffer_size_max".equals(nameText)
          || "schema_region_ratis_log_appender_buffer_size_max".equals(nameText)
          || "data_region_ratis_log_appender_buffer_size_max".equals(nameText)
          || "config_node_ratis_log_segment_size_max_in_byte".equals(nameText)
          || "schema_region_ratis_log_segment_size_max_in_byte".equals(nameText)
          || "data_region_ratis_log_segment_size_max_in_byte".equals(nameText)
          || "config_node_simple_consensus_log_segment_size_max_in_byte".equals(nameText)
          || "config_node_ratis_grpc_flow_control_window".equals(nameText)
          || "schema_region_ratis_grpc_flow_control_window".equals(nameText)
          || "# data_region_ratis_grpc_flow_control_window=4194304".equals(nameText)
          || "mqtt_max_message_size".equals(nameText)) {
        Element options = valueUnits.addElement(OPTIONS);
        options.setText("B");
        Element currentUnit = valueUnits.addElement(CURRENT_UNIT);
        currentUnit.setText("B");
        Element unitType = valueUnits.addElement(UNIT_TYPE);
        unitType.setText(BYTE);
        Element minUnit = valueUnits.addElement(MIN_UNIT);
        minUnit.setText("B");
      } else if ("cn_connection_timeout_ms".equals(nameText)
          || "time_partition_interval_for_routing".equals(nameText)
          || "heartbeat_interval_in_ms".equals(nameText)
          || "check_period_when_insert_blocked".equals(nameText)
          || "tag_attribute_flush_interval".equals(nameText)
          || "sync_mlog_period_in_ms".equals(nameText)
          || "mpp_data_exchange_keep_alive_time_in_ms".equals(nameText)
          || "default_fill_interval".equals(nameText)
          || "driver_task_execution_time_slice_in_ms".equals(nameText)
          || "default_ttl_in_ms".equals(nameText)
          || "max_waiting_time_when_insert_blocked".equals(nameText)
          || "time_partition_interval_for_storage".equals(nameText)
          || "slow_query_threshold".equals(nameText)
          || "seq_memtable_flush_interval_in_ms".equals(nameText)
          || "seq_memtable_flush_check_interval_in_ms".equals(nameText)
          || "unseq_memtable_flush_interval_in_ms".equals(nameText)
          || "unseq_memtable_flush_check_interval_in_ms".equals(nameText)
          || "recovery_log_interval_in_ms".equals(nameText)
          || "cross_compaction_file_selection_time_budget".equals(nameText)
          || "compaction_submission_interval_in_ms".equals(nameText)
          || "compaction_schedule_interval_in_ms".equals(nameText)
          || "fsync_wal_delay_in_ms".equals(nameText)
          || "delete_wal_files_period_in_ms".equals(nameText)
          || "multi_leader_cache_window_time_in_ms".equals(nameText)
          || "continuous_query_min_every_interval_in_ms".equals(nameText)
          || "session_timeout_threshold".equals(nameText)
          || "connection_timeout_ms".equals(nameText)
          || "config_node_ratis_rpc_leader_election_timeout_min_ms".equals(nameText)
          || "schema_region_ratis_rpc_leader_election_timeout_min_ms".equals(nameText)
          || "data_region_ratis_rpc_leader_election_timeout_min_ms".equals(nameText)
          || "config_node_ratis_rpc_leader_election_timeout_max_ms".equals(nameText)
          || "schema_region_ratis_rpc_leader_election_timeout_max_ms".equals(nameText)
          || "data_region_ratis_rpc_leader_election_timeout_max_ms".equals(nameText)
          || "config_node_ratis_request_timeout_ms".equals(nameText)
          || "schema_region_ratis_request_timeout_ms".equals(nameText)
          || "data_region_ratis_request_timeout_ms".equals(nameText)
          || "config_node_ratis_initial_sleep_time_ms".equals(nameText)
          || "config_node_ratis_max_sleep_time_ms".equals(nameText)
          || "schema_region_ratis_initial_sleep_time_ms".equals(nameText)
          || "schema_region_ratis_max_sleep_time_ms".equals(nameText)
          || "data_region_ratis_initial_sleep_time_ms".equals(nameText)
          || "data_region_ratis_max_sleep_time_ms".equals(nameText)
          || "ratis_first_election_timeout_min_ms".equals(nameText)
          || "ratis_first_election_timeout_max_ms".equals(nameText)) {
        Element options = valueUnits.addElement(OPTIONS);
        options.setText("ms");
        Element currentUnit = valueUnits.addElement(CURRENT_UNIT);
        currentUnit.setText("ms");
        Element unitType = valueUnits.addElement(UNIT_TYPE);
        unitType.setText(TIME);
        Element minUnit = valueUnits.addElement(MIN_UNIT);
        minUnit.setText("ms");
      } else if ("procedure_completed_clean_interval".equals(nameText)
          || "procedure_completed_evict_ttl".equals(nameText)
          || "compaction_write_throughput_mb_per_sec".equals(nameText)
          || "query_timeout_threshold".equals(nameText)) {
        Element options = valueUnits.addElement(OPTIONS);
        options.setText("s");
        Element currentUnit = valueUnits.addElement(CURRENT_UNIT);
        currentUnit.setText("s");
        Element unitType = valueUnits.addElement(UNIT_TYPE);
        unitType.setText(TIME);
        Element minUnit = valueUnits.addElement(MIN_UNIT);
        minUnit.setText("s");
      } else if ("author_cache_expire_time".equals(nameText)) {
        Element options = valueUnits.addElement(OPTIONS);
        options.setText("min");
        Element currentUnit = valueUnits.addElement(CURRENT_UNIT);
        currentUnit.setText("min");
        Element unitType = valueUnits.addElement(UNIT_TYPE);
        unitType.setText(TIME);
        Element minUnit = valueUnits.addElement(MIN_UNIT);
        minUnit.setText("min");
      } else {
        valueUnits.setText(valueUnitsText);
      }
      Element userDefined = flag.addElement(USER_DEFINED);
      Element position = userDefined.addElement(POSITION);
      position.setText(positionText);
      logger.info("Generate successfully");
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Build failed", e);
    }
  }

  private static void createDictionaries(List<String> dictionaries, Element allFlags) {
    Element dictionariesFlag = allFlags.addElement(DICTIONARIES);
    for (String dictionaryFlag : dictionaries) {
      Element dictionary = dictionariesFlag.addElement(DICTIONARY);
      Element name = dictionary.addElement(NAME);
      name.setText(dictionaryFlag);
      Element value = dictionary.addElement(VALUE);
      value.setText(dictionaryFlag);
    }
  }

  /**
   * generate xml file
   *
   * @param fileNames
   * @param xmlFilePath
   */
  public static void getXml(List<String> fileNames, String xmlFilePath) {
    Document document = DocumentHelper.createDocument();
    Element allFlags = document.addElement("AllFlags");
    List<String> dictionaries = new ArrayList<>();
    String group = null;
    for (String fileName : fileNames) {
      Path path = Paths.get(fileName);
      List<String[]> list = new ArrayList<>();
      String[] text = null;
      try {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int currentNum = 0;
        // Stores the row number of the previous parameter
        int beforeNum = 21;
        for (String line : lines) {
          text = new String[13];
          if (line.contains("### ")) {
            group = line.substring(4);
            dictionaries.add(group);
          }
          if (line.contains("=")) {
            String[] split = line.split("=");
            if (split.length == 1
                || split[0].contains("If you have extremely high write load")
                || split[0].contains("If sync_mlog_period_in_ms")
                || split[0].contains("How many threads can concurrently flush. When")
                || split[0].contains(
                    "How many threads can concurrently execute query statement. When")
                || split[0].contains(
                    "How many threads can concurrently read data for raw data query. When")
                || split[0].contains("Blocking queue size for read task in raw data query. Must")
                || split[0].contains(
                    "How many threads can be used for evaluating sliding windows. When")
                || split[0].contains(
                    "Max number of window evaluation tasks that can be pending for execution. When")
                || split[0].contains(
                    "Maximum number of continuous query tasks that can be pending for execution. When")
                || line.contains("cn_system_dir=data\\\\confignode\\\\system")
                || line.contains("cn_consensus_dir=data\\\\confignode\\\\consensus")
                || line.contains("dn_system_dir=data\\\\datanode\\\\system")
                || line.contains("dn_data_dirs=data\\\\datanode\\\\data")
                || line.contains("dn_consensus_dir=data\\\\datanode\\\\consensus")
                || line.contains("dn_wal_dirs=data\\\\datanode\\\\wal")
                || line.contains("dn_tracing_dir=datanode\\\\tracing")
                || line.contains("trigger_lib_dir=ext\\\\trigger")
                || line.contains("udf_lib_dir=ext\\\\udf")
                || split[0].contains("<")) {
              currentNum++;
              continue;
            }
            if (split[0].contains("#")) {
              String[] s = split[0].split("#");
              split[0] = s[1];
              text[2] = group;
              text[3] = "0";
            } else {
              text[2] = group;
              text[3] = "1";
            }
            text[4] = "";
            text[5] = "";
            text[0] = split[0];
            for (int i = beforeNum + 2; i <= currentNum - 1; i++) {
              text[1] += lines.get(i);
            }
            if ("dn_rpc_address".equals(split[0])
                || "dn_internal_address".equals(split[0])
                || "cn_internal_address".equals(split[0])
                || "mqtt_host".equals(split[0])) {
              text[6] = "ip";
              String s = lines.get(currentNum - 1) + lines.get(currentNum - 2);
              String replace = s.replace("#", "");
              text[1] = replace;
            } else if ("dn_rpc_port".equals(split[0])
                || "dn_internal_port".equals(split[0])
                || "dn_mpp_data_exchange_port".equals(split[0])
                || "dn_schema_region_consensus_port".equals(split[0])
                || "dn_data_region_consensus_port".equals(split[0])
                || "cn_internal_port".equals(split[0])
                || "cn_consensus_port".equals(split[0])
                || "mqtt_port".equals(split[0])
                || "influxdb_rpc_port".equals(split[0])) {
              text[6] = "port";
            } else if ("true".equals(split[1]) || "false".equals(split[1])) {
              text[6] = "select";
            } else if ("dn_target_config_node_list".equals(split[0])
                || "cn_target_config_node_list".equals(split[0])) {
              text[6] = "ipPortList";
            } else if (split[1].matches("^[/\\\\A-Za-z]+$")) {
              text[6] = "string";
            } else if (split[1].contains(".")) {
              text[6] = "double";
            } else if ("config_node_consensus_protocol_class".equals(split[0])
                || "schema_region_consensus_protocol_class".equals(split[0])
                || "data_region_consensus_protocol_class".equals(split[0])
                || "series_partition_executor_class".equals(split[0])
                || "region_allocate_strategy".equals(split[0])
                || "routing_policy".equals(split[0])
                || "read_consistency_level".equals(split[0])
                || "handle_system_error".equals(split[0])
                || "tvlist_sort_algorithm".equals(split[0])
                || "cross_selector".equals(split[0])
                || "cross_performer".equals(split[0])
                || "inner_seq_selector".equals(split[0])
                || "inner_seq_performer".equals(split[0])
                || "inner_unseq_selector".equals(split[0])
                || "inner_unseq_performer".equals(split[0])
                || "compaction_priority".equals(split[0])
                || "wal_mode".equals(split[0])) {
              text[6] = "select";
            } else if ("dn_join_cluster_retry_interval_ms".equals(split[0])
                || "time_partition_interval_for_routing".equals(split[0])
                || "heartbeat_interval_in_ms".equals(split[0])
                || "concurrent_writing_time_partition".equals(split[0])
                || "sync_mlog_period_in_ms".equals(split[0])
                || "slow_query_threshold".equals(split[0])
                || "default_ttl_in_ms".equals(split[0])
                || "time_partition_interval_for_storage".equals(split[0])
                || "memtable_size_threshold".equals(split[0])
                || "seq_memtable_flush_interval_in_ms".equals(split[0])
                || "seq_memtable_flush_check_interval_in_ms".equals(split[0])
                || "unseq_memtable_flush_interval_in_ms".equals(split[0])
                || "unseq_memtable_flush_check_interval_in_ms".equals(split[0])
                || "target_compaction_file_size".equals(split[0])
                || "target_chunk_size".equals(split[0])
                || "chunk_size_lower_bound_in_compaction".equals(split[0])
                || "chunk_point_num_lower_bound_in_compaction".equals(split[0])
                || "max_cross_compaction_candidate_file_size".equals(split[0])
                || "cross_compaction_file_selection_time_budget".equals(split[0])
                || "compaction_schedule_interval_in_ms".equals(split[0])
                || "compaction_submission_interval_in_ms".equals(split[0])
                || "fsync_wal_delay_in_ms".equals(split[0])
                || "wal_file_size_threshold_in_byte".equals(split[0])
                || "wal_memtable_snapshot_threshold_in_byte".equals(split[0])
                || "delete_wal_files_period_in_ms".equals(split[0])
                || "multi_leader_throttle_threshold_in_byte".equals(split[0])
                || "multi_leader_cache_window_time_in_ms".equals(split[0])
                || "continuous_query_min_every_interval_in_ms".equals(split[0])
                || "config_node_ratis_log_appender_buffer_size_max".equals(split[0])
                || "schema_region_ratis_log_appender_buffer_size_max".equals(split[0])
                || "data_region_ratis_log_appender_buffer_size_max".equals(split[0])
                || "config_node_ratis_snapshot_trigger_threshold".equals(split[0])
                || "schema_region_ratis_snapshot_trigger_threshold".equals(split[0])
                || "data_region_ratis_snapshot_trigger_threshold".equals(split[0])
                || "config_node_simple_consensus_snapshot_trigger_threshold".equals(split[0])
                || "config_node_ratis_log_segment_size_max_in_byte".equals(split[0])
                || "schema_region_ratis_log_segment_size_max_in_byte".equals(split[0])
                || "data_region_ratis_log_segment_size_max_in_byte".equals(split[0])
                || "config_node_simple_consensus_log_segment_size_max_in_byte".equals(split[0])
                || "config_node_ratis_grpc_flow_control_window".equals(split[0])
                || "schema_region_ratis_grpc_flow_control_window".equals(split[0])
                || "data_region_ratis_grpc_flow_control_window".equals(split[0])
                || "config_node_ratis_rpc_leader_election_timeout_min_ms".equals(split[0])
                || "schema_region_ratis_rpc_leader_election_timeout_min_ms".equals(split[0])
                || "data_region_ratis_rpc_leader_election_timeout_min_ms".equals(split[0])
                || "config_node_ratis_rpc_leader_election_timeout_max_ms".equals(split[0])
                || "schema_region_ratis_rpc_leader_election_timeout_max_ms".equals(split[0])
                || "data_region_ratis_rpc_leader_election_timeout_max_ms".equals(split[0])
                || "config_node_ratis_request_timeout_ms".equals(split[0])
                || "schema_region_ratis_request_timeout_ms".equals(split[0])
                || "data_region_ratis_request_timeout_ms".equals(split[0])
                || "config_node_ratis_initial_sleep_time_ms".equals(split[0])
                || "config_node_ratis_max_sleep_time_ms".equals(split[0])
                || "schema_region_ratis_initial_sleep_time_ms".equals(split[0])
                || "schema_region_ratis_max_sleep_time_ms".equals(split[0])
                || "data_region_ratis_initial_sleep_time_ms".equals(split[0])
                || "data_region_ratis_max_sleep_time_ms".equals(split[0])
                || "config_node_ratis_preserve_logs_num_when_purge".equals(split[0])
                || "schema_region_ratis_preserve_logs_num_when_purge".equals(split[0])
                || "data_region_ratis_preserve_logs_num_when_purge".equals(split[0])
                || "ratis_first_election_timeout_min_ms".equals(split[0])
                || "ratis_first_election_timeout_max_ms".equals(split[0])) {
              text[6] = "long";
            } else {
              text[6] = "int";
            }
            text[7] = split[1];
            text[8] = split[1];
            text[9] = split[1];
            text[10] = split[1];
            // unit
            text[11] = "";

            String[] lastFileName = fileName.split("/");
            if (lastFileName[lastFileName.length - 1].equals(DATANODE_FILE_NAME)) {
              text[12] = DATANODE_FILE_NAME;
            } else if (lastFileName[lastFileName.length - 1].equals(CONFIG_FILE_NAME)) {
              text[12] = CONFIG_FILE_NAME;
            } else if (lastFileName[lastFileName.length - 1].equals(COMMON_FILE_NAME)) {
              text[12] = COMMON_FILE_NAME;
            }
            if (text[1] == null) {
              text[1] = "";
            } else {
              String replace = text[1].replace("#", "").replace("<", "small");
              if (replace.startsWith("null ")) {
                replace = replace.substring(5);
              }
              text[1] = replace;
            }
            text[0] = text[0].replace(" ", "");
            list.add(text);
            beforeNum = currentNum;
          }
          currentNum++;
        }
        for (String[] strings : list) {
          createXml(
              strings[0],
              strings[1],
              strings[2],
              strings[3],
              strings[4],
              strings[5],
              strings[6],
              strings[7],
              strings[8],
              strings[9],
              strings[10],
              strings[11],
              strings[12],
              allFlags);
          //
        }
      } catch (IOException e) {
        logger.error("Failed to generate xml document", e);
      }
    }
    createDictionaries(dictionaries, allFlags);
    OutputFormat format = OutputFormat.createPrettyPrint();
    format.setEncoding("UTF-8");

    // generate xml file
    File file = new File(xmlFilePath);
    XMLWriter writer = null;
    try {
      writer = new XMLWriter(new FileOutputStream(file), format);
      // Set whether to escape, the default is to use escape characters
      writer.setEscapeText(false);
      writer.write(document);
      writer.close();
    } catch (IOException e) {
      logger.error("Failed to generate xml file", e);
    }
  }

  public static void updateConfigNodeParameterInformation() throws PropertiesEmptyException {
    setValue(
        PropertiesUtils.updateProperties(
            PropertiesUtils.getConfigNodePropsUrl(DATANODE_FILE_NAME), DATANODE_FILE_NAME),
        PropertiesUtils.getConfigNodePropsUrl(DATANODE_FILE_NAME));
    setValue(
        PropertiesUtils.updateProperties(
            PropertiesUtils.getConfigNodePropsUrl(CONFIG_FILE_NAME), CONFIG_FILE_NAME),
        PropertiesUtils.getConfigNodePropsUrl(CONFIG_FILE_NAME));
    setValue(
        PropertiesUtils.updateProperties(
            PropertiesUtils.getConfigNodePropsUrl(COMMON_FILE_NAME), COMMON_FILE_NAME),
        PropertiesUtils.getConfigNodePropsUrl(COMMON_FILE_NAME));
    writeInFile(PropertiesUtils.getConfigNodePropsUrl(DATANODE_ENV_BAT));
    writeInFile(PropertiesUtils.getConfigNodePropsUrl(DATANODE_ENV_SH));
    writeInFile(PropertiesUtils.getConfigNodePropsUrl(CONFIGNODE_ENV_SH));
    writeInFile(PropertiesUtils.getConfigNodePropsUrl(CONFIGNODE_ENV_BAT));
  }

  public static void updateDataNodeParameterInformation() throws PropertiesEmptyException {
    setValue(
        PropertiesUtils.updateProperties(
            PropertiesUtils.getDataNodePropsUrl(DATANODE_FILE_NAME), DATANODE_FILE_NAME),
        PropertiesUtils.getDataNodePropsUrl(DATANODE_FILE_NAME));
    setValue(
        PropertiesUtils.updateProperties(
            PropertiesUtils.getDataNodePropsUrl(CONFIG_FILE_NAME), CONFIG_FILE_NAME),
        PropertiesUtils.getDataNodePropsUrl(CONFIG_FILE_NAME));
    setValue(
        PropertiesUtils.updateProperties(
            PropertiesUtils.getDataNodePropsUrl(COMMON_FILE_NAME), COMMON_FILE_NAME),
        PropertiesUtils.getDataNodePropsUrl(COMMON_FILE_NAME));
    writeInFile(PropertiesUtils.getDataNodePropsUrl(DATANODE_ENV_BAT));
    writeInFile(PropertiesUtils.getDataNodePropsUrl(DATANODE_ENV_SH));
    writeInFile(PropertiesUtils.getDataNodePropsUrl(CONFIGNODE_ENV_SH));
    writeInFile(PropertiesUtils.getDataNodePropsUrl(CONFIGNODE_ENV_BAT));
  }
}
