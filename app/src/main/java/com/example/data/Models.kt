package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Admin(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "username") val username: String,
    @Json(name = "password_hash") val passwordHash: String,
    @Json(name = "role") val role: String, // super_admin, admin, config
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Category(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name_ar") val nameAr: String,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "order_index") val orderIndex: Int? = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ServiceProvider(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "category_id") val categoryId: Int,
    @Json(name = "rating") val rating: Double? = 0.0,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Review(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "provider_id") val providerId: Int,
    @Json(name = "user_phone") val userPhone: String,
    @Json(name = "rating") val rating: Int,
    @Json(name = "comment") val comment: String? = "",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AppConfig(
    @Json(name = "app_name") val appName: String = "دليلي - Dalili",
    @Json(name = "primary_color") val primaryColor: String = "#000000",
    @Json(name = "secondary_color") val secondaryColor: String = "#FFD700",
    @Json(name = "app_icon") val appIcon: String = "", // Base64 or URL
    @Json(name = "footer_text") val footerText: String = "MAW 777644670",
    @Json(name = "admin_password") val adminPassword: String = "maher736462"
)
