package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiscateApp(viewModel: BiscateViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val biscateiros by viewModel.allBiscateiros.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Início, 1 = Biscatos, 2 = Diretório, 3 = Carteira, 4 = Comunidade

    // Community social network interactions states
    val likedTasks = remember { mutableStateMapOf<Int, Boolean>() }
    val likesCount = remember { mutableStateMapOf<Int, Int>() }

    // Seed initial likes count based on task IDs to make it feel alive!
    LaunchedEffect(tasks) {
        tasks.forEach { task ->
            if (!likesCount.containsKey(task.id)) {
                likesCount[task.id] = (task.id * 3) % 11 + 2
            }
        }
    }

    var isLoadingFeed by remember { mutableStateOf(false) }
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Smooth loading shimmer effect whenever category filters or search inputs change
    LaunchedEffect(activeTab, categoryFilter, searchQuery) {
        isLoadingFeed = true
        kotlinx.coroutines.delay(450)
        isLoadingFeed = false
    }

    val onLikeToggle = { id: Int ->
        val liked = likedTasks[id] ?: false
        likedTasks[id] = !liked
        val count = likesCount[id] ?: 3
        likesCount[id] = if (liked) count - 1 else count + 1
        Toast.makeText(context, if (liked) "Removeste apoiado." else "Apoiado com sucesso! ❤️", Toast.LENGTH_SHORT).show()
    }

    val onShareTask = { title: String, loc: String ->
        Toast.makeText(context, "Link copiado! 🔗\nbiscate.ao/bico/${title.lowercase().replace(" ", "-")} ($loc)", Toast.LENGTH_LONG).show()
    }

    // Dialog state controllers
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    // Selected job state
    val selectedTaskId by viewModel.selectedTaskId.collectAsStateWithLifecycle()
    val activeTask by viewModel.activeTask.collectAsStateWithLifecycle()
    val activeCandidaturas by viewModel.activeCandidaturas.collectAsStateWithLifecycle()
    val activeChats by viewModel.activeChats.collectAsStateWithLifecycle()

    var showTaskDetailDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var selectedBiscateiroForDetail by remember { mutableStateOf<Biscateiro?>(null) }

    val userRole = session?.userRole ?: "CLIENTE"
    val walletCredits = session?.walletCredits ?: 0
    val currentUserName = session?.name ?: "Mário Santos"

    if (session == null || !session!!.isLoggedIn) {
        BiscateAuthScreen(
            viewModel = viewModel,
            onAccessAsGuest = {
                viewModel.login("+244 931 445 221", "123456", {}, {})
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "BISCATE",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            color = Laranja,
                            letterSpacing = 2.sp,
                            fontSize = 24.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(Laranja, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AO",
                                fontWeight = FontWeight.Bold,
                                color = Branco,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Quick Theme Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.Brightness2,
                            contentDescription = "Alternar Tema",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Notification Bell Icon with Badge
                    IconButton(
                        onClick = { showNotificationsSheet = true },
                        modifier = Modifier.testTag("notification_bell_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                val unreadCount = notifications.count { !it.isRead }
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = Vermelho,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadCount", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Central de Notificações",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Quick Profile Trigger Button
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("onboarding_profile_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Editar Perfil",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Credits Indicator Balance
                    Card(
                        onClick = { activeTab = 3 },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("credits_balance_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallet,
                                contentDescription = "Carteira",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$walletCredits Lds",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(imageVector = if (activeTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Início") },
                    label = { Text("Início", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Laranja, selectedTextColor = Laranja)
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(imageVector = if (activeTab == 1) Icons.Filled.Work else Icons.Outlined.WorkOutline, contentDescription = "Trabalhos") },
                    label = { Text(if (userRole == "CLIENTE") "Meus Pedidos" else "Biscatos", fontSize = 11.sp) },
                    modifier = Modifier.testTag("contracts_tab_item"),
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Laranja, selectedTextColor = Laranja)
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(imageVector = if (activeTab == 2) Icons.Filled.People else Icons.Outlined.People, contentDescription = "Biscateiros") },
                    label = { Text("Parceiros", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Laranja, selectedTextColor = Laranja)
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(imageVector = if (activeTab == 3) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet, contentDescription = "Carteira") },
                    label = { Text("Carteira", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Laranja, selectedTextColor = Laranja)
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(imageVector = if (activeTab == 4) Icons.Filled.Info else Icons.Outlined.Info, contentDescription = "Piloto") },
                    label = { Text("Piloto", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Laranja, selectedTextColor = Laranja)
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 1 && userRole == "CLIENTE") {
                FloatingActionButton(
                    onClick = { showCreateTaskDialog = true },
                    containerColor = Laranja,
                    contentColor = Branco,
                    modifier = Modifier.testTag("create_biscato_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Pedir Serviço")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant Mode Switcher + Welcome Header Board
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Terra),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Olá, $currentUserName 👋",
                                color = Creme,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bem-vindo ao Biscate.ao Angola",
                                color = Areia,
                                fontSize = 12.sp
                            )
                        }

                        // Status pill verifying identity
                        Box(
                            modifier = Modifier
                                .background(Verde, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Branco, modifier = Modifier.size(12.dp))
                                Text("Acesso VIP", color = Branco, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ESTOU NA PLATAFORMA COMO:",
                        fontSize = 10.sp,
                        color = Areia,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulated Dual Economy Switch Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.setRole("CLIENTE")
                                Toast.makeText(context, "Mudou para modo CLIENTE (Submeter pedidos)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userRole == "CLIENTE") Laranja else TerraClaro,
                                contentColor = Branco
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("role_client_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Sou Cliente", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.setRole("PRESTADOR")
                                Toast.makeText(context, "Mudou para modo PRESTADOR (Candidatar a bicos)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userRole == "PRESTADOR") Laranja else TerraClaro,
                                contentColor = Branco
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("role_provider_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Sou Prestador", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tab contents switcher with smooth horizontal slide & fade transitions!
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
                            )
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "tabContentTransitions"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> HomeTab(
                            userRole = userRole,
                            tasks = tasks,
                            biscateiros = biscateiros,
                            likedTasks = likedTasks,
                            likesCount = likesCount,
                            onLikeToggle = onLikeToggle,
                            onShare = onShareTask,
                            isLoading = isLoadingFeed,
                            onNavigateToTasks = { activeTab = 1 },
                            onNavigateToWorkers = { activeTab = 2 },
                            onSelectTask = { id ->
                                viewModel.selectTask(id)
                                showTaskDetailDialog = true
                            }
                        )
                        1 -> JobsTab(
                            userRole = userRole,
                            tasks = tasks,
                            biscateiros = biscateiros,
                            viewModel = viewModel,
                            likedTasks = likedTasks,
                            likesCount = likesCount,
                            onLikeToggle = onLikeToggle,
                            onShare = onShareTask,
                            isLoading = isLoadingFeed,
                            onSelectTask = { id ->
                                viewModel.selectTask(id)
                                showTaskDetailDialog = true
                            },
                            onOpenChat = { id ->
                                viewModel.selectTask(id)
                                showChatDialog = true
                            },
                            onOpenRating = { id ->
                                viewModel.selectTask(id)
                                showRatingDialog = true
                            }
                        )
                        2 -> WorkersTab(
                            biscateiros = biscateiros,
                            onViewWorker = { worker ->
                                selectedBiscateiroForDetail = worker
                            }
                        )
                        3 -> WalletTab(
                            currentCredits = walletCredits,
                            onAddCredits = { amount ->
                                viewModel.addCredits(amount)
                                Toast.makeText(context, "Pronto! Foram creditados +$amount Kz na tua conta.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        4 -> PilotTab(
                            viewModel = viewModel,
                            tasksCount = tasks.size,
                            biscateirosCount = biscateiros.size
                        )
                    }
                }
            }
        }
    }

    // --- POPUPS & DIALOGS LAYER ---

    // 1. Create task request dialog (CLIENTE MODE)
    if (showCreateTaskDialog) {
        var formTitle by remember { mutableStateOf("") }
        var formDesc by remember { mutableStateOf("") }
        var formLocation by remember { mutableStateOf("Talatona") }
        var formCategory by remember { mutableStateOf("Pintura") }
        var formBudget by remember { mutableStateOf("") }
        var formDuration by remember { mutableStateOf("1-2 dias") }

        Dialog(onDismissRequest = { showCreateTaskDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Branco),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Laranja),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Publicar Pedido Rápido",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Terra
                    )
                    Divider(color = CremeEscuro)

                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Título do biscato") },
                        placeholder = { Text("Ex: Pintura completa sala") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        singleLine = true
                    )

                    var expandedCat by remember { mutableStateOf(false) }
                    val categories = listOf("Pintura", "Limpeza", "Electricidade", "Construção", "Canalização", "Carpintaria", "Climatização", "Outros")

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Especialidade requisitada") },
                            trailingIcon = {
                                IconButton(onClick = { expandedCat = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        formCategory = cat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = formLocation,
                            onValueChange = { formLocation = it },
                            label = { Text("Bairro/Cadeia") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = formBudget,
                            onValueChange = { formBudget = it },
                            label = { Text("Orçamento (Kz)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("task_budget_input"),
                            singleLine = true
                        )
                    }

                    var expandedDur by remember { mutableStateOf(false) }
                    val durations = listOf("Poucas horas", "1-2 dias", "1 semana", "Várias semanas", "Contínuo")

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formDuration,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Duração estimada") },
                            trailingIcon = {
                                IconButton(onClick = { expandedDur = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedDur,
                            onDismissRequest = { expandedDur = false }
                        ) {
                            durations.forEach { dur ->
                                DropdownMenuItem(
                                    text = { Text(dur) },
                                    onClick = {
                                        formDuration = dur
                                        expandedDur = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formDesc,
                        onValueChange = { formDesc = it },
                        label = { Text("Descrição dos trabalhos") },
                        placeholder = { Text("Descreve pormenorizadamente o que necessita de ser feito...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("task_desc_input"),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { showCreateTaskDialog = false }) {
                            Text("Cancelar", color = TerraClaro, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val budgetVal = formBudget.toDoubleOrNull() ?: 0.0
                                if (formTitle.trim().isNotEmpty() && formDesc.trim().isNotEmpty() && budgetVal > 0.0) {
                                    viewModel.publishTask(
                                        title = formTitle,
                                        description = formDesc,
                                        category = formCategory,
                                        location = formLocation,
                                        budget = budgetVal,
                                        duration = formDuration
                                    )
                                    showCreateTaskDialog = false
                                    Toast.makeText(context, "Pedido em aberto divulgado no feed nacional!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Preencha todos os campos e valor orçado válido!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Laranja),
                            modifier = Modifier.testTag("submit_biscato_btn")
                        ) {
                            Text("Divulgar Pedido", fontWeight = FontWeight.ExtraBold, color = Branco)
                        }
                    }
                }
            }
        }
    }

    // 2. Main Selected Job detail flow (Includes candidate list for Clients, and Apply button for Workers)
    if (showTaskDetailDialog && activeTask != null) {
        val task = activeTask!!
        Dialog(onDismissRequest = { showTaskDetailDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Branco),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Terra),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Terra, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = task.category.uppercase(),
                                color = Areia,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        JobStatusBadge(status = task.status)
                    }

                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Terra,
                        lineHeight = 24.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Room, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(task.location, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TerraClaro)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Verde, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(task.duration, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Verde)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Creme),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text("Valor Orçado:", fontSize = 12.sp, color = TerraClaro, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${formatKz(task.budget)} Kz",
                                fontSize = 18.sp,
                                color = Laranja,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Column {
                        Text("Descrição Geral:", fontSize = 12.sp, color = Areia, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            color = TerraClaro,
                            lineHeight = 18.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Areia, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Publicado por: ${task.clientName}",
                            fontSize = 12.sp,
                            color = TerraClaro,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Divider(color = CremeEscuro)

                    // Context dependent actions in dialogue:
                    if (userRole == "CLIENTE") {
                        // CLIENT VIEW: list all apply proposals
                        Text(
                            text = "Candidaturas Recebidas (${activeCandidaturas.size}):",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Terra
                        )

                        if (activeCandidaturas.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Creme, RoundedCornerShape(8.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "A aguardar por bicos locais de vizinhos...",
                                    fontSize = 12.sp,
                                    color = TerraClaro,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                activeCandidaturas.forEach { candidature ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Branco),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (candidature.status == "ACEITA") Verde else CremeEscuro),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Laranja),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(candidature.workerInitials, color = Branco, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text(candidature.workerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Terra)
                                                }

                                                Text(
                                                    text = candidature.priceProposal,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = Laranja
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = candidature.message,
                                                fontSize = 12.sp,
                                                color = TerraClaro
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (task.status == "ABERTO") {
                                                Button(
                                                    onClick = {
                                                        viewModel.acceptProposal(
                                                            taskId = task.id,
                                                            candidaturaId = candidature.id,
                                                            workerId = candidature.workerId
                                                        )
                                                        Toast.makeText(context, "Biscateiro contratado! Chat privado aberto.", Toast.LENGTH_LONG).show()
                                                        showTaskDetailDialog = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Verde),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(36.dp)
                                                ) {
                                                    Text("Contratar Vizinho", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Branco)
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .background(if (candidature.status == "ACEITA") Verde else Vermelho, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (candidature.status == "ACEITA") "CONTRATADO" else "REJEITADO",
                                                        color = Branco,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // WORKER VIEW: Apply section
                        val isAlreadyApplied = activeCandidaturas.any { it.workerName == currentUserName }

                        if (task.status != "ABERTO") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Creme, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Esta vaga de biscato já se encontra fechada ou atribuída.",
                                    fontSize = 12.sp,
                                    color = TerraClaro,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (isAlreadyApplied) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Verde.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Verde)
                                    Text(
                                        "Já submeteste a tua candidatura para este biscato!",
                                        fontSize = 12.sp,
                                        color = Verde,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Can apply
                            Button(
                                onClick = {
                                    showApplyDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Laranja),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_proposal_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Branco)
                                    Text("Candidatar-me (-3 Créditos Leads)", fontWeight = FontWeight.Black, color = Branco)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showTaskDetailDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = TerraClaro),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar Detalhes", color = Branco)
                    }
                }
            }
        }
    }

    // 3. Simple Apply bidding form (PRESTADOR MODE)
    if (showApplyDialog && activeTask != null) {
        val task = activeTask!!
        var bidPrice by remember { mutableStateOf("") }
        var bidPitch by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showApplyDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Branco),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Laranja),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Submeter Proposta",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Terra
                    )
                    Text(
                        text = "Custo: 3 Créditos Leads. O seu saldo atual é de $walletCredits créditos.",
                        fontSize = 11.sp,
                        color = Areia,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(color = CremeEscuro)

                    OutlinedTextField(
                        value = bidPrice,
                        onValueChange = { bidPrice = it },
                        label = { Text("Valor da tua proposta (Kz ou Kz/dia)") },
                        placeholder = { Text("Ex: 30.000 Kz total") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("proposal_price_input")
                    )

                    OutlinedTextField(
                        value = bidPitch,
                        onValueChange = { bidPitch = it },
                        label = { Text("Mensagem explicativa para o cliente") },
                        placeholder = { Text("Ex: Olá, tenho 5 anos de experiência e posso começar já na segunda de manhã...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("proposal_message_input"),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { showApplyDialog = false }) {
                            Text("Retroceder", color = TerraClaro)
                        }

                        Button(
                            onClick = {
                                if (bidPrice.trim().isNotEmpty() && bidPitch.trim().isNotEmpty()) {
                                    viewModel.applyForTask(
                                        taskId = task.id,
                                        workerId = 99, // Static testing user ID
                                        workerName = currentUserName,
                                        workerInitials = currentUserName.split(" ").map { it.take(1) }.joinToString(""),
                                        priceProposal = bidPrice,
                                        coverMessage = bidPitch,
                                        onSuccess = {
                                            showApplyDialog = false
                                            showTaskDetailDialog = false
                                            Toast.makeText(context, "Candidatura enviada! Foram reduzidos -3 créditos.", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { errorMsg ->
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Preencha todos os campos do lance!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Laranja),
                            modifier = Modifier.testTag("submit_proposal_confirm_btn")
                        ) {
                            Text("Confirmar Envio", fontWeight = FontWeight.Bold, color = Branco)
                        }
                    }
                }
            }
        }
    }

    // 4. Simulated Real-time Instant Chat
    if (showChatDialog && activeTask != null) {
        val task = activeTask!!
        var chatInputText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        // Trigger scroll to bottom on every database insert message
        LaunchedEffect(activeChats.size) {
            if (activeChats.isNotEmpty()) {
                listState.animateScrollToItem(activeChats.size - 1)
            }
        }

        Dialog(onDismissRequest = { showChatDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Branco),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Terra),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Terra)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(task.title, fontWeight = FontWeight.Bold, color = Branco, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Canal Privado • Proposta Atribuída", fontSize = 11.sp, color = Areia)
                            }
                            IconButton(onClick = { showChatDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Branco)
                            }
                        }
                    }

                    // Chat logs scroll area
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Creme)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeChats) { msg ->
                            val isMe = (userRole == "CLIENTE" && msg.sender == "CLIENTE") ||
                                    (userRole == "PRESTADOR" && msg.sender == "PRESTADOR")

                            val isSystem = msg.sender == "SISTEMA"

                            if (isSystem) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Terra.copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            text = msg.content,
                                            fontSize = 11.sp,
                                            color = TerraClaro,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                ) {
                                    Text(
                                        text = msg.senderName,
                                        fontSize = 10.sp,
                                        color = TerraClaro,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMe) Laranja else Branco,
                                            contentColor = if (isMe) Branco else Terra
                                        ),
                                        shape = RoundedCornerShape(
                                            topStart = 8.dp,
                                            topEnd = 8.dp,
                                            bottomStart = if (isMe) 8.dp else 0.dp,
                                            bottomEnd = if (isMe) 0.dp else 8.dp
                                        )
                                    ) {
                                        Text(
                                            text = msg.content,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Chat inputs bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Escreve uma mensagem...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            maxLines = 2,
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (chatInputText.trim().isNotEmpty()) {
                                    viewModel.sendChatMessage(task.id, chatInputText)
                                    chatInputText = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Laranja),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Branco)
                        }
                    }
                }
            }
        }
    }

    // 5. Rating Completion Dialog (CLIENTE MODE)
    if (showRatingDialog && activeTask != null) {
        val task = activeTask!!
        var starRating by remember { mutableStateOf(5) }
        var feedbackComment by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showRatingDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Branco),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Laranja),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Concluir & Avaliar",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Terra
                    )
                    Text(
                        text = "O que achaste do trabalho do teu vizinho? A tua avaliação vai ajudar no ranking de confiança do bairro.",
                        fontSize = 12.sp,
                        color = TerraClaro,
                        textAlign = TextAlign.Center
                    )
                    Divider(color = CremeEscuro)

                    // 5-Star Selection Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { index ->
                            IconButton(
                                onClick = { starRating = index },
                                modifier = Modifier.testTag("star_$index")
                            ) {
                                Icon(
                                    imageVector = if (index <= starRating) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "$index Estrelas",
                                    tint = if (index <= starRating) Laranja else Areia,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feedbackComment,
                        onValueChange = { feedbackComment = it },
                        label = { Text("Comentário (opcional)") },
                        placeholder = { Text("Ex: Muito rápido e educado. Trabalho de pintura excelente...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { showRatingDialog = false }) {
                            Text("Fechar", color = TerraClaro)
                        }

                        Button(
                            onClick = {
                                viewModel.completeJob(task.id, starRating, feedbackComment)
                                showRatingDialog = false
                                Toast.makeText(context, "Biscato finalizado com sucesso! Classificação registada.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Verde),
                            modifier = Modifier.testTag("submit_rating_btn")
                        ) {
                            Text("Fechar Negócio", fontWeight = FontWeight.Bold, color = Branco)
                        }
                    }
                }
            }
        }
    }

    // 6. Worker Detail Public Profile Dialogue
    if (selectedBiscateiroForDetail != null) {
        val worker = selectedBiscateiroForDetail!!
        Dialog(onDismissRequest = { selectedBiscateiroForDetail = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Laranja),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Header Block
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(worker.bgColor))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = worker.initials,
                                color = Color(android.graphics.Color.parseColor(worker.textColor)),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = worker.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (worker.verified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Identidade Verificada",
                                        tint = Verde,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = worker.role,
                                fontSize = 13.sp,
                                color = Laranja,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Contacto: ${worker.contact}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Key Stats Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", worker.ratingAvg),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text("reputação", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${worker.ratingCount}+",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("bicos feitos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = worker.bairro,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("zona atuação", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Section 1: Descrição Geral (Resumo)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                            Text("Resumo do Serviço:", fontSize = 12.sp, color = Laranja, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = worker.desc,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }

                    // Section 2: Experiência Detalhada (if provided)
                    if (worker.experiencia.isNotBlank()) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                                Text("Experiência Profissional Detalhada:", fontSize = 12.sp, color = Laranja, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = worker.experiencia,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Section 3: Competências Principais
                    if (worker.competencias.isNotBlank()) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                                Text("Competências & Diferenciais:", fontSize = 12.sp, color = Laranja, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val bullets = worker.competencias.split(",")
                            bullets.forEach { bullet ->
                                if (bullet.trim().isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text("• ", fontWeight = FontWeight.Bold, color = Laranja, fontSize = 14.sp)
                                        Text(
                                            text = bullet.trim(),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Habilidades (Tags)
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Construction, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                            Text("Habilidades Tecnológicas / Áreas:", fontSize = 12.sp, color = Laranja, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRowWithCustom(
                            tagsList = worker.tags.split(",")
                        )
                    }

                    // Section 5: PROVAS DE TRABALHOS (PORTFÓLIO DE SUCESSO)
                    if (worker.provasTrabalho.isNotBlank()) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                                Text("Provas de Trabalho Concluídos (Portfólio):", fontSize = 12.sp, color = Laranja, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            val projects = worker.provasTrabalho.split("|")
                            projects.forEachIndexed { index, project ->
                                if (project.trim().isNotBlank()) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                                        border = BorderStroke(1.dp, Laranja.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Laranja.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Trabalho Verificado",
                                                    tint = Laranja,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Trabalho #${index + 1}",
                                                    fontSize = 10.sp,
                                                    color = Laranja,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = project.trim(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 15.sp
                                                )
                                                Text(
                                                    text = "✓ Verificado física e contratualmente em Angola",
                                                    fontSize = 9.sp,
                                                    color = Verde,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pricing Row
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preço de Referência:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text(worker.rate, fontSize = 16.sp, color = Laranja, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "A direcionar para chamada de voz com ${worker.name} (${worker.contact})...", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Verde),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Telefonar / Sms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Button(
                            onClick = { selectedBiscateiroForDetail = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fechar Perfil", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationsSheet = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Laranja
                        )
                        Text(
                            text = "Notificações",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Terra
                        )
                    }
                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearNotifications() }
                        ) {
                            Text("Limpar", color = Laranja, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notifications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = Areia,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Sem novas notificações",
                                fontSize = 14.sp,
                                color = TerraClaro,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Avisaremos quando houver atualizações nos teus bicos ou propostas.",
                                fontSize = 11.sp,
                                color = Areia,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        // Mark all as read when opening the notification panel
                        LaunchedEffect(Unit) {
                            viewModel.markAllAsRead()
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(notifications.size) { index ->
                                val notification = notifications[index]
                                val categoryIcon = when (notification.category) {
                                    "FINANCEIRO" -> Icons.Default.AccountBalanceWallet
                                    "TRABALHO" -> Icons.Default.Work
                                    "CHAT" -> Icons.Default.Chat
                                    else -> Icons.Default.Info
                                }
                                val categoryColor = when (notification.category) {
                                    "FINANCEIRO" -> Verde
                                    "TRABALHO" -> Laranja
                                    "CHAT" -> Color(0xFF0077B6)
                                    else -> TerraClaro
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Branco),
                                    border = BorderStroke(1.dp, CinzaBorda),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(categoryColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = categoryIcon,
                                                contentDescription = null,
                                                tint = categoryColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = notification.title,
                                                    color = Terra,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                
                                                // Unread indicator dot
                                                if (!notification.isRead) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(Vermelho, CircleShape)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = notification.description,
                                                color = TerraClaro,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Agora há pouco • Luanda",
                                                color = Areia,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationsSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Laranja)
                ) {
                    Text("Fechar", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White, // Force beautiful White theme container color for notifications!
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 7. Profile Edit Dialogue
    if (showProfileDialog) {
        var profileName by remember { mutableStateOf(session?.name ?: "") }
        var profileContact by remember { mutableStateOf(session?.contact ?: "") }
        var profileBairro by remember { mutableStateOf(session?.bairro ?: "Talatona") }
        var profileExperience by remember { mutableStateOf(session?.experiencia ?: "") }
        var profileCompetencias by remember { mutableStateOf(session?.competencias ?: "") }
        var profileProvasTrabalho by remember { mutableStateOf(session?.provasTrabalho ?: "") }

        var isBairroDropdownExpanded by remember { mutableStateOf(false) }
        val bairrosList = listOf("Talatona", "Kilamba", "Viana", "Cazenga", "Sambizanga", "Maianga", "Cacuaco", "Samba")

        Dialog(onDismissRequest = { showProfileDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Laranja),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gerenciar Meu Perfil",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .background(Laranja.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = session?.userRole ?: "CLIENTE",
                                fontWeight = FontWeight.Bold,
                                color = Laranja,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "Gerencie seus dados públicos exibidos no directório e ofertas de trabalho biscate.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_field")
                    )

                    OutlinedTextField(
                        value = profileContact,
                        onValueChange = { profileContact = it },
                        label = { Text("Número de Contacto") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_field")
                    )

                    // BAIRRO SELECTOR DROPDOWN
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = isBairroDropdownExpanded,
                            onExpandedChange = { isBairroDropdownExpanded = !isBairroDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = "Bairro: $profileBairro",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBairroDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isBairroDropdownExpanded,
                                onDismissRequest = { isBairroDropdownExpanded = false }
                            ) {
                                bairrosList.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b) },
                                        onClick = {
                                            profileBairro = b
                                            isBairroDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Worker Specific Detailed Settings
                    if (session?.userRole == "PRESTADOR") {
                        Text(
                            text = "Credenciais do Prestador",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Laranja,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        OutlinedTextField(
                            value = profileExperience,
                            onValueChange = { profileExperience = it },
                            label = { Text("Experiência & Anos de Carreira") },
                            placeholder = { Text("Pintor residencial com 6 anos de experiência em Angola...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        OutlinedTextField(
                            value = profileCompetencias,
                            onValueChange = { profileCompetencias = it },
                            label = { Text("Competências Principais (Separadas por vírgula)") },
                            placeholder = { Text("Pontualidade, Especialista em Drywall, Lixamento perfeito") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = profileProvasTrabalho,
                            onValueChange = { profileProvasTrabalho = it },
                            label = { Text("Trabalhos Concluídos / Portfólio (Separados por '|')") },
                            placeholder = { Text("Reforma de WC no Kilamba|Pintura externa de vivenda em Talatona") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 3
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Buttons Layout Including Logout Action
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showProfileDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Voltar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    if (profileName.trim().isNotEmpty() && profileContact.trim().isNotEmpty()) {
                                        viewModel.updateProfile(
                                            name = profileName,
                                            contact = profileContact,
                                            bairro = profileBairro,
                                            experiencia = profileExperience,
                                            competencias = profileCompetencias,
                                            provasTrabalho = profileProvasTrabalho
                                        )
                                        showProfileDialog = false
                                        Toast.makeText(context, "Perfil guardado com sucesso!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Nome e Contacto são obrigatórios!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Laranja),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("save_profile_btn")
                            ) {
                                Text("Gravar", fontWeight = FontWeight.Bold, color = Branco)
                            }
                        }

                        // Logout interactive button
                        OutlinedButton(
                            onClick = {
                                viewModel.logout()
                                showProfileDialog = false
                                Toast.makeText(context, "Sessão terminada. Volte sempre!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("logout_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                                Text("Terminar Sessão (Sair)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// ============================================
// SUB COMPOSABLES FOR INDIV// --- TAB 0: HOME PAGE (General Overview, Quick Categories List, and highlighted open tasks) ---
@Composable
fun HomeTab(
    userRole: String,
    tasks: List<JobTask>,
    biscateiros: List<Biscateiro>,
    likedTasks: Map<Int, Boolean> = emptyMap(),
    likesCount: Map<Int, Int> = emptyMap(),
    onLikeToggle: (Int) -> Unit = {},
    onShare: (String, String) -> Unit = { _, _ -> },
    isLoading: Boolean = false,
    onNavigateToTasks: () -> Unit,
    onNavigateToWorkers: () -> Unit,
    onSelectTask: (Int) -> Unit
) {
    val context = LocalContext.current

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BiscateSkeletonLoader()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Manifesto Call-to-Action
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "O BISCATE QUE TU PRECISAS",
                            fontWeight = FontWeight.ExtraBold,
                            color = Laranja,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sem algoritmo manipulado. Sem comissões escondidas.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Biscate.ao liga parceiros honestos do mesmo bairro de Luanda ou outras regiões para reparações, montagens e limpezas imediatas.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Quick Specialties/Categories Grid (Horizontally scrollable or inline chips)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESPECIALIDADES",
                            fontSize = 11.sp,
                            color = Areia,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        TextButton(onClick = onNavigateToWorkers) {
                            Text("Ver Todos", color = Laranja, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickCats = listOf(
                            "🎨 Pintura", "🧹 Limpeza", "⚡ Electricidade", "🏗️ Construção",
                            "🔧 Canalização", "🪚 Carpintaria", "❄️ AC/Frio"
                        )
                        quickCats.forEach { cat ->
                            AssistChip(
                                onClick = {
                                    Toast.makeText(context, "A filtrar biscateiros por: ${cat.substring(3)}", Toast.LENGTH_SHORT).show()
                                    onNavigateToWorkers()
                                },
                                label = { Text(cat, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }

            // Live Open Board Request
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2ECC71))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PEDIDOS EM ABERTO RECENTES",
                            fontSize = 11.sp,
                            color = Areia,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    TextButton(onClick = onNavigateToTasks) {
                        Text("Ver Todos", color = Laranja, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            val openTasks = tasks.filter { it.status == "ABERTO" }.take(3)
            if (openTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Nenhum pedido recente em aberto.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(openTasks) { task ->
                    JobCardItem(
                        task = task,
                        isLiked = likedTasks[task.id] ?: false,
                        likesCount = likesCount[task.id] ?: 3,
                        onLikeToggle = { onLikeToggle(task.id) },
                        onShare = { onShare(task.title, task.location) },
                        onClick = { onSelectTask(task.id) }
                    )
                }
            }
        }
    }
}

// --- TAB 1: JOBS TAB ---
// (Changes visual layout based on Role state - Cliente workflow has "My posted requests", Worker workflow has "Browse listings")
@Composable
fun JobsTab(
    userRole: String,
    tasks: List<JobTask>,
    biscateiros: List<Biscateiro>,
    viewModel: BiscateViewModel,
    likedTasks: Map<Int, Boolean> = emptyMap(),
    likesCount: Map<Int, Int> = emptyMap(),
    onLikeToggle: (Int) -> Unit = {},
    onShare: (String, String) -> Unit = { _, _ -> },
    isLoading: Boolean = false,
    onSelectTask: (Int) -> Unit,
    onOpenChat: (Int) -> Unit,
    onOpenRating: (Int) -> Unit
) {
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (userRole == "CLIENTE") {
            // CLIENT EXPERIENCE: Manage own services
            Text(
                text = "GESTÃO DE ENCOMENDAS",
                fontSize = 10.sp,
                color = Areia,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, tint = Areia, modifier = Modifier.size(64.dp))
                        Text(
                            text = "Ainda não criaste nenhum pedido rápido! Crie um clicando no botão (+) laranja no canto inferior.",
                            fontSize = 13.sp,
                            color = TerraClaro,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks) { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Branco),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, CremeEscuro),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Terra
                                        )
                                        Text(
                                            text = "Categoria: ${task.category} •📍 ${task.location}",
                                            fontSize = 12.sp,
                                            color = TerraClaro
                                        )
                                    }

                                    JobStatusBadge(status = task.status)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatKz(task.budget)} Kz",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Laranja
                                    )

                                    // Display actions depending on active state status
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (task.status == "ABERTO") {
                                            IconButton(
                                                onClick = { viewModel.deleteTask(task) }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Cancelar Vaga", tint = Vermelho)
                                            }

                                            Button(
                                                onClick = { onSelectTask(task.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Terra),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("Candidatos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else if (task.status == "EM_CURSO") {
                                            Button(
                                                onClick = { onOpenChat(task.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Laranja),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Button(
                                                onClick = { onOpenRating(task.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Verde),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("Concluir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Star, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Classificação: ${task.rating ?: 5}/5",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Terra
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // WORKER EXPERIENCE: Browse available listings in neighborhood
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Pesquisar bicos... Ex: Pintor, fossa", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Areia) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Quick Category selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val catFilters = listOf("Todos", "Pintura", "Limpeza", "Electricidade", "Construção", "Canalização", "Carpintaria")
                catFilters.forEach { item ->
                    val isActive = categoryFilter == item
                    FilterChip(
                        selected = isActive,
                        onClick = { viewModel.setCategoryFilter(item) },
                        label = { Text(item, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Laranja,
                            selectedLabelColor = Branco
                        )
                    )
                }
            }

            val filteredTasks = tasks.filter { task ->
                val matchCat = categoryFilter == "Todos" || task.category == categoryFilter
                val matchQuery = searchQuery.isEmpty() ||
                        task.title.contains(searchQuery, ignoreCase = true) ||
                        task.description.contains(searchQuery, ignoreCase = true) ||
                        task.category.contains(searchQuery, ignoreCase = true)
                matchCat && matchQuery
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(10.dp))
                BiscateSkeletonLoader()
            } else if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem biscates ativos anunciados para esta especialidade no momento.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "BICOS DISPONÍVEIS (${filteredTasks.size}):",
                            fontSize = 10.sp,
                            color = Areia,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    items(filteredTasks) { task ->
                        JobCardItem(
                            task = task,
                            isLiked = likedTasks[task.id] ?: false,
                            likesCount = likesCount[task.id] ?: 3,
                            onLikeToggle = { onLikeToggle(task.id) },
                            onShare = { onShare(task.title, task.location) },
                            onClick = { onSelectTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 2: WORKERS DIRECTORY ---
@Composable
fun WorkersTab(
    biscateiros: List<Biscateiro>,
    onViewWorker: (Biscateiro) -> Unit
) {
    var searchWorkerQuery by remember { mutableStateOf("") }
    var selectedCatFilter by remember { mutableStateOf("Todos") }

    val filteredWorkers = biscateiros.filter { worker ->
        val matchCat = selectedCatFilter == "Todos" || worker.category == selectedCatFilter
        val matchSearch = searchWorkerQuery.isEmpty() ||
                worker.name.contains(searchWorkerQuery, ignoreCase = true) ||
                worker.role.contains(searchWorkerQuery, ignoreCase = true)
        matchCat && matchSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchWorkerQuery,
            onValueChange = { searchWorkerQuery = it },
            placeholder = { Text("Procurar prestador... Ex: Pedro", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Areia) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val categories = listOf("Todos", "Pintura", "Limpeza", "Electricidade", "Construção", "Canalização", "Carpintaria")
            categories.forEach { cat ->
                val isActive = selectedCatFilter == cat
                FilterChip(
                    selected = isActive,
                    onClick = { selectedCatFilter = cat },
                    label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Laranja,
                        selectedLabelColor = Branco
                    )
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredWorkers) { worker ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScaleEffect()
                        .clickable { onViewWorker(worker) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(worker.bgColor))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = worker.initials,
                                color = Color(android.graphics.Color.parseColor(worker.textColor)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = worker.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (worker.verified) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Verde, modifier = Modifier.size(14.dp))
                            }
                        }

                        Text(
                            text = worker.role,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Laranja, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                  text = "${worker.ratingAvg} (${worker.ratingCount})",
                                  fontSize = 11.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("📍 ${worker.bairro}", fontSize = 10.sp, color = Laranja, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: WALLET & LEADS PACK CONVERSION ---
@Composable
fun WalletTab(
    currentCredits: Int,
    onAddCredits: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explaining leads concept widget
        Card(
            colors = CardDefaults.cardColors(containerColor = Terra),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "A CARTEIRA DE LÍDERES (LEADS)",
                    fontSize = 10.sp,
                    color = Areia,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Como funciona a nossa monetização?",
                    color = Branco,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Adotamos o modelo democrático de Leads (Paga por Proposta), tal como a Fixando, Zaask e Habitissimo. " +
                            "O registo é 100% grátis. Clientes publicam de graça. Os prestadores pagam apenas 3 créditos leads por candidatura enviada para um trabalho. " +
                            "Sem comissões ocultas sobre o valor final do seu trabalho!",
                    fontSize = 12.sp,
                    color = Creme,
                    lineHeight = 16.sp
                )
            }
        }

        // Current user credits box balance
        Card(
            colors = CardDefaults.cardColors(containerColor = Branco),
            border = BorderStroke(2.dp, Laranja),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "O SEU SALDO DE LEADS:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Areia
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currentCredits Lds",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Laranja
                )
                Text(
                    text = "1 lead enviada = 3 créditos consumidos",
                    fontSize = 12.sp,
                    color = TerraClaro,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Text(
            text = "ADQUIRIR PACOTES DE CRÉDITO:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Areia,
            letterSpacing = 1.sp
        )

        // Predefined promo credit packs
        val packs = listOf(
            CreditPack("Pack Bronze", 10, "1.500 Kz", "Ideal para biscates pontuais"),
            CreditPack("Pack Prata (Popular)", 35, "4.500 Kz", "Melhor economia residencial"),
            CreditPack("Pack Ouro", 100, "10.000 Kz", "Maior visibilidade empresarial")
        )

        packs.forEachIndexed { index, pack ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Branco),
                border = BorderStroke(1.5.dp, if (index == 1) Laranja else CremeEscuro),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pack.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Terra)
                        Text(pack.description, fontSize = 11.sp, color = TerraClaro)
                        Text("+${pack.credits} Créditos de Candidatura", fontSize = 12.sp, color = Verde, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onAddCredits(pack.credits) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (index == 1) Laranja else Terra),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(pack.price, fontWeight = FontWeight.Bold, color = Branco, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- TAB 4: PILOT, COMMUNITY AND ADMIN SANDBOX CONTROL ---
@Composable
fun PilotTab(
    viewModel: BiscateViewModel,
    tasksCount: Int,
    biscateirosCount: Int
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Branco),
            border = BorderStroke(1.dp, CremeEscuro)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ESTRATÉGIA GO-TO-MARKET ANGOLA",
                    fontSize = 10.sp,
                    color = Laranja,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lançamento Piloto Luanda",
                    color = Terra,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "O Biscate.ao planeia a sua validação inicial em Luanda (Cazenga, Talatona, Kilamba, Viana e Maianga) nas próximas semanas " +
                            "através de parcerias locais com administrações de bairro, panfletagem focalizada em depósitos de materiais de construção e divulgação em redes sociais comunitárias.\n\n" +
                            "Meta piloto: obter 1.000 usuários ativos e 500 bicos concluídos com sucesso no primeiro semestre.",
                    fontSize = 12.sp,
                    color = TerraClaro,
                    lineHeight = 16.sp
                )
            }
        }

        // Checklist guidelines indicators
        Card(
            colors = CardDefaults.cardColors(containerColor = Branco),
            border = BorderStroke(1.dp, CremeEscuro)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CONFIANÇA & CONTROLOS DE SEGURANÇA",
                    fontSize = 10.sp,
                    color = Areia,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                SecurityItemRow(title = "Verificação de Identidade Básica", desc = "Upload simples de BI para selo de Confiança.")
                SecurityItemRow(title = "Políticas Anti-Fraude e Moderação", desc = "Remoção de comentários ofensivos e perfis clonados.")
                SecurityItemRow(title = "Avaliações Autênticas e Verificadas", desc = "Apenas clientes que realmente contrataram avaliam.")
            }
        }

        // Simulated admin control box
        Card(
            colors = CardDefaults.cardColors(containerColor = Terra),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PAINEL ADMINISTRADOR SIMULADO (SANDBOX)",
                    color = Areia,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Divider(color = TerraClaro)

                Text(
                    text = "Métricas Ativas de Banco Room SQLite:",
                    color = Creme,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Pedidos na Tabela:", color = Creme, fontSize = 12.sp)
                    Text("$tasksCount", color = Laranja, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Biscateiros Cadastrados:", color = Creme, fontSize = 12.sp)
                    Text("$biscateirosCount", color = Laranja, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addCredits(100)
                            Toast.makeText(context, "Bónus Admin: +100 créditos adicionados!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Laranja)
                    ) {
                        Text("Simular +100 Lds", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Sincronização forçada com SQLite realizada com êxito!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerraClaro)
                    ) {
                        Text("Forçar Sync Room", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ============================================
// SHARED HELPER UI MODULES & DECORATORS
// ============================================

@Composable
fun JobCardItem(
    task: JobTask,
    isLiked: Boolean = false,
    likesCount: Int = 3,
    onLikeToggle: () -> Unit = {},
    onShare: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleEffect()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val emoji = when (task.category.lowercase()) {
                        "pintura" -> "🎨"
                        "limpeza" -> "🧹"
                        "electricidade" -> "⚡"
                        "construção" -> "🏗️"
                        "canalização" -> "🔧"
                        "carpintaria" -> "🪚"
                        else -> "⚙️"
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }

                    Column {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "📍 ${task.location} • ⏳ ${task.duration}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                JobStatusBadge(status = task.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Orçamento:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${formatKz(task.budget)} Kz",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Laranja
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Apoiar / Like Button
                    Row(
                        modifier = Modifier
                            .background(
                                if (isLiked) Laranja.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onLikeToggle() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Apoiar",
                            tint = if (isLiked) Laranja else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (likesCount > 0) "$likesCount" else "Apoiar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiked) Laranja else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partilhar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.pressScaleEffect(): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pressScaleAnim"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.shimmerPulse(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return this.graphicsLayer { this.alpha = alpha }
}

@Composable
fun BiscateSkeletonLoader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shimmerPulse(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 12.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 70.dp, height = 8.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 50.dp, height = 16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 70.dp, height = 24.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun FlowRowWithCustom(tagsList: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tagsList.forEach { tag ->
            Box(
                modifier = Modifier
                    .background(Creme, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = tag.trim(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerraClaro
                )
            }
        }
    }
}

@Composable
fun SecurityItemRow(title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Verde, modifier = Modifier.size(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Terra)
            Text(desc, fontSize = 11.sp, color = TerraClaro)
        }
    }
}

@Composable
fun JobStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "ABERTO" -> Triple(Creme, Laranja, "Em Aberto")
        "EM_CURSO" -> Triple(TerraClaro.copy(alpha = 0.1f), TerraClaro, "Em Curso")
        "CONCLUIDO" -> Triple(Verde.copy(alpha = 0.1f), Verde, "Concluído")
        else -> Triple(Creme, Terra, "Status")
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatKz(amount: Double): String {
    return String.format("%,.0f", amount).replace(",", ".")
}

// Modifiers helper to control sizing cleanly
fun Modifier.size32() = this.size(14.dp)

data class CreditPack(
    val name: String,
    val credits: Int,
    val price: String,
    val description: String
)
