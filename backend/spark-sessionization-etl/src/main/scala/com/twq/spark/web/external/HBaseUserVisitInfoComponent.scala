package com.twq.spark.web.external

import com.twq.spark.web.CombinedId
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.LoggerFactory
import resource._

object HBaseUserVisitInfoComponent {
  private val logger = LoggerFactory.getLogger(classOf[HBaseUserVisitInfoComponent])
  private val hbaseConn = HbaseConnectionFactory.getHbaseConn

  private val targetUserTable =
    TableName.valueOf(HbaseConnectionFactory.hbaseTableNamespace,
      System.getProperty("web.etl.hbase.UserTableName", "web-user"))

  private val columnFamily = "f".getBytes("UTF-8")

  private val lastVisit = "v".getBytes("UTF-8")
}

/**
  * 利用hbase来实现用户访问状态信息的存取
  */
trait HBaseUserVisitInfoComponent extends UserVisitInfoComponent {

  import HBaseUserVisitInfoComponent._

  /**
    * 根据user唯一标识批量从hbase中查询出user的访问信息
    *
    * @param ids user唯一标识列表
    * @return 每一个user的访问信息
    */
  override def retrieveUsersVisitInfo(ids: Seq[CombinedId]): Map[CombinedId, UserVisitInfo] = {
    val gets = ids.map(id => new Get(id.encode.getBytes("UTF-8")).addFamily(columnFamily))
    managed(hbaseConn.getTable(targetUserTable)) map { userTable =>
      val results = userTable.get(scala.collection.JavaConversions.seqAsJavaList(gets))
      ids zip results map { case (id, result) =>
        if (result.isEmpty) {
          None
        } else {
          val lastVisitCell = result.getColumnLatestCell(columnFamily, lastVisit)
          lastVisitCell match {
            case null => logger.error("{} -- 'visit' cells are null", id); None
            case c1 => Some(id ->
              UserVisitInfo(id, c1.getTimestamp, Bytes.toInt(c1.getValueArray, c1.getValueOffset, c1.getValueLength)))
          }
        }
      }
    } either match {
      case Left(exceptions) =>
        logger.error("Failed to retrieve the user visit info, details: ", exceptions)
        Map.empty[CombinedId, UserVisitInfo]
      case Right(result) => result.flatten.toMap
    }
  }

  override def updateUsersVisitInfo(usersInfo: Seq[UserVisitInfo]): Unit = {
    val puts = usersInfo map { userVisitInfo =>
      val rowKey = userVisitInfo.id.encode.getBytes("UTF-8")
      val put = new Put(rowKey)
      put.addColumn(columnFamily, lastVisit,
        userVisitInfo.lastVisitTime, Bytes.toBytes(userVisitInfo.lastVisitIndex))
      put
    }

    for (table <- managed(hbaseConn.getTable(targetUserTable))) {
      logger.debug(s"Trying to update ${puts.size} users' visit stats , detailed stats: ${usersInfo}, put cmds: ${puts}")
      val putList = scala.collection.JavaConversions.seqAsJavaList(puts)
      try {
        table.batch(putList, Array.fill(putList.size)(new Object))
      } catch {
        case e: Exception => logger.error("Failed to do the batch puts in hbase.", e)
      }
    }
  }

}