package com.vtex.akkahttpseed.models

/**
  * Created by felipe on 14/06/16.
  */
case class DailyQuoteResult(dataset: DatasetData)

case class DatasetData(data:List[(String,Double)])
