package com.twq.spark.web.external

import com.twq.spark.web.CombinedId

/**
  * 用户访问信息
  * @param id 用户的唯一标识
  * @param lastVisitTime  上次访问时间
  * @param lastVisitIndex 上次访问次数
  */
case class UserVisitInfo(
                          id: CombinedId,
                          lastVisitTime: Long,
                          lastVisitIndex: Int)

object UserVisitInfo {
  val INIT_LAST_VISIT_TIME = 0L
  val INIT_LAST_VISIT_INDEX = 0
}