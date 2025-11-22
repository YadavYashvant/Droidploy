package com.example.droidploy.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CloudflareApi {
    @POST("accounts/{accountId}/cfd_tunnel")
    suspend fun createTunnel(
        @Header("Authorization") token: String,
        @Path("accountId") accountId: String,
        @Body body: CreateTunnelRequest
    ): CreateTunnelResponse

    @PUT("accounts/{accountId}/cfd_tunnel/{tunnelId}/configurations")
    suspend fun configureTunnel(
        @Header("Authorization") token: String,
        @Path("accountId") accountId: String,
        @Path("tunnelId") tunnelId: String,
        @Body body: ConfigureTunnelRequest
    ): Any

    @POST("zones/{zoneId}/dns_records")
    suspend fun createDnsRecord(
        @Header("Authorization") token: String,
        @Path("zoneId") zoneId: String,
        @Body body: CreateDnsRecordRequest
    ): Any
}

data class CreateTunnelRequest(val name: String, val config_src: String = "cloudflare")
data class CreateTunnelResponse(val result: TunnelResult)
data class TunnelResult(val id: String, val token: String)

data class ConfigureTunnelRequest(val config: TunnelConfig)
data class TunnelConfig(val ingress: List<IngressRule>)
data class IngressRule(val hostname: String? = null, val service: String)

data class CreateDnsRecordRequest(
    val type: String = "CNAME",
    val name: String,
    val content: String,
    val proxied: Boolean = true
)

object CloudflareManager {
    private const val BASE_URL = "https://api.cloudflare.com/client/v4/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    val api: CloudflareApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CloudflareApi::class.java)
}
