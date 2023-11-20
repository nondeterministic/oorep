package org.multics.baueran.frep.shared

import org.scalajs.dom
import sttp.client4._
import sttp.model.Header
import sttp.model.Method
import scala.util.{Failure, Success, Try}
import monix.execution.Scheduler.Implicits.global

import org.multics.baueran.frep.shared.frontend.Notify
import org.multics.baueran.frep.shared.frontend.{apiPrefix, serverUrl}

/** Minimal wrapper around a networking library (in this case: RosHTTP)
  * so that eventually the networking library can be replaced more easily
  * if need be.
  */

case class HttpRequest2(apiEndPoint: String) {

  private var _onSuccessCallback: (String) => Unit = null
  private var _onFailureCallback: (String) => Unit = null
  private val _headers = scala.collection.mutable.Map[String, String]()
  private val _qParams = scala.collection.mutable.Map[String, String]()
  private val _bParams = scala.collection.mutable.Map[String, String]()
  private var _method = Method("GET")

  // This will ensure the app stays response after a network transmission error
  // A bit clumsy, as this is some kind of side-effect, but OK for now...
  private def afterException() = {
    if (Notify.noAlertsVisible())
      new Notify("tempFeedbackAlert", "ERROR: Network failure. Please, try again!")
    dom.document.body.classList.remove("wait")
  }

  def onSuccess(callback: (String) => Unit): HttpRequest2 = {
    _onSuccessCallback = callback
    this
  }

  def onFailure(callback: (String) => Unit): HttpRequest2 = {
    _onFailureCallback = callback
    this
  }

  def withHeaders(headers: (String, String)*): HttpRequest2 = {
    headers.map { case (k, v) => _headers += (k -> v) }
    this
  }

  def withQueryParameters(qParams: (String, String)*): HttpRequest2 = {
    qParams.map { case (k, v) => _qParams += (k -> v) }
    this
  }

  def withMethod(method: String): HttpRequest2 = {
    _method = Method(method)
    this
  }

  def withBody(bParams: (String, String)*): HttpRequest2 = {
    bParams.map { case (k, v) => _bParams += (k -> v) }
    this
  }

  private def onComplete(response: Try[Response[Either[String, String]]], apiURI: String): Unit = {
    response match {
      case Success(response) =>
        response.body match {
          case Right(body) =>
            if (_onSuccessCallback != null)
              _onSuccessCallback(body)
          case Left(message) =>
            if (_onFailureCallback == null) { // Default error handler
              println(s"HttpRequest2: failed to parse ${apiURI}: ${message.substring(0,20)}")
              afterException()
            }
            else
              _onFailureCallback(message)
        }
      case Failure(message) =>
        println(s"HttpRequest2: call to ${apiURI} failed: ${message}")
        afterException()
    }
  }

  private def createRequest(): PartialRequest[Either[String,String]] = {
    var req = basicRequest

    // https://sttp.softwaremill.com/en/stable/requests/basics.html#query-parameters-and-uris
    if (!_headers.isEmpty)
      req = req.withHeaders(_headers.toSeq.map(h => Header(h._1,h._2)))
    if (!_bParams.isEmpty)
      req = req.body(_bParams.toMap)

    req
  }

  def send(): Unit = {
    var apiURI = uri"${serverUrl()}/${apiPrefix()}/${apiEndPoint.split('/').toList}"
    if (!_qParams.isEmpty)
      apiURI = uri"${serverUrl()}/${apiPrefix()}/${apiEndPoint.split('/').toList}?${_qParams.toMap}"

    val response = {
      createRequest()
        .method(_method, apiURI)
        .send(DefaultFutureBackend())
    }

    response.onComplete(onComplete(_, apiURI.toString()))
  }

  def put(body: (String, String)*): Unit = {
    val apiURI = uri"${serverUrl()}/${apiPrefix()}/${apiEndPoint.split('/').toList}"
    val response =
      createRequest()
        .put(apiURI)
        .body(body.toMap)
        .send(DefaultFutureBackend())

    response.onComplete(onComplete(_, apiURI.toString()))
  }

  def post(body: (String, String)*): Unit = {
    val apiURI = uri"${serverUrl()}/${apiPrefix()}/${apiEndPoint.split('/').toList}"
    val response =
      createRequest()
        .post(apiURI)
        .body(body.toMap)
        .send(DefaultFutureBackend())

    response.onComplete(onComplete(_, apiURI.toString()))
  }

  def post(body: String): Unit = {
    val apiURI = uri"${serverUrl()}/${apiPrefix()}/${apiEndPoint.split('/').toList}"
    val response =
      createRequest()
        .post(apiURI)
        .body(body)
        .send(DefaultFutureBackend())

    response.onComplete(onComplete(_, apiURI.toString()))
  }

}
