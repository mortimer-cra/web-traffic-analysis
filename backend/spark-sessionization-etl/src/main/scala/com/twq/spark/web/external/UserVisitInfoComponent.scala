package com.twq.spark.web.external

import com.twq.spark.web.CombinedId

trait UserVisitInfoComponent {
  /**
    *  根据访客唯一标识查询访客的历史访问信息
    * @param ids
    * @return
    */
  def retrieveUsersVisitInfo(ids: Seq[CombinedId]): Map[CombinedId, UserVisitInfo]

  /**
    *  更新访客的历史访问信息
    * @param users
    */
  def updateUsersVisitInfo(users: Seq[UserVisitInfo]): Unit
}
