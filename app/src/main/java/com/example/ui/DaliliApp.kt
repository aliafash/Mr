package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.AppConfig
import com.example.data.Category
import com.example.data.ServiceProvider

// Simple State-Based Route definitions
sealed class Screen {
    object Splash : Screen()
    object UserHome : Screen()
    data class ProviderList(val category: Category) : Screen()
    object AdminPanel : Screen()
}

@Composable
fun DaliliApp(viewModel: MainViewModel) {
    // Force RTL direction for complete Arabic support
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
        val config by viewModel.appConfig.collectAsState()
        val errorMsg by viewModel.errorMessage.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val connState by viewModel.connectionState.collectAsState()
        val context = LocalContext.current

        // Dynamic colors resolved from config
        val primaryColor = parseHexColor(config.primaryColor, Color.Black)
        val secondaryColor = parseHexColor(config.secondaryColor, Color(0xFFD700))

        // Toast effect for errors
        LaunchedEffect(errorMsg) {
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is Screen.Splash -> {
                    SplashScreen(
                        config = config,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    ) {
                        currentScreen = Screen.UserHome
                    }
                }
                is Screen.UserHome -> {
                    UserHomeScreen(
                        viewModel = viewModel,
                        config = config,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        onCategorySelect = { cat ->
                            currentScreen = Screen.ProviderList(cat)
                        },
                        onOpenAdmin = {
                            currentScreen = Screen.AdminPanel
                        }
                    )
                }
                is Screen.ProviderList -> {
                    ProviderListScreen(
                        category = screen.category,
                        viewModel = viewModel,
                        config = config,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        onBack = {
                            currentScreen = Screen.UserHome
                        }
                    )
                }
                is Screen.AdminPanel -> {
                    AdminPanelScreen(
                        viewModel = viewModel,
                        config = config,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        onLogout = {
                            viewModel.logout()
                            currentScreen = Screen.UserHome
                        }
                    )
                }
            }

            // Connection State Banner overlay at top
            AnimatedVisibility(
                visible = connState != null,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
            ) {
                connState?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = secondaryColor),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(horizontal = 16.dp).border(1.dp, primaryColor, RoundedCornerShape(20.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                color = primaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }

            // Generic action Loading Indicator
            if (isLoading && connState == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = secondaryColor)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SPLASH SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun SplashScreen(
    config: AppConfig,
    primaryColor: Color,
    secondaryColor: Color,
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(primaryColor, Color(0xFF1C1C1C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App adaptive branding logo mock
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(3.dp, secondaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (config.appIcon.isNotEmpty()) {
                    AsyncImage(
                        model = config.appIcon,
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "خدمات",
                        color = secondaryColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = config.appName,
                color = secondaryColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "دليلك الشامل لجميع خدمات اليمن",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// USER HOME SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    viewModel: MainViewModel,
    config: AppConfig,
    primaryColor: Color,
    secondaryColor: Color,
    onCategorySelect: (Category) -> Unit,
    onOpenAdmin: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val providers by viewModel.serviceProviders.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var logoClickCount by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن الأقسام أو مقدمي الخدمات...", color = Color.Gray) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = secondaryColor,
                                cursorColor = primaryColor
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("home_search_input")
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    logoClickCount++
                                    if (logoClickCount >= 5) {
                                        logoClickCount = 0
                                        showLoginDialog = true
                                    }
                                }
                                .testTag("home_logo_title_click")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(1.dp, secondaryColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (config.appIcon.isNotEmpty()) {
                                    AsyncImage(
                                        model = config.appIcon,
                                        contentDescription = "Logo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = "خ",
                                        color = secondaryColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = config.appName,
                                color = secondaryColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                },
                actions = {
                    // Search Switch Icon
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = secondaryColor
                        )
                    }

                    // Profile / Admin Panel Access button (Only visible if logged in!)
                    if (currentUser != null) {
                        IconButton(
                            onClick = {
                                onOpenAdmin()
                            },
                            modifier = Modifier.testTag("admin_portal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "بوابة الإدارة",
                                tint = secondaryColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // Main Content Area
            if (isSearchActive && searchQuery.trim().isNotEmpty()) {
                // Search Mode displays filtered results:
                val filteredProviders = providers.filter {
                    it.isActive && (it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery))
                }
                val filteredCategories = categories.filter {
                    it.nameAr.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                    if (filteredCategories.isNotEmpty()) {
                        item {
                            Text(
                                text = "الأقسام المطابقة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(filteredCategories) { cat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onCategorySelect(cat) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                border = BoxBorder(Color.LightGray)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.orderIndex ?: 0),
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = cat.nameAr,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                            }
                        }
                    }

                    if (filteredProviders.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "مقدمو الخدمات المطابقون",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(filteredProviders) { provider ->
                            val cat = categories.firstOrNull { it.id == provider.categoryId }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        cat?.let { onCategorySelect(it) }
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = provider.name.take(1),
                                            color = secondaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(text = provider.name, fontWeight = FontWeight.Bold, color = primaryColor)
                                        Text(text = "رقم الهاتف: ${provider.phone}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    if (filteredCategories.isEmpty() && filteredProviders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "لم يتم العثور على نتائج!", color = Color.Gray)
                            }
                        }
                    }
                }
            } else {
                // Regular view of categories Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).padding(12.dp)
                ) {
                    items(categories) { cat ->
                        CategoryGridCard(
                            category = cat,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        ) {
                            onCategorySelect(cat)
                        }
                    }
                }
            }

            // Promotional Interactive Footer
            InteractiveFooter(
                config = config,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )
        }
    }

    // Login Modal dialog
    if (showLoginDialog) {
        LoginDialog(
            viewModel = viewModel,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            onDismiss = { showLoginDialog = false }
        ) {
            showLoginDialog = false
            onOpenAdmin()
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CATEGORY CARD COMPONENT
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun CategoryGridCard(
    category: Category,
    primaryColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .testTag("category_card_${category.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BoxBorder(Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.orderIndex ?: 0),
                    contentDescription = category.nameAr,
                    tint = secondaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = category.nameAr,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Map index to elegant default icons
fun getCategoryIcon(index: Int): ImageVector {
    return when (index) {
        1 -> Icons.Default.Build
        2 -> Icons.Default.Computer
        3 -> Icons.Default.School
        4 -> Icons.Default.Face
        5 -> Icons.Default.DirectionsCar
        6 -> Icons.Default.Home
        7 -> Icons.Default.LocalShipping
        8 -> Icons.Default.Work
        else -> Icons.Default.Star
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LOGIN DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun LoginDialog(
    viewModel: MainViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "بوابة تسجيل الدخول",
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم", color = primaryColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("login_username_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور", color = primaryColor) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.login(username, password) { success ->
                        if (success) onSuccess()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor),
                modifier = Modifier.fillMaxWidth().testTag("login_submit_button")
            ) {
                Text("تسجيل الدخول")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إلغاء", color = primaryColor)
            }
        },
        containerColor = Color.White
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PROVIDER LIST SCREEN (USER)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    category: Category,
    viewModel: MainViewModel,
    config: AppConfig,
    primaryColor: Color,
    secondaryColor: Color,
    onBack: () -> Unit
) {
    val providers by viewModel.serviceProviders.collectAsState()
    val reviews by viewModel.reviews.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeReviewsProviderId by remember { mutableStateOf<Int?>(null) }
    var reviewComposerProviderId by remember { mutableStateOf<Int?>(null) }

    // Filter providers inside category
    val currentProviders = providers.filter {
        it.categoryId == category.id && it.isActive &&
                (searchQuery.trim().isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category.nameAr,
                        color = secondaryColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع", tint = secondaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // Search Input Block
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في مقدمي خدمات ${category.nameAr}...", color = Color.Gray) },
                singleLine = true,
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = primaryColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = secondaryColor,
                    focusedLabelColor = secondaryColor,
                    cursorColor = primaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("providers_search_input")
            )

            // Providers List
            if (currentProviders.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "لا يوجد مقدمو خدمات متاحين حالياً في هذا القسم", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentProviders) { provider ->
                        val countReviews = reviews.count { it.providerId == provider.id }
                        ProviderCardItem(
                            provider = provider,
                            countReviews = countReviews,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            onViewReviews = { activeReviewsProviderId = provider.id },
                            onAddReview = { reviewComposerProviderId = provider.id }
                        )
                    }
                }
            }

            // Footer Campaign
            InteractiveFooter(
                config = config,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )
        }
    }

    // Modal Sheet or Dialog: View reviews for specific provider
    activeReviewsProviderId?.let { id ->
        val provider = providers.firstOrNull { it.id == id }
        val providerReviews = reviews.filter { it.providerId == id }
        ReviewsListDialog(
            providerName = provider?.name ?: "",
            reviews = providerReviews,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            onDismiss = { activeReviewsProviderId = null }
        )
    }

    // Modal dialog: compose a review
    reviewComposerProviderId?.let { id ->
        ReviewComposeDialog(
            providerId = id,
            viewModel = viewModel,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            onDismiss = { reviewComposerProviderId = null }
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PROVIDER CARD LIST ITEM COMPONENT
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun ProviderCardItem(
    provider: ServiceProvider,
    countReviews: Int,
    primaryColor: Color,
    secondaryColor: Color,
    onViewReviews: () -> Unit,
    onAddReview: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BoxBorder(Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Async avatar or placeholder
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!provider.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = provider.imageUrl,
                            contentDescription = provider.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = provider.name.take(1),
                            color = secondaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "هاتف: ${provider.phone}",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Stars Review Summary Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val roundedRating = String.format("%.1f", provider.rating ?: 0.0)
                        Text(
                            text = "$roundedRating ($countReviews تقييمات)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons row: Call, WhatsApp, View reviews, Add Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // CALL BUTTON
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("provider_call_button"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // WHATSAPP BUTTON (Green accent background)
                Button(
                    onClick = {
                        val url = "https://api.whatsapp.com/send?phone=${cleanPhoneNumber(provider.phone)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("provider_whatsapp_button"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // VIEW REVIEWS
                OutlinedButton(
                    onClick = onViewReviews,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                    modifier = Modifier.weight(1f).testTag("provider_reviews_button"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("التعليقات", fontSize = 11.sp)
                }

                // ADD REVIEW
                TextButton(
                    onClick = onAddReview,
                    modifier = Modifier.testTag("provider_rate_button")
                ) {
                    Text("قيمنا", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Clean phone format for WhatsApp api
fun cleanPhoneNumber(phone: String): String {
    val cleanStr = phone.replace("+", "").replace(" ", "").trim()
    if (cleanStr.startsWith("0")) {
        // Yemeni locale prefix fallback
        return "967" + cleanStr.substring(1)
    }
    if (!cleanStr.startsWith("967") && cleanStr.length == 9) {
        return "967$cleanStr"
    }
    return cleanStr
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// VIEW REVIEWS DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun ReviewsListDialog(
    providerName: String,
    reviews: List<com.example.data.Review>,
    primaryColor: Color,
    secondaryColor: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().height(450.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BoxBorder(Color(0xFFEEEEEE))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "تعليقات لـ $providerName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (reviews.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "لا توجد تعليقات لمقدم الخدمة هذا بعد", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reviews) { r ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Masked phone format (partial obscurement)
                                        val maskedPhone = r.userPhone.take(3) + "***" + r.userPhone.takeLast(3)
                                        Text(text = maskedPhone, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = primaryColor)
                                        
                                        Row {
                                            repeat(r.rating) {
                                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = r.comment ?: "بدون تعليق",
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق")
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ADD REVIEW (COMPOSE) DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun ReviewComposeDialog(
    providerId: Int,
    viewModel: MainViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var reviewerPhone by remember { mutableStateOf("") }
    var reviewerComment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BoxBorder(Color(0xFFEEEEEE))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "إضافة تقييم ومراجعة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Starts Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (i <= rating) secondaryColor else Color.LightGray,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = i }
                                .testTag("star_rate_$i")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reviewerPhone,
                    onValueChange = { reviewerPhone = it },
                    label = { Text("رقم الهاتف (ضروري للتحقق)", color = primaryColor) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reviewer_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reviewerComment,
                    onValueChange = { reviewerComment = it },
                    label = { Text("اكتب تعليقك (اختياري)", color = primaryColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth().height(90.dp).testTag("reviewer_comment_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (reviewerPhone.trim().isEmpty()) {
                            Toast.makeText(viewModel.errorMessage.value?.let { null } ?: viewModel.hashCode().toString().let { viewModel::class.java.simpleName.let { "ملاحظة: رقم الهاتف ضروري!" } }, "يرجى كتابة رقم الهاتف للتحقق!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addReview(providerId, reviewerPhone, rating, reviewerComment) { success ->
                            if (success) onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor),
                    modifier = Modifier.fillMaxWidth().testTag("submit_review_button")
                ) {
                    Text("حفظ التقييم")
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إلغاء", color = primaryColor)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LOGGED IN ADMIN PANEL SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: MainViewModel,
    config: AppConfig,
    primaryColor: Color,
    secondaryColor: Color,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val currentUser by viewModel.currentUser.collectAsState()

    // Setup active tabs based on SuperAdmin/Admin permissions
    val isSuper = currentUser?.role == "super_admin"
    val tabsList = if (isSuper) {
        listOf("الأقسام", "مقدمو الخدمات", "المشرفون", "الإعدادات")
    } else {
        listOf("الأقسام", "مقدمو الخدمات")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "لوحة التحكم | " + (currentUser?.username ?: "مشرف"),
                        color = secondaryColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "خروج من الإدارة", tint = secondaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = primaryColor
            ) {
                tabsList.forEachIndexed { index, tabTitle ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            val icon = when (tabTitle) {
                                "الأقسام" -> Icons.Default.Category
                                "مقدمو الخدمات" -> Icons.Default.Business
                                "المشرفون" -> Icons.Default.SupervisorAccount
                                "الإعدادات" -> Icons.Default.Settings
                                else -> Icons.Default.Build
                            }
                            Icon(imageVector = icon, contentDescription = tabTitle)
                        },
                        label = { Text(tabTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryColor,
                            selectedTextColor = secondaryColor,
                            indicatorColor = secondaryColor,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (tabsList[selectedTab]) {
                "الأقسام" -> AdminCategoriesTab(viewModel, primaryColor, secondaryColor)
                "مقدمو الخدمات" -> AdminProvidersTab(viewModel, primaryColor, secondaryColor)
                "المشرفون" -> AdminAccountsTab(viewModel, primaryColor, secondaryColor)
                "الإعدادات" -> AdminSettingsTab(viewModel, config, primaryColor, secondaryColor)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ADMIN CATEGORIES TAB COMPONENT
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun AdminCategoriesTab(viewModel: MainViewModel, primaryColor: Color, secondaryColor: Color) {
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTargetCategory by remember { mutableStateOf<Category?>(null) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "إدارة تصنيفات الدليل", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor)
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة قسم")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = getCategoryIcon(cat.orderIndex ?: 0), contentDescription = null, tint = secondaryColor)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = cat.nameAr, fontWeight = FontWeight.Bold, color = primaryColor)
                                    Text(text = "رقم الترتيب: ${cat.orderIndex}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Row {
                                IconButton(onClick = { editTargetCategory = cat }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = Color.Blue)
                                }
                                IconButton(onClick = { viewModel.removeCategory(cat.id ?: 0) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        var nameAr by remember { mutableStateOf("") }
        var orderIndexStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة قسم جديد") },
            text = {
                Column {
                    OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("اسم القسم بالعربية") })
                    OutlinedTextField(value = orderIndexStr, onValueChange = { orderIndexStr = it }, label = { Text("ترتيب الظهور (رقم)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = orderIndexStr.toIntOrNull() ?: 0
                        viewModel.addCategory(nameAr, "", index)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor)
                ) {
                    Text("إرسال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Edit dialog
    editTargetCategory?.let { target ->
        var nameAr by remember { mutableStateOf(target.nameAr) }
        var orderIndexStr by remember { mutableStateOf((target.orderIndex ?: 0).toString()) }
        AlertDialog(
            onDismissRequest = { editTargetCategory = null },
            title = { Text("تعديل القسم") },
            text = {
                Column {
                    OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("اسم القسم بالعربية") })
                    OutlinedTextField(value = orderIndexStr, onValueChange = { orderIndexStr = it }, label = { Text("ترتيب الظهور (رقم)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = orderIndexStr.toIntOrNull() ?: 0
                        viewModel.editCategory(target.id ?: 0, nameAr, target.icon, index)
                        editTargetCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { editTargetCategory = null }) { Text("إلغاء") }
            }
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ADMIN PROVIDERS TAB COMPONENT
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun AdminProvidersTab(viewModel: MainViewModel, primaryColor: Color, secondaryColor: Color) {
    val providers by viewModel.serviceProviders.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editTargetProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "إدارة مقدمي الخدمات", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor)
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة موفر")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(providers) { provider ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = provider.name, fontWeight = FontWeight.Bold, color = primaryColor)
                                Text(text = "رقم الهاتف: ${provider.phone}", fontSize = 12.sp, color = Color.Gray)
                                val assocCatName = categories.firstOrNull { it.id == provider.categoryId }?.nameAr ?: "غير محدد"
                                Text(text = "القسم: $assocCatName", fontSize = 12.sp, color = secondaryColor, fontWeight = FontWeight.Bold)
                            }

                            Row {
                                IconButton(onClick = { editTargetProvider = provider }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = Color.Blue)
                                }
                                IconButton(onClick = { viewModel.removeServiceProvider(provider.id ?: 0) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0) }
        var imageUrl by remember { mutableStateOf("") }
        var isActive by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة موفر خدمة جديد") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") })
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("رابط الصورة (اختياري)") })
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("اختر القسم:")
                    // Category choose options (simple picker)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { c ->
                            Card(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedCatId = c.id ?: 0 },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCatId == c.id) secondaryColor else Color(0xFFEEEEEE)
                                )
                            ) {
                                Text(text = c.nameAr, fontSize = 10.sp, modifier = Modifier.padding(6.dp), color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                        Text("نشط ومتاح للجميع")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addServiceProvider(name, phone, selectedCatId, imageUrl, isActive)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor)
                ) {
                    Text("إرسال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Edit target provider dialog
    editTargetProvider?.let { provider ->
        var name by remember { mutableStateOf(provider.name) }
        var phone by remember { mutableStateOf(provider.phone) }
        var selectedCatId by remember { mutableStateOf(provider.categoryId) }
        var imageUrl by remember { mutableStateOf(provider.imageUrl ?: "") }
        var isActive by remember { mutableStateOf(provider.isActive) }

        AlertDialog(
            onDismissRequest = { editTargetProvider = null },
            title = { Text("تعديل موفر الخدمة") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") })
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("رابط الصورة (اختياري)") })
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("القسم:")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { c ->
                            Card(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedCatId = c.id ?: 0 },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCatId == c.id) secondaryColor else Color(0xFFEEEEEE)
                                )
                            ) {
                                Text(text = c.nameAr, fontSize = 10.sp, modifier = Modifier.padding(6.dp), color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                        Text("نشط ومتاح للجميع")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.editServiceProvider(
                            provider.id ?: 0,
                            name,
                            phone,
                            selectedCatId,
                            provider.rating ?: 0.0,
                            imageUrl,
                            isActive
                        )
                        editTargetProvider = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { editTargetProvider = null }) { Text("إلغاء") }
            }
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ADMIN MANAGING OTHER ACCOUNTS (SUPER ADMIN ONLY)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun AdminAccountsTab(viewModel: MainViewModel, primaryColor: Color, secondaryColor: Color) {
    val adminsList by viewModel.admins.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "إدارة حسابات المشرفين", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor)
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشرف جديد")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(adminsList) { adm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "رابط المشرف: ${adm.username}", fontWeight = FontWeight.Bold, color = primaryColor)
                                Text(text = "الرتبة: ${adm.role}", fontSize = 12.sp, color = secondaryColor, fontWeight = FontWeight.Bold)
                            }

                            if (adm.username != "admin") {
                                IconButton(onClick = { viewModel.removeAdmin(adm.id ?: 0) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة حساب مشرف جديد") },
            text = {
                Column {
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") })
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addAdmin(username, password)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor)
                ) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ADMIN SECRET OPTIONS & THEME SETTINGS TAB
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun AdminSettingsTab(
    viewModel: MainViewModel,
    config: AppConfig,
    primaryColor: Color,
    secondaryColor: Color
) {
    var appName by remember { mutableStateOf(config.appName) }
    var primaryColorHex by remember { mutableStateOf(config.primaryColor) }
    var secondaryColorHex by remember { mutableStateOf(config.secondaryColor) }
    var appIcon by remember { mutableStateOf(config.appIcon) }
    var footerText by remember { mutableStateOf(config.footerText) }
    var adminPassword by remember { mutableStateOf(config.adminPassword) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "الإعدادات السرية والمظهر", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it },
                label = { Text("اسم التطبيق الرئيسي") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = primaryColorHex,
                onValueChange = { primaryColorHex = it },
                label = { Text("اللون الأساسي (Hex Code)") },
                modifier = Modifier.fillMaxWidth()
            )
            // Color Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseHexColor(primaryColorHex, Color.Black))
            )
        }

        item {
            OutlinedTextField(
                value = secondaryColorHex,
                onValueChange = { secondaryColorHex = it },
                label = { Text("اللون الثانوي والرموز (Hex Code)") },
                modifier = Modifier.fillMaxWidth()
            )
            // Color Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseHexColor(secondaryColorHex, Color(0xFFD700)))
            )
        }

        item {
            OutlinedTextField(
                value = appIcon,
                onValueChange = { appIcon = it },
                label = { Text("رابط أيقونة/شعار التطبيق (صورة)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = footerText,
                onValueChange = { footerText = it },
                label = { Text("التذييل الدعائي أسفل الشاشات") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = adminPassword,
                onValueChange = { adminPassword = it },
                label = { Text("كلمة المرور الرئيسية للمالك (admin)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    val updated = AppConfig(
                        appName = appName,
                        primaryColor = primaryColorHex,
                        secondaryColor = secondaryColorHex,
                        appIcon = appIcon,
                        footerText = footerText,
                        adminPassword = adminPassword
                    )
                    viewModel.updateConfig(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor),
                modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
            ) {
                Text("حفظ التغييرات ونشرها فوراً", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Utility class to parse hex color safely
fun parseHexColor(hexString: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexString.trim()))
    } catch (e: Exception) {
        fallback
    }
}

fun BoxBorder(color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, color)
}

@Composable
fun InteractiveFooter(
    config: AppConfig,
    primaryColor: Color,
    secondaryColor: Color
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = secondaryColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = config.footerText,
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showDialog) {
        val phones = extractPhoneNumbers(config.footerText).toMutableList()
        // Ensure default 777644670 is supported if not explicitly extracted
        if (phones.isEmpty() && config.footerText.contains("777644670")) {
            phones.add("777644670")
        }
        val urls = extractUrls(config.footerText)

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "دعم التطبيق والاتصال",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "يمكنك النقر على أي من خيارات الاتصال أو الروابط التالية للتواصل مباشرة:",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Display all detected phone numbers
                    if (phones.isNotEmpty()) {
                        Text(
                            text = "أرقام الهواتف المتاحة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                        phones.distinct().forEach { phone ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BoxBorder(Color(0xFFEEEEEE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = phone,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = primaryColor,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Call
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Phone, contentDescription = "اتصال", tint = primaryColor)
                                        }
                                        // WhatsApp
                                        IconButton(
                                            onClick = {
                                                val url = "https://api.whatsapp.com/send?phone=${cleanPhoneNumber(phone)}"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Message, contentDescription = "واتساب", tint = Color(0xFF25D366))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Display all detected URL links
                    if (urls.isNotEmpty()) {
                        Text(
                            text = "الروابط والمواقع الإلكترونية المتوفرة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                        urls.distinct().forEach { url ->
                            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                "https://$url"
                            } else url
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                                    context.startActivity(intent)
                                },
                                border = BoxBorder(Color(0xFFEEEEEE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(imageVector = Icons.Default.Language, contentDescription = "رابط", tint = secondaryColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = url,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = Color.Blue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Standard developer support block as fallback
                    if (phones.isEmpty() && urls.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BoxBorder(Color(0xFFEEEEEE))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "تواصل مع الدعم الفني: 777644670",
                                    fontSize = 13.sp,
                                    color = primaryColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:777644670"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Phone, contentDescription = "اتصال", tint = primaryColor)
                                    }
                                    IconButton(
                                        onClick = {
                                            val url = "https://api.whatsapp.com/send?phone=96777644670"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Message, contentDescription = "واتساب", tint = Color(0xFF25D366))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = secondaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق")
                }
            },
            containerColor = Color.White
        )
    }
}

fun extractPhoneNumbers(text: String): List<String> {
    val regex = Regex("\\b\\+?[0-9]{7,15}\\b")
    return regex.findAll(text).map { it.value }.toList()
}

fun extractUrls(text: String): List<String> {
    val regex = Regex("\\b(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?|www\\.[\\w-]+(\\.[\\w-]+)+(/\\S*)?)\\b")
    return regex.findAll(text).map { it.value }.toList()
}
