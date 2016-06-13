package com.queirozf.akkahttpseed

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializerSettings, ActorMaterializer}

/**
  * Created by felipe on 12/06/16.
  */
object Main extends App{

  implicit val system = ActorSystem("main-actor-system")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val ec = system.dispatcher

  val sqsConnector = system.actorOf

}
