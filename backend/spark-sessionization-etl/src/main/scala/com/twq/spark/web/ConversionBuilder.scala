package com.twq.spark.web

import com.twq.parser.dataobject.dim.TargetPageInfo
import com.twq.parser.dataobject.{EventDataObject, TargetPageDataObject}
import com.twq.web.{Conversion, ConversionType}

object ConversionBuilder {
  private val DEFAULT_CONVERSION_VALUE = 1F
}

trait ConversionBuilder {
  import ConversionBuilder._
  import com.twq.parser.utils.ParserUtils._

  private var conversionIdIncrValue: Long = 0L

  def buildConversions(sessionId: Long,
                       targetInfoData: Seq[(TargetPageDataObject, TargetPageInfo)],
                       eventData: Seq[EventDataObject]): Seq[Conversion] = {
    val event2Conversions = getConversionFromEvent(eventData, sessionId)
    val targetPage2Conversions = getConversionFromTargetPage(targetInfoData, sessionId)

    event2Conversions ++ targetPage2Conversions
  }

  /**
    *  事件转成转化
    * @param eventData
    * @param sessionId
    * @return
    */
  private def getConversionFromEvent(eventData: Seq[EventDataObject], sessionId: Long) = {
    eventData map { event =>
      val conversion = Conversion.newBuilder().build()
      conversion.setConversionId(generateConversionId(sessionId))
      conversion.setType(ConversionType.EVENT.getValue)
      conversion.setCategory(notNull(event.getEventCategory))
      conversion.setLabel(notNull(event.getEventLabel))
      conversion.setName(notNull(event.getEventAction))
      conversion.setConversionPageUrl(notNull(event.getUrl))
      conversion.setConversionPageTitle(notNull(event.getTitle))
      conversion.setConversionPageHostname(notNull(event.getHostDomain).toLowerCase)
      conversion.setConversionValue(event.getEventValue)
      conversion.setConversionServerTime(event.getServerTimeString)
      conversion
    }
  }

  /**
    *  目标页面也是转化
    * @param activeTargetInfoArray
    * @param sessionId
    * @return
    */
  private def getConversionFromTargetPage(activeTargetInfoArray: Seq[(TargetPageDataObject, TargetPageInfo)],
                                          sessionId: Long) = {
    activeTargetInfoArray map { case (target, targetInfo) =>
      val conversion = Conversion.newBuilder().build()
        conversion.setConversionId(generateConversionId(sessionId))
      conversion.setType(ConversionType.TARGET_PAGE.getValue)
      conversion.setName(targetInfo.getTargetName)
      conversion.setConversionPageUrl(target.getPvDataObject.getSiteResourceInfo.getUrl)
      conversion.setConversionPageTitle(target.getPvDataObject.getSiteResourceInfo.getPageTitle)
      conversion.setConversionPageHostname(notNull(target.getPvDataObject.getSiteResourceInfo.getDomain).toLowerCase)
      conversion.setConversionValue(DEFAULT_CONVERSION_VALUE)
      conversion.setConversionServerTime(target.getServerTimeString)
      conversion
    }
  }

  private def generateConversionId(sessionId: Long) = {
    conversionIdIncrValue += 1
    s"^!${sessionId}_${conversionIdIncrValue}"
  }

}
