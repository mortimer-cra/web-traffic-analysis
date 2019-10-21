package com.twq.spark.web

import com.twq.parser.dataobject.BaseDataObject
import com.twq.spark.web.external.{HBaseUserVisitInfoComponent, UserVisitInfo}
import com.twq.spark.web.session.{SessionGenerator, UserSessionDataAggregator}

/**
  * 每一个分区数据的处理者(分区级别)
  *
  * @param index              分区的index
  * @param dataPerProfileUser 分区中的数据
  * @param outputBasePath     分区输出的路径
  * @param dateStr            处理的数据日期
  */
class PartitionProcessor(
                          index: Int,
                          dataPerProfileUser: Iterator[(CombinedId, Iterable[BaseDataObject])],
                          outputBasePath: String,
                          dateStr: String)
  extends SessionGenerator with AvroOutputComponent with HBaseUserVisitInfoComponent{

  //一个user的开始的serverSessionId
  private var serverSessionIdStart = index.toLong + System.currentTimeMillis() * 1000

  def run() = {
    //循环处理每一个user的DataObjects
    dataPerProfileUser grouped (512) foreach { case currentBatch =>
      val ids = currentBatch.map(_._1)
      val usersVisitInfo = retrieveUsersVisitInfo(ids)

      val persistUserVisitInfoBuilder = Vector.newBuilder[UserVisitInfo]

      currentBatch foreach { case (profileUser, dataObjects) =>
        //对一个user中的所有的DataObject按照时间进行升序排序
        val sortedObjectSeq = dataObjects.toSeq.sortBy(obj => obj.getServerTime.getTime)
        //会话的切割
        val sessionData = groupDataObjects(sortedObjectSeq)
        //对当前user产生的会话进行聚合计算
        val aggregator = new UserSessionDataAggregator(profileUser, serverSessionIdStart, usersVisitInfo.get(profileUser))
        val (userVisitInfo, records) = aggregator.aggregate(sessionData)
        persistUserVisitInfoBuilder += userVisitInfo
        serverSessionIdStart += sessionData.size
        //将产生的记录写到HDFS中
        writeDataRecords(records, outputBasePath, dateStr, index)
      }

      updateUsersVisitInfo(persistUserVisitInfoBuilder.result())
    }
  }

}
