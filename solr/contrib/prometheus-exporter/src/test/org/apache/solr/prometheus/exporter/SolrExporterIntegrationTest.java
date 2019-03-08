/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.prometheus.exporter;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.util.LuceneTestCase.Slow;
import org.junit.Before;
import org.junit.Test;

@Slow
public class SolrExporterIntegrationTest extends SolrExporterTestBase {

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    startMetricsExporterWithConfiguration("conf/prometheus-solr-exporter-integration-test-config.xml");
  }

  private Map<String, Double> metricsWithName(Map<String, Double> allMetrics, String name) {
    return allMetrics.entrySet()
        .stream()
        .filter(entry -> entry.getKey().startsWith(name))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Test
  public void pingAllCollectionsAndCoresAreAvailable() throws Exception {
    Map<String, Double> pingMetrics = metricsWithName(getAllMetrics(), "solr_ping");

    assertEquals(5, pingMetrics.size());

    for (Map.Entry<String, Double> metric : pingMetrics.entrySet()) {
      assertEquals(1.0, metric.getValue(), 0.001);
    }
  }

  @Test
  public void solrExporterDurationMetric() throws Exception {
    Map<String, Double> durationHistogram = metricsWithName(getAllMetrics(), "solr_exporter_duration");

    assertTrue(durationHistogram.get("solr_exporter_duration_seconds_count") > 0);
    assertTrue(durationHistogram.get("solr_exporter_duration_seconds_sum") > 0);

    // 17 = (15 buckets in the histogram) + (count metric) + (sum metric)
    assertEquals(17, durationHistogram.size());
  }

  @Test
  public void jvmMetrics() throws Exception {
    Map<String, Double> jvmMetrics = metricsWithName(getAllMetrics(), "solr_metrics_jvm_threads{item=\"terminated\"");
    assertEquals(NUM_NODES, jvmMetrics.size());
  }

  @Test
  public void jsonFacetMetrics() throws Exception {
    Map<String, Double> facetMetrics = metricsWithName(getAllMetrics(), "solr_facets_category");
    assertEquals(FACET_VALUES.size(), facetMetrics.size());
  }

  @Test
  public void collectionMetrics() throws Exception {
    Map<String, Double> allMetrics = getAllMetrics();
    Map<String, Double> liveNodeMetrics = metricsWithName(allMetrics, "solr_collections_live_nodes");

    assertEquals(1, liveNodeMetrics.size());
    liveNodeMetrics.forEach((metric, value) -> {
      assertEquals((double) NUM_NODES, value, 0.001);
    });

    Map<String, Double> shardLeaderMetrics = metricsWithName(allMetrics, "solr_collections_shard_leader");

    assertEquals(NUM_NODES, shardLeaderMetrics.size());

    double totalLeaderCount = shardLeaderMetrics.values()
        .stream()
        .mapToDouble(Double::doubleValue)
        .sum();

    assertEquals(NUM_SHARDS, totalLeaderCount, 0.001);
  }
}