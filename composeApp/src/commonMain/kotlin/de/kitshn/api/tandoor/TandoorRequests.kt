package de.kitshn.api.tandoor

import co.touchlab.kermit.Logger
import coil3.network.NetworkResponse
import de.kitshn.json
import de.kitshn.redactForRelease
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class TandoorRequestsError(
    val response: HttpResponse?,
) : Exception() {
    override val message: String
        get() = response?.request?.url.toString() + " | " + response?.request?.method.toString() + " | " + response?.status.toString()
}

suspend fun TandoorClient.reqAny(
    endpoint: String,
    _method: HttpMethod,
    data: Any? = null,
    contentType: ContentType? = null,
    custom: HttpRequestBuilder.() -> Unit = { }
): HttpResponse {
    try {
        val token = this.credentials.token

        Logger.d("TandoorRequests") { "Method: $_method, URL: ${credentials.instanceUrl.redactForRelease()}/api${endpoint}" }

        val url = if(endpoint.startsWith("@")) {
            "${credentials.instanceUrl}/${endpoint.substring(1)}"
        } else {
            "${credentials.instanceUrl}/api${endpoint}"
        }

        val response = httpClient.request {
            url(url)
            method = _method
            headers {
                set("Authorization", "Bearer ${token?.token ?: ""}")
            }
            if(data != null && contentType != null) {
                setBody(data.toString())
                contentType(contentType)
            }

            custom()
        }

        if(!response.status.isSuccess())
            throw TandoorRequestsError(response)

        return response
    }catch(e: TandoorRequestsError) {
        throw e
    }catch(e: Exception) {
        e.printStackTrace()
        throw TandoorRequestsError(null)
    }
}

suspend fun TandoorClient.req(
    endpoint: String,
    _method: HttpMethod,
    data: JsonObject? = null
): HttpResponse {
    return reqAny(endpoint, _method, data?.toString(), ContentType.Application.Json)
}

suspend fun TandoorClient.reqArray(
    endpoint: String,
    method: HttpMethod,
    data: JsonArray? = null
): JsonArray {
    return json.decodeFromString(reqAny(endpoint, method, data?.toString(), ContentType.Application.Json).readRawBytes().decodeToString())
}

suspend fun TandoorClient.reqObject(
    endpoint: String,
    method: HttpMethod,
    data: JsonObject? = null
): JsonObject {
    return json.decodeFromString(reqAny(endpoint, method, data?.toString(), ContentType.Application.Json).readRawBytes().decodeToString())
}

suspend fun TandoorClient.reqByteArray(
    endpoint: String,
    method: HttpMethod,
    data: JsonObject? = null
): ByteArray {
    return reqAny(endpoint, method, data?.toString(), ContentType.Application.Json).readRawBytes()
}

suspend fun TandoorClient.reqMultipart(
    endpoint: String,
    _method: HttpMethod,
    _formData: FormBuilder.() -> Unit
): HttpResponse {
    try {
        val token = this.credentials.token

        Logger.d("TandoorRequests") { "Method: $_method, URL: ${credentials.instanceUrl.redactForRelease()}/api${endpoint}" }

        val url = if(endpoint.startsWith("@")) {
            "${credentials.instanceUrl}/${endpoint.substring(1)}"
        } else {
            "${credentials.instanceUrl}/api${endpoint}"
        }

        val response = httpClient.request {
            url(url)
            method = _method
            headers {
                set("Authorization", "Bearer ${token?.token ?: ""}")
            }

            setBody(MultiPartFormDataContent(
                formData(_formData)
            ))
        }

        if(!response.status.isSuccess())
            throw TandoorRequestsError(response)

        return response
    }catch(e: TandoorRequestsError) {
        throw e
    }catch(e: Exception) {
        e.printStackTrace()
        throw TandoorRequestsError(null)
    }
}

/*

@Throws(TandoorRequestsError::class)
suspend fun TandoorClient.reqMultipart(
    endpoint: String,
    method: Int,
    data: MutableMap<String, String>
) = suspendCoroutine { cont ->
    val queue = Volley.newRequestQueue(context)
    val token = this.credentials.token

    Log.d(
        "TandoorRequests",
        "Method: $method, URL: ${credentials.instanceUrl.redactForRelease()}/api${endpoint}"
    )
    val url = "${credentials.instanceUrl}/api${endpoint}"

    val request = object : VolleyMultipartRequest(
        method,
        url,
        { response: NetworkResponse? ->
            cont.resume(response)
        },
        { error: VolleyError? ->
            Log.e("TandoorRequests", "Exception for request \"$endpoint\" ($method) (jsonObject)")
            error?.printStackTrace()
            cont.resumeWithException(
                TandoorRequestsError(
                    error,
                    method,
                    "${credentials.instanceUrl.redactForRelease()}/api${endpoint}"
                )
            )
        }
    ) {
        override fun getHeaders(): MutableMap<String, String> {
            val headers = super.getHeaders().toMutableMap()
            headers["Authorization"] = "Bearer ${token?.token ?: ""}"
            return headers
        }

        override fun getParams(): MutableMap<String, String> {
            return data
        }
    }

    queue.add(request)
}
*/

suspend fun TandoorClient.getObject(endpoint: String) = reqObject(endpoint, HttpMethod.Get)
suspend fun TandoorClient.getArray(endpoint: String) = reqArray(endpoint, HttpMethod.Get)
suspend fun TandoorClient.getByteArray(endpoint: String) = reqByteArray(endpoint, HttpMethod.Get)

suspend fun TandoorClient.postObject(endpoint: String, data: JsonObject) =
    reqObject(endpoint, HttpMethod.Post, data)

suspend fun TandoorClient.postArray(endpoint: String, data: JsonArray) =
    reqArray(endpoint, HttpMethod.Post, data)

suspend fun TandoorClient.put(endpoint: String, data: JsonObject) =
    req(endpoint, HttpMethod.Put, data)

suspend fun TandoorClient.putObject(endpoint: String, data: JsonObject) =
    reqObject(endpoint, HttpMethod.Put, data)

suspend fun TandoorClient.putArray(endpoint: String, data: JsonArray) =
    reqArray(endpoint, HttpMethod.Put, data)

suspend fun TandoorClient.putMultipart(endpoint: String, _formData: FormBuilder.() -> Unit) =
    reqMultipart(endpoint, HttpMethod.Put, _formData)

suspend fun TandoorClient.patchObject(endpoint: String, data: JsonObject) =
    reqObject(endpoint, HttpMethod.Patch, data)

suspend fun TandoorClient.patchArray(endpoint: String, data: JsonArray) =
    reqArray(endpoint, HttpMethod.Patch, data)

suspend fun TandoorClient.delete(endpoint: String) =
    req(endpoint, HttpMethod.Delete)