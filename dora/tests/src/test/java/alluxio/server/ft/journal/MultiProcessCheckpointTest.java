/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.server.ft.journal;

import static org.junit.Assert.assertEquals;

import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.client.file.FileSystem;
import alluxio.client.metrics.MetricsMasterClient;
import alluxio.conf.PropertyKey;
import alluxio.master.journal.JournalType;
import alluxio.metrics.MetricKey;
import alluxio.multi.process.MultiProcessCluster;
import alluxio.multi.process.PortCoordination;
import alluxio.testutils.IntegrationTestUtils;

import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

@Ignore("In Dora, Client does not use Master/Journal services.")
public class MultiProcessCheckpointTest {

  @Test
  public void testDefault() throws Exception {
    runTest(-1, false);
  }

  @Test
  public void testNoCompression() throws Exception {
    runTest(0, false);
  }

  @Test
  public void testParallelCompression() throws Exception {
    runTest(-1, true);
  }

  void runTest(int compressionLevel, boolean parallelCompression) throws Exception {
    MultiProcessCluster cluster = MultiProcessCluster.newBuilder(PortCoordination.CHECKPOINT)
        .setClusterName("CheckpointTest")
        .addProperty(PropertyKey.MASTER_JOURNAL_TYPE, JournalType.UFS)
        .addProperty(PropertyKey.ZOOKEEPER_SESSION_TIMEOUT, "2sec")
        .addProperty(PropertyKey.ZOOKEEPER_CONNECTION_TIMEOUT, "1sec")
        .addProperty(PropertyKey.MASTER_METASTORE, "ROCKS")
        .addProperty(PropertyKey.MASTER_JOURNAL_CHECKPOINT_PERIOD_ENTRIES, 100)
        .addProperty(PropertyKey.MASTER_JOURNAL_LOG_SIZE_BYTES_MAX, "500")
        .addProperty(PropertyKey.MASTER_JOURNAL_TAILER_SHUTDOWN_QUIET_WAIT_TIME_MS, "500")
        .addProperty(PropertyKey.MASTER_METASTORE_ROCKS_CHECKPOINT_COMPRESSION_LEVEL,
            compressionLevel)
        .addProperty(PropertyKey.MASTER_METASTORE_ROCKS_PARALLEL_BACKUP,
            parallelCompression)
        .setNumMasters(2)
        .setNumWorkers(0)
        .build();
    try {
      cluster.start();
      cluster.waitForAllNodesRegistered(20 * Constants.SECOND_MS);
      String journal = cluster.getJournalDir();
      FileSystem fs = cluster.getFileSystemClient();
      int numFiles = 100;
      for (int i = 0; i < numFiles; i++) {
        fs.createFile(new AlluxioURI("/file" + i)).close();
      }
      MetricsMasterClient metricsClient = cluster.getMetricsMasterClient();
      assertEquals(numFiles + 1, (long) metricsClient.getMetrics()
          .get(MetricKey.MASTER_TOTAL_PATHS.getName()).getDoubleValue());
      IntegrationTestUtils.waitForUfsJournalCheckpoint(Constants.FILE_SYSTEM_MASTER_NAME,
          new URI(journal));
      cluster.stopMasters();
      cluster.startMasters();
      cluster.waitForAllNodesRegistered(60 * Constants.SECOND_MS);
      fs = cluster.getFileSystemClient();
      assertEquals(numFiles, fs.listStatus(new AlluxioURI("/")).size());
      assertEquals(numFiles + 1, (long) metricsClient.getMetrics()
          .get(MetricKey.MASTER_TOTAL_PATHS.getName()).getDoubleValue());
      cluster.notifySuccess();
    } finally {
      cluster.destroy();
    }
  }
}
