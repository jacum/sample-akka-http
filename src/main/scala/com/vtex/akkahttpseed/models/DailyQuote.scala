package com.vtex.akkahttpseed.models

case class DailyQuoteResult(dataset: DatasetData)

case class DatasetData(data:List[(String,Double)])
