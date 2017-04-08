package com.bowlingx

import scala.concurrent.Future
import scala.util.Try

trait Engine {

  /**
    * Delegates a rendering of something to a `method`
    *
    * @param method    name of the method to call
    * @param arguments arguments
    * @tparam T any argument type
    * @return
    */
  def render[T <: Any](method: String, arguments: T*): Future[Try[Option[AnyRef]]]

}