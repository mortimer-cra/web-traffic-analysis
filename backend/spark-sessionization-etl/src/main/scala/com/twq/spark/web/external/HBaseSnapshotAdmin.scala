package com.twq.spark.web.external

import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Connection
import org.slf4j.LoggerFactory
import resource._


object HBaseSnapshotAdmin {
  private val logger = LoggerFactory.getLogger(classOf[HBaseSnapshotAdmin])
}

class HBaseSnapshotAdmin(conn: Connection) {

  import HBaseSnapshotAdmin._

  def takeSnapshot(snapshotName: String, tableName: String): Unit = {
    for (admin <- managed(conn.getAdmin)) {
      val table = TableName.valueOf(HbaseConnectionFactory.hbaseTableNamespace, tableName)

      admin.disableTable(table)

      // check if snapshot with the name already exists
      val snapshots = admin.listSnapshots(snapshotName)

      if (!snapshots.isEmpty) {
        logger.warn(s"Snapshot ${snapshotName} already exists, deleting it and take the latest snapshot again.")
        admin.deleteSnapshot(snapshotName)
      }

      admin.snapshot(snapshotName, table)

      admin.enableTable(table)
    }

    logger.info(s"Successfully took a snapshot ${snapshotName} for table ${tableName}.")
  }
}
