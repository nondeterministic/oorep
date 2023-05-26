package org.multics.baueran.frep.shared

// Stuff that RosHTTP needs...
import fr.hmil.roshttp.{BackendConfig, HttpRequest, Method}
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.frontend.Notify
import org.scalajs.dom

import scala.util.{Failure, Success, Try}

// Local imports...
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
  private var _bConfig = BackendConfig()
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

  def withChunkSize(value: Int): HttpRequest2 = {
    _bConfig = BackendConfig(maxChunkSize = value)
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

  private def onComplete(future: Try[SimpleHttpResponse]) = {
    future match {
      case response: Success[SimpleHttpResponse] =>
        if (_onSuccessCallback != null)
          _onSuccessCallback(response.get.body)
      case response: Failure[SimpleHttpResponse] =>
        if (_onFailureCallback == null) {  // Default error handler
          println(s"HttpRequest2: call to ${serverUrl()}/${apiPrefix()}/$apiEndPoint failed: ${response.get.body}")
          afterException()
        }
        else
          _onFailureCallback(response.get.body)
      case _ =>
        println(s"HttpRequest2: call to ${serverUrl()}/${apiPrefix()}/$apiEndPoint failed for unknown reason.")
        afterException()
    }
  }

  private def createRequest(): HttpRequest = {
    var req = HttpRequest(s"${serverUrl()}/${apiPrefix()}/$apiEndPoint")

    if (!_headers.isEmpty)
      req = req.withHeaders((_headers.toSeq): _*)
    if (!_qParams.isEmpty)
      req = req.withQueryParameters((_qParams.toSeq): _*)
    if (!_bParams.isEmpty)
      req = req.withBody(MultiPartBody((_bParams.map { case (v, k) => (v, PlainTextBody(k)) }).toList: _*))

    req
      .withBackendConfig(_bConfig)
      .withMethod(_method) // GET is default
  }

  def send(): Unit = {
    val req = createRequest().send()

    req.recover {
      case e: Exception =>
        println(s"HttpRequest2: send() to ${serverUrl()}/${apiPrefix()}/$apiEndPoint failed: ${e.getMessage}")
        afterException()
    }

    req.onComplete({ onComplete(_) })
  }

  def put(multiPartBody: (String, String)*): Unit = {
    val req = createRequest()
      .withMethod(Method.PUT) // GET is default
      .put(MultiPartBody(multiPartBody.map { case (v, k) => (v -> PlainTextBody(k)) }.toList: _*))

    req.recover {
      case e: Exception =>
        println(s"HttpRequest2: put() to ${serverUrl()}/${apiPrefix()}/$apiEndPoint failed: ${e.getMessage}")
        afterException()
    }

    req.onComplete({ onComplete(_) })
  }

  def post(multiPartBody: (String, String)*): Unit = {
    val req = createRequest()
      .withMethod(Method.POST) // GET is default
      .post(MultiPartBody(multiPartBody.map { case (v, k) => (v -> PlainTextBody(k)) }.toList: _*))

    req.recover {
      case e: Exception =>
        println(s"HttpRequest2: post() to ${serverUrl()}/${apiPrefix()}/$apiEndPoint failed: ${e.getMessage}")
        afterException()
    }

    req.onComplete({ onComplete(_) })
  }

  def post(plainTextBody: String): Unit = {
    val req = createRequest()
      .withMethod(Method.POST) // GET is default
      .post(PlainTextBody(plainTextBody))

    req.recover {
      case e: Exception =>
        println(s"HttpRequest2: post() to ${serverUrl()}/${apiPrefix()}/$apiEndPoint failed: ${e.getMessage}")
        afterException()
    }

    req.onComplete({ onComplete(_) })
  }

}
