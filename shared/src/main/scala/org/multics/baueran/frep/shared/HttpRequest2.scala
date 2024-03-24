package org.multics.baueran.frep.shared

import sttp.client4.*
import sttp.model.Header
import sttp.model.Method

import scala.util.{Failure, Success, Try}
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.frontend.{apiPrefix, serverUrl}

/** Minimal wrapper around a networking library so that eventually the
  * networking library can be replaced more easily if need be.
  * (used to be RosHTTP is now sttp; so API is RosHTTP-inspired)
  */

case class HttpRequest2(apiEndPoint: String) {

  private var _onSuccessCallback: (String) => Unit = null
  private var _onFailureCallback: (String) => Unit = null
  private val _headers = scala.collection.mutable.Map[String, String]()
  private val _qParams = scala.collection.mutable.Map[String, String]()
  private val _bParams = scala.collection.mutable.Map[String, String]()
  private var _method = Method("GET")

  // -1 is the response code if there wasn't in fact any response from the backend
  private def afterException(apiURI: String, responseCode: Int = -1) = {
    new HttpRequest2DefaultExceptionHandler(apiURI, responseCode)()
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
              println(s"HttpRequest2: failed to parse ${apiURI}: ${message}")
              afterException(apiURI, response.code.code)
            }
            else
              _onFailureCallback(message)
        }
      case Failure(message) =>
        println(s"HttpRequest2: call to ${apiURI} failed: ${message}")
        afterException(apiURI)
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
