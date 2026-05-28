package com.example.data

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

const val SUPABASE_URL = "https://sazbudkuzxbvmuztaxeg.supabase.co/rest/v1/"
const val SUPABASE_KEY = "sb_publishable_vvR8V-Y4Ge4-PMZa1AuFnQ_t9TJrwnx"

interface SupabaseApi {
    // Admins
    @GET("admins")
    suspend fun getAdmins(
        @Query("select") select: String = "*"
    ): List<Admin>

    @GET("admins")
    suspend fun getAdminByUsername(
        @Query("username") username: String, // String comparison needs "eq.username" in postgrest, but Query passes it as is or we format it. We'll pass "eq.username" directly.
        @Query("select") select: String = "*"
    ): List<Admin>

    @POST("admins")
    suspend fun insertAdmin(
        @Body admin: Admin
    ): List<Admin>

    @PATCH("admins")
    suspend fun updateAdmin(
        @Query("username") usernameFilter: String, // "eq.admin"
        @Body admin: Map<String, String>
    )

    @DELETE("admins")
    suspend fun deleteAdmin(
        @Query("id") idFilter: String // "eq.1"
    )

    // Categories
    @GET("categories")
    suspend fun getCategories(
        @Query("select") select: String = "*",
        @Query("order") order: String = "order_index.asc"
    ): List<Category>

    @POST("categories")
    suspend fun insertCategory(
        @Body category: Category
    ): List<Category>

    @PATCH("categories")
    suspend fun updateCategory(
        @Query("id") idFilter: String, // "eq.1"
        @Body category: Category
    )

    @DELETE("categories")
    suspend fun deleteCategory(
        @Query("id") idFilter: String // "eq.1"
    )

    // Service Providers
    @GET("service_providers")
    suspend fun getServiceProviders(
        @Query("select") select: String = "*",
        @Query("order") order: String = "name.asc"
    ): List<ServiceProvider>

    @GET("service_providers")
    suspend fun getServiceProvidersByCategory(
        @Query("category_id") categoryIdFilter: String, // "eq.1"
        @Query("select") select: String = "*",
        @Query("order") order: String = "name.asc"
    ): List<ServiceProvider>

    @POST("service_providers")
    suspend fun insertServiceProvider(
        @Body provider: ServiceProvider
    ): List<ServiceProvider>

    @PATCH("service_providers")
    suspend fun updateServiceProvider(
        @Query("id") idFilter: String, // "eq.1"
        @Body provider: ServiceProvider
    )

    @DELETE("service_providers")
    suspend fun deleteServiceProvider(
        @Query("id") idFilter: String // "eq.1"
    )

    // Reviews
    @GET("reviews")
    suspend fun getReviews(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<Review>

    @GET("reviews")
    suspend fun getReviewsForProvider(
        @Query("provider_id") providerIdFilter: String, // "eq.1"
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<Review>

    @POST("reviews")
    suspend fun insertReview(
        @Body review: Review
    ): List<Review>
}

object SupabaseClient {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer $SUPABASE_KEY")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: SupabaseApi = Retrofit.Builder()
        .baseUrl(SUPABASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(SupabaseApi::class.java)
}

class SupabaseRealtime(
    private val client: OkHttpClient,
    private val onMessageReceived: () -> Unit
) {
    private var webSocket: WebSocket? = null
    private var isDisposed = false

    fun connect() {
        if (isDisposed) return
        val request = Request.Builder()
            .url("wss://sazbudkuzxbvmuztaxeg.supabase.co/realtime/v1/websocket?apikey=$SUPABASE_KEY&vsn=1.0.0")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SupabaseRealtime", "Connected to Realtime WebSocket")
                // Join general public postgres changes channel
                val joinMessage = """{"topic":"realtime:public","event":"phx_join","payload":{},"ref":"1"}"""
                webSocket.send(joinMessage)
                
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SupabaseRealtime", "WebSocket msg: $text")
                // If anything postgres change occurs, request view model refresh
                if (text.contains("postgres_changes") || text.contains("INSERT") || text.contains("UPDATE") || text.contains("DELETE")) {
                    onMessageReceived()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SupabaseRealtime", "WebSocket Failed. Reconnecting in 5s...", t)
                if (!isDisposed) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        connect()
                    }, 5000)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SupabaseRealtime", "WebSocket Closed: $reason")
            }
        })
    }

    private fun startHeartbeat(socket: WebSocket) {
        val intervalMs = 25000L
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (isDisposed) return
                try {
                    val pingMessage = """{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"2"}"""
                    socket.send(pingMessage)
                    handler.postDelayed(this, intervalMs)
                } catch (e: Exception) {
                    Log.e("SupabaseRealtime", "Heartbeat send error", e)
                }
            }
        }
        handler.postDelayed(runnable, intervalMs)
    }

    fun dispose() {
        isDisposed = true
        webSocket?.close(1000, "Disposed")
    }
}
