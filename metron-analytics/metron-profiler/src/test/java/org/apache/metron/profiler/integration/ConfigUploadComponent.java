/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.profiler.integration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.KafkaWithZKComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.apache.metron.common.configuration.ConfigurationsUtils.*;

public class ConfigUploadComponent implements InMemoryComponent {

  private Properties topologyProperties;
  private String globalConfigPath;
  private String parserConfigsPath;
  private String enrichmentConfigsPath;
  private String profilerConfigPath;
  private Optional<String> globalConfig = Optional.empty();
  private Map<String, SensorParserConfig> parserSensorConfigs = new HashMap<>();

  public ConfigUploadComponent withTopologyProperties(Properties topologyProperties) {
    this.topologyProperties = topologyProperties;
    return this;
  }

  public ConfigUploadComponent withGlobalConfigsPath(String globalConfigPath) {
    this.globalConfigPath = globalConfigPath;
    return this;
  }

  public ConfigUploadComponent withParserConfigsPath(String parserConfigsPath) {
    this.parserConfigsPath = parserConfigsPath;
    return this;
  }
  public ConfigUploadComponent withEnrichmentConfigsPath(String enrichmentConfigsPath) {
    this.enrichmentConfigsPath = enrichmentConfigsPath;
    return this;
  }

  public ConfigUploadComponent withProfilerConfigsPath(String profilerConfigsPath) {
    this.profilerConfigPath = profilerConfigsPath;
    return this;
  }

  public ConfigUploadComponent withParserSensorConfig(String name, SensorParserConfig config) {
    parserSensorConfigs.put(name, config);
    return this;
  }

  public ConfigUploadComponent withGlobalConfig(String globalConfig) {
    this.globalConfig = Optional.ofNullable(globalConfig);
    return this;
  }

  @Override
  public void start() throws UnableToStartException {
    try {
      final String zookeeperUrl = topologyProperties.getProperty(KafkaWithZKComponent.ZOOKEEPER_PROPERTY);

      if(globalConfigPath != null) {
        uploadConfigsToZookeeper(globalConfigPath, parserConfigsPath, enrichmentConfigsPath, profilerConfigPath, zookeeperUrl);
      }

      for(Map.Entry<String, SensorParserConfig> kv : parserSensorConfigs.entrySet()) {
        writeSensorParserConfigToZookeeper(kv.getKey(), kv.getValue(), zookeeperUrl);
      }

      if(globalConfig.isPresent()) {
        writeGlobalConfigToZookeeper(globalConfig.get().getBytes(), zookeeperUrl);
      }

    } catch (Exception e) {
      throw new UnableToStartException(e.getMessage(), e);
    }
  }

  public SensorParserConfig getSensorParserConfig(String sensorType) {
    SensorParserConfig sensorParserConfig = new SensorParserConfig();
    CuratorFramework client = getClient(topologyProperties.getProperty(KafkaWithZKComponent.ZOOKEEPER_PROPERTY));
    client.start();
    try {
      sensorParserConfig = readSensorParserConfigFromZookeeper(sensorType, client);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }
    return sensorParserConfig;
  }

  @Override
  public void stop() {

  }
}
