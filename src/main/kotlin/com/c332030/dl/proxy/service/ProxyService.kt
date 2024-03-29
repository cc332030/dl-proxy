package com.c332030.dl.proxy.service

import com.c332030.CLogger
import com.c332030.service.CAbstractSpringService
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.io.FilenameUtils
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
class ProxyService(
  private val request: HttpServletRequest
  , private val response: HttpServletResponse
  , private val okHttpClient: OkHttpClient
): CAbstractSpringService() {

  private val log = CLogger(this.javaClass)

  fun proxy(url: String) {

    log.info { "url : $url" }

    val okHeadersBuilder = Headers.Builder()
    request.headerNames.asIterator().forEachRemaining { headerName ->
      if (!HttpHeaders.HOST.equals(headerName, ignoreCase = false)) {
        okHeadersBuilder[headerName] = request.getHeader(headerName)
      }
    }

    val okRequest = Request.Builder().get()
      .url(url)
      .headers(okHeadersBuilder.build())
      .build()

    val okResponse = okHttpClient.newCall(okRequest).execute()
    response.status = okResponse.code

    updateHeaders(okResponse, url)

    val inputStream = okResponse.body?.byteStream()
    inputStream?.use {
      inputStream.transferTo(response.outputStream)
    }
  }

  fun updateHeaders(okResponse: Response, url: String) {

    var contentDisposition = ""
    okResponse.headers.iterator().forEachRemaining { pair ->

      val key = pair.first
      val value = pair.second

      if (HttpHeaders.CONTENT_DISPOSITION == key) {
        contentDisposition = value
        return@forEachRemaining
      }
      response.setHeader(key, value)
    }
    if (contentDisposition.isEmpty() || "attachment" != contentDisposition) {
      val attachment = "attachment; filename=\"${FilenameUtils.getName(url)}\""
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION, attachment)
    }
  }

}
