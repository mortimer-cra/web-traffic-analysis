package com.twq.spark.web.external

import java.util.concurrent._

import com.github.rholder.retry.{RetryerBuilder, StopStrategies, WaitStrategies}
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}
import org.slf4j.LoggerFactory

object HbaseConnectionFactory {
  private val logger = LoggerFactory.getLogger(HbaseConnectionFactory.getClass)
  private lazy val zks = Option(System.getProperty("web.etl.hbase.zk.quorums"))
    .getOrElse(sys.error("must set parameter web.etl.hbase.zk.quorums"))
  private val zkPort = System.getProperty("web.etl.hbase.zk.port", "2181")
  val hbaseTableNamespace = System.getProperty("web.etl.hbase.namespace", "default")

  private lazy val hbaseConfig = {
    val conf = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum", zks)
    conf.set("hbase.zookeeper.property.clientPort", zkPort)
    conf
  }

  private val lock = new Object()

  private var connection = createHbaseConnectionWithRetry

  def getHbaseConn = {
    if (isNormalHbaseConn(connection)) {
      connection
    } else {
      lock.synchronized {
        if (isNormalHbaseConn(connection)) {
          connection
        } else {
          val newConn = createHbaseConnectionWithRetry
          connection = newConn
          newConn
        }
      }
    }
  }


  private def isNormalHbaseConn(conn: Connection): Boolean = {
    conn != null && !conn.isClosed && !conn.isAborted
  }

  private def createHbaseConnectionWithRetry = {
    val callable = new Callable[Connection]() {
      override def call(): Connection = {
        logger.info("creating hbase connection for etl")
        ConnectionFactory.createConnection(hbaseConfig)
      }
    }

    val retryer = RetryerBuilder.newBuilder[Connection]()
      .retryIfException()
      .withWaitStrategy(WaitStrategies.exponentialWait(1000, 5, TimeUnit.MINUTES))
      .withStopStrategy(StopStrategies.stopAfterAttempt(Integer.getInteger("wd.etl.hbase.connection.retryTimes", 4)))
      .build()

    retryer.call(callable)
  }

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      if (isNormalHbaseConn(connection)) {
        logger.info("closing hbase connection for etl")
        connection.close()
      }
    }
  })
}
