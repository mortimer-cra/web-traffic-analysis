package com.twq.spark.web.external

import com.twq.spark.web.CombinedId
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.scalatest.FlatSpec

class HBaseUserVisitInfoComponentTest extends FlatSpec with HBaseUserVisitInfoComponent {

  behavior of "HBaseUserVisitInfoComponentTest"

  private val columnFamily = "f".getBytes("UTF-8")

  private val lastVisit = "v".getBytes("UTF-8")

  System.setProperty("web.etl.hbase.zk.quorums", "master")
  System.setProperty("web.etl.hbase.UserTableName", "web-user-test")
  val hbaseConn = HbaseConnectionFactory.getHbaseConn
  val admin = hbaseConn.getAdmin
  val tableName = TableName.valueOf("web-user-test")
  if (!admin.tableExists(tableName)) {
    val hTableDescriptor = new HTableDescriptor(tableName)
    hTableDescriptor.addFamily(new HColumnDescriptor("f"))
    admin.createTable(hTableDescriptor)
  }
  admin.disableTable(tableName)
  admin.truncateTable(tableName, true)
  val userTable = hbaseConn.getTable(tableName)

  val userVisitInfo1 = new UserVisitInfo(CombinedId(1, "user1"), 123, 2)
  val put1 = new Put(userVisitInfo1.id.encode.getBytes("utf-8"))
  put1.addColumn(columnFamily, lastVisit,
    userVisitInfo1.lastVisitTime, Bytes.toBytes(userVisitInfo1.lastVisitIndex))
  val userVisitInfo2 = new UserVisitInfo(CombinedId(2, "user1"), 1234, 4)
  val put2 = new Put(userVisitInfo2.id.encode.getBytes("utf-8"))
  put2.addColumn(columnFamily, lastVisit,
    userVisitInfo2.lastVisitTime, Bytes.toBytes(userVisitInfo2.lastVisitIndex))
  userTable.put(put1)
  userTable.put(put2)
  userTable.close()

  it should "retrieveUsersVisitInfo" in {
    val ids = Seq(CombinedId(1, "user1"), CombinedId(2, "user1"))
    val userVisitInfoMap = retrieveUsersVisitInfo(ids)
    assert(2 == userVisitInfoMap.size)
    val userVisitInfo1 = userVisitInfoMap.get(CombinedId(1, "user1")).get
    assert(userVisitInfo1.lastVisitIndex == 2)
    assert(userVisitInfo1.lastVisitTime == 123)
    val userVisitInfo2 = userVisitInfoMap.get(CombinedId(2, "user1")).get
    assert(userVisitInfo2.lastVisitIndex == 4)
    assert(userVisitInfo2.lastVisitTime == 1234)
  }

  it should "updateUsersVisitInfo" in {
    val userVisitInfo3 = new UserVisitInfo(CombinedId(3, "user1"), 222, 3)
    val userVisitInfo4 = new UserVisitInfo(CombinedId(4, "user1"), 333, 4)

    updateUsersVisitInfo(Seq(userVisitInfo3, userVisitInfo4))

    val userVisitInfoMap = retrieveUsersVisitInfo(Seq(CombinedId(3, "user1"), CombinedId(4, "user1")))
    assert(2 == userVisitInfoMap.size)
    val userVisitInfo3assert = userVisitInfoMap.get(CombinedId(3, "user1")).get
    assert(userVisitInfo3assert.lastVisitIndex == 3)
    assert(userVisitInfo3assert.lastVisitTime == 222)
    val userVisitInfo4assert = userVisitInfoMap.get(CombinedId(4, "user1")).get
    assert(userVisitInfo4assert.lastVisitIndex == 4)
    assert(userVisitInfo4assert.lastVisitTime == 333)
  }

}
