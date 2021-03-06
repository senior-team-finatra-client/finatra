package com.twitter.finatra.httpclientv2

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.service.{ResponseClassifier, RetryBudget, RetryFilter, RetryPolicy}
import com.twitter.finagle.stats.LoadedStatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.Try

object HttpClientBuilder {
  def create(): HttpClientBuilder = {
    createWithClient(Http.client)
  }

  def createWithClient(client: Http.Client): HttpClientBuilder = {
    new HttpClientBuilder(client)
  }
}


class HttpClientBuilder(client: Http.Client) {

  var hostname: String = ""
  var retryPolicy: Option[RetryPolicy[(Request, Try[Response])]] = None
  var budget: RetryBudget = RetryBudget()
  var policy: RetryPolicy[(Request, Try[Response])] = ???
  var defaultHeaders: Map[String, String] = Map()
  var sslHostname: String = ""
  var classifier: ResponseClassifier = ResponseClassifier.Default

  /**
    * Sets hostname
    * @param name hostname that will be used for host header
    */
  def withHostname(name: String): HttpClientBuilder = {
    this.hostname = hostname
    this
  }

  /**
    * Sets retry budget
    * @param retryBudget Amount of times that the client will retry on request
    * @return
    */
  def withRetryBudget(retryBudget: RetryBudget): HttpClientBuilder = {
    budget = retryBudget
    this
  }

  /**
    * Sets retry policy
    * @param policy optional retry policy if service fails to get succesful response
    */
  def withRetryPolicy(policy: RetryPolicy[(Request,Try[Response])]): HttpClientBuilder = {
    this.retryPolicy = Some(policy)
    this.policy = policy
    this
  }

  /**
    * Sets default headers
    * @param defaultHeaders default headers that sent to server
    */
  def withDefaultHeaders(defaultHeaders: Map[String, String] = Map()): HttpClientBuilder = {
    this.defaultHeaders = defaultHeaders
    this
  }

  /**
    * Sets HTTPs hostname
    * @param sslHostname host name if using HTTPS
    */
  def withTls(sslHostname: String): HttpClientBuilder = {
    this.sslHostname = sslHostname
    this
  }

  /**
    * Sets response classifier
    * @param classifier optional classifier that can be set to handle certain HTTP status codes
    */
  def withResponseClassifier(classifier: ResponseClassifier): HttpClientBuilder = {
    this.classifier = classifier
    this
  }

  def newClient(dest: String): HttpClient = {
    val service = buildService(dest)

    new HttpClient(hostname = hostname, httpService = service, retryPolicy = retryPolicy, defaultHeaders = defaultHeaders)
  }

  private def buildService(dest: String): Service[Request, Response] ={
    var configuredClient = client
    configuredClient = configuredClient
      .withRetryBudget(budget)
      .withResponseClassifier(classifier)

    if (sslHostname.nonEmpty) {
      configuredClient = configuredClient.withTls(sslHostname)
    }
    val service = configuredClient.newService(dest)
    val filteredService = setFilteredService(service)
    filteredService
  }

  private def setFilteredService(service: Service[Request, Response]): Service[Request, Response] ={
    val filteredService = retryPolicy match {
      case Some(policy) =>
        new RetryFilter(
          retryPolicy = policy,
          timer = DefaultTimer,
          statsReceiver = LoadedStatsReceiver,
          retryBudget = budget).andThen(service)
      case _ =>
        service
    }
    filteredService
  }
}
