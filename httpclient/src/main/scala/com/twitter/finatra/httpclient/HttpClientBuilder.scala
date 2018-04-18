package com.twitter.finatra.httpclient

import com.twitter.finagle.Http

class HttpClientBuilder {

  var client: Http.Client

  def newBuilder: Http.Client = {
    client = Http.client.withLoadBalancer =
    client
  }

  def build(dest: String): HttpClient = {
    val service = client.newService(dest)
    new HttpClient(httpService = service)
  }

}
