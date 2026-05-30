package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BiscateNotification(
    val id: Int,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val category: String = "SISTEMA" // "SISTEMA", "FINANCEIRO", "TRABALHO", "CHAT"
)

class BiscateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BiscateRepository

    // Raw flows from Database
    val allTasks: StateFlow<List<JobTask>>
    val allBiscateiros: StateFlow<List<Biscateiro>>
    val session: StateFlow<UserSession?>

    // Selected task for Details or Chat screen
    private val _selectedTaskId = MutableStateFlow<Int?>(null)
    val selectedTaskId = _selectedTaskId.asStateFlow()

    // Combined flows observing selected task details
    val activeTask: StateFlow<JobTask?> = _selectedTaskId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else allTasks.map { tasks -> tasks.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeCandidaturas: StateFlow<List<Candidatura>> = _selectedTaskId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getCandidaturasForTask(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeChats: StateFlow<List<ChatMessage>> = _selectedTaskId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMessagesForTask(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter properties
    private val _categoryFilter = MutableStateFlow("Todos")
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _durationFilter = MutableStateFlow("Qualquer")
    val durationFilter = _durationFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Global Theme & Notifications Center States
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    private val _notifications = MutableStateFlow<List<BiscateNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private var nextNotificationId = 1

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun addNotification(title: String, description: String, category: String = "SISTEMA") {
        val newNotif = BiscateNotification(
            id = nextNotificationId++,
            title = title,
            description = description,
            category = category
        )
        _notifications.update { list -> listOf(newNotif) + list }
    }

    fun markAllAsRead() {
        _notifications.update { list ->
            list.map { it.copy(isRead = true) }
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BiscateRepository(database.appDao())

        // Seed initial notifications to show on first access
        addNotification(
            title = "Bem-vindo ao Biscate.ao!",
            description = "Encontre prestadores qualificados em Luanda sem intermediários nem taxas percentuais. O bico direto e honesto!",
            category = "SISTEMA"
        )
        addNotification(
            title = "Oferta de Boas-Vindas 🎉",
            description = "Recebeste bónus de 50 créditos grátis para se candidatar ou publicar pedidos de serviços na app.",
            category = "FINANCEIRO"
        )

        // Initial seeding check
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }

        allTasks = repository.allTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allBiscateiros = repository.allBiscateiros
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        session = repository.userSession
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    // Tab Filters
    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun setDurationFilter(duration: String) {
        _durationFilter.value = duration
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Session Switcher
    fun setRole(role: String) {
        viewModelScope.launch {
            val current = session.value ?: return@launch
            repository.saveUserSession(current.copy(userRole = role))
        }
    }

    fun selectTask(taskId: Int?) {
        _selectedTaskId.value = taskId
    }

    // Publish New Task (Cliente Mode)
    fun publishTask(
        title: String,
        description: String,
        category: String,
        location: String,
        budget: Double,
        duration: String
    ) {
        viewModelScope.launch {
            val user = session.value
            val authorName = user?.name ?: "Cliente Anónimo"
            val authorContact = user?.contact ?: "+244 900 000 000"

            val newTask = JobTask(
                title = title,
                description = description,
                category = category,
                location = location,
                budget = budget,
                duration = duration,
                clientName = authorName,
                clientContact = authorContact
            )
            repository.insertTask(newTask)
            
            addNotification(
                title = "Pedido Publicado 📝",
                description = "O teu pedido '$title' com orçamento de ${budget.toInt()} Kz em $location foi publicado com sucesso.",
                category = "TRABALHO"
            )
        }
    }

    // Job proposal apply (Prestador Mode)
    // Uses 3 Proposal Credits
    fun applyForTask(
        taskId: Int,
        workerId: Int,
        workerName: String,
        workerInitials: String,
        priceProposal: String,
        coverMessage: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = session.value ?: return@launch
            if (user.walletCredits < 3) {
                onError("Créditos insuficientes! Tens apenas ${user.walletCredits} créditos. Adquire um pacote na Carteira.")
                return@launch
            }

            // Deduct credits and insert proposal
            repository.saveUserSession(user.copy(walletCredits = user.walletCredits - 3))

            val proposal = Candidatura(
                taskId = taskId,
                workerId = workerId,
                workerName = workerName,
                workerInitials = workerInitials,
                priceProposal = priceProposal,
                message = coverMessage,
                status = "PENDENTE"
            )
            repository.insertCandidatura(proposal)
            
            addNotification(
                title = "Candidatura Enviada 💼",
                description = "Candidataste-te com sucesso ao biscato #$taskId com proposta de $priceProposal. Foram descontados 3 créditos.",
                category = "TRABALHO"
            )
            
            onSuccess()
        }
    }

    // Client accepts a proposal (Cliente Mode)
    fun acceptProposal(taskId: Int, candidaturaId: Int, workerId: Int) {
        viewModelScope.launch {
            // Find active task
            val task = repository.getTaskById(taskId) ?: return@launch
            repository.updateTask(task.copy(status = "EM_CURSO", assignedWorkerId = workerId))

            // Update candidate status
            val pList = activeCandidaturas.value
            var approvedCandidateName = "Parceiro"
            for (p in pList) {
                if (p.id == candidaturaId) {
                    approvedCandidateName = p.workerName
                    repository.updateCandidatura(p.copy(status = "ACEITA"))
                } else {
                    repository.updateCandidatura(p.copy(status = "REJEITADA"))
                }
            }

            // Post automated chat message
            repository.insertChatMessage(
                ChatMessage(
                    taskId = taskId,
                    sender = "SISTEMA",
                    senderName = "Trabalho Atribuído",
                    content = "Parabéns! O cliente aceitou a candidatura. Iniciem a conversa para combinar os detalhes físicos do biscato."
                )
            )
            
            addNotification(
                title = "Candidatura Aceite! 🤝",
                description = "Aceitaste a proposta de $approvedCandidateName para o biscato '${task.title}'. Iniciaram uma conversa no Chat.",
                category = "CHAT"
            )
        }
    }

    private fun titleOfTask(task: JobTask): String {
        return task.title
    }

    // Complete Job Task + star review rating
    fun completeJob(taskId: Int, rating: Int, feedback: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            repository.updateTask(task.copy(status = "CONCLUIDO", rating = rating, feedback = feedback))

            // Update dynamic rating details of the worker profile
            val workerId = task.assignedWorkerId
            var workerName = "Parceiro"
            if (workerId != null) {
                val workersList = allBiscateiros.value
                val worker = workersList.find { it.id == workerId }
                if (worker != null) {
                    workerName = worker.name
                    val newCount = worker.ratingCount + 1
                    val newAvg = ((worker.ratingAvg * worker.ratingCount) + rating) / newCount
                    repository.insertBiscateiro(worker.copy(ratingCount = newCount, ratingAvg = newAvg))
                }
            }
            
            addNotification(
                title = "Trabalho Concluído ✅",
                description = "Classificaste o trabalho de $workerName em '${task.title}' com $rating estrelas. Obrigado pela avaliação autêntica!",
                category = "TRABALHO"
            )
        }
    }

    // Send chat text
    fun sendChatMessage(taskId: Int, text: String) {
        viewModelScope.launch {
            val user = session.value ?: return@launch
            val isClient = user.userRole == "CLIENTE"
            val senderRole = if (isClient) "CLIENTE" else "PRESTADOR"
            val senderName = if (isClient) user.name else "Biscateiro Parceiro"

            repository.insertChatMessage(
                ChatMessage(
                    taskId = taskId,
                    sender = senderRole,
                    senderName = senderName,
                    content = text
                )
            )
        }
    }

    // Delete task (Cliente mode retract)
    fun deleteTask(task: JobTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Credit Pack topup store
    fun addCredits(amount: Int) {
        viewModelScope.launch {
            val user = session.value ?: return@launch
            repository.saveUserSession(user.copy(walletCredits = user.walletCredits + amount))
            addNotification(
                title = "Créditos Adicionados 💳",
                description = "Foram recarregados +$amount créditos à sua carteira. Saldo atual: ${user.walletCredits + amount} créditos.",
                category = "FINANCEIRO"
            )
        }
    }

    // Custom Profile creation simulation
    fun updateProfile(
        name: String,
        contact: String,
        bairro: String = "Talatona",
        experiencia: String = "",
        competencias: String = "",
        provasTrabalho: String = ""
    ) {
        viewModelScope.launch {
            val user = session.value ?: return@launch
            repository.saveUserSession(
                user.copy(
                    name = name,
                    contact = contact,
                    bairro = bairro,
                    experiencia = experiencia,
                    competencias = competencias,
                    provasTrabalho = provasTrabalho
                )
            )
            
            addNotification(
                title = "Perfil Atualizado 👤",
                description = "As alterações do seu perfil em $bairro foram gravadas com sucesso.",
                category = "SISTEMA"
            )
            
            // If the user is a PRESTADOR, sync or update their corresponding Biscateiro record as well
            if (user.userRole == "PRESTADOR") {
                val existing = allBiscateiros.value.find { it.contact == contact || it.name == name }
                if (existing != null) {
                    repository.insertBiscateiro(
                        existing.copy(
                            name = name,
                            bairro = bairro,
                            contact = contact,
                            desc = experiencia,
                            experiencia = experiencia,
                            competencias = competencias,
                            provasTrabalho = provasTrabalho
                        )
                    )
                }
            }
        }
    }

    // AUTHENTICATION FLOWS (Login, Register & Logout)
    fun login(contact: String, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val current = session.value
            if (current != null && current.contact == contact && current.password == pin) {
                repository.saveUserSession(current.copy(isLoggedIn = true))
                addNotification(
                    title = "Sessão Iniciada",
                    description = "Olá, ${current.name}! Bem-vindo de volta ao Biscate.ao.",
                    category = "SISTEMA"
                )
                onSuccess()
            } else {
                // Let's check if there is a seeded or matching worker with this contact to allow logging in as standard workers!
                val bWorker = allBiscateiros.value.find { it.contact == contact }
                if (bWorker != null) {
                    val fallbackPin = pin.ifEmpty { "123456" }
                    // Log in as this worker
                    repository.saveUserSession(
                        UserSession(
                            id = 1,
                            userRole = "PRESTADOR",
                            name = bWorker.name,
                            contact = bWorker.contact,
                            walletCredits = 15,
                            isLoggedIn = true,
                            password = fallbackPin,
                            bairro = bWorker.bairro,
                            experiencia = bWorker.experiencia,
                            competencias = bWorker.competencias,
                            provasTrabalho = bWorker.provasTrabalho
                        )
                    )
                    addNotification(
                        title = "Ligação Efetuada",
                        description = "Olá, ${bWorker.name}! Iniciaste sessão como Prestador Parceiro.",
                        category = "SISTEMA"
                    )
                    onSuccess()
                } else if (contact == "923456789" || contact == "+244 923 456 789" || contact == "931445221" || contact == "+244 931 445 221") {
                    // Default fallback login for convenience
                    val fallbackName = if (contact.contains("931")) "Mário Santos" else "João Baptista"
                    repository.saveUserSession(
                        UserSession(
                            id = 1,
                            userRole = "CLIENTE",
                            name = fallbackName,
                            contact = contact,
                            walletCredits = 50,
                            isLoggedIn = true,
                            password = pin.ifEmpty { "123456" }
                        )
                    )
                    addNotification(
                        title = "Sessão Iniciada (Demo)",
                        description = "Entraste com a conta de demonstração de $fallbackName.",
                        category = "SISTEMA"
                    )
                    onSuccess()
                } else {
                    onError("Credenciais incorretas! Experimenta o contacto de testemunho do Mário Santos (+244 931 445 221) com PIN '123456' ou redefina no link 'Recuperar PIN'.")
                }
            }
        }
    }

    fun register(
        name: String,
        contact: String,
        pin: String,
        bairro: String,
        role: String,
        category: String,
        experiencia: String,
        competencias: String,
        provasTrabalho: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || contact.isBlank() || pin.isBlank()) {
                onError("Por favor, preencha o Nome, Contacto e PIN de acesso.")
                return@launch
            }

            // Save new user session
            val newSession = UserSession(
                id = 1,
                userRole = role,
                name = name,
                contact = contact,
                password = pin,
                bairro = bairro,
                walletCredits = if (role == "PRESTADOR") 20 else 50, // 20 free proposals for prestadores!
                isLoggedIn = true,
                experiencia = experiencia,
                competencias = competencias,
                provasTrabalho = provasTrabalho
            )
            repository.saveUserSession(newSession)

            addNotification(
                title = "Conta Criada 🎉",
                description = "Olá, $name! Recebeste um bónus de ${if (role == "PRESTADOR") "20 propostas grátis" else "50 créditos cliente"} para utilizar em Luanda.",
                category = "FINANCEIRO"
            )

            // If registering as PRESTADOR, register as a public worker in the directory!
            if (role == "PRESTADOR") {
                val b = Biscateiro(
                    name = name,
                    role = "$category Profissional",
                    category = category,
                    bairro = bairro,
                    rate = "Sob Orçamento",
                    desc = experiencia.ifEmpty { "Prestador de serviços especializado em $category pronto a atender na região do $bairro." },
                    initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").take(2),
                    bgColor = "#E6F4EA",
                    textColor = "#137333",
                    tags = category,
                    contact = contact,
                    verified = false,
                    ratingAvg = 5.0f,
                    ratingCount = 0,
                    experiencia = experiencia,
                    competencias = competencias,
                    provasTrabalho = provasTrabalho
                )
                repository.insertBiscateiro(b)
                
                addNotification(
                    title = "Perfil de Prestador Ativo 👷",
                    description = "O teu perfil de $category foi adicionado ao diretório público e já está visível para clientes em $bairro.",
                    category = "SISTEMA"
                )
            }
            onSuccess()
        }
    }

    fun logout() {
        viewModelScope.launch {
            val current = session.value ?: return@launch
            repository.saveUserSession(current.copy(isLoggedIn = false))
            addNotification(
                title = "Sessão Terminada",
                description = "Até logo, ${current.name}! Volte sempre para fechar mais biscates.",
                category = "SISTEMA"
            )
        }
    }

    // Interactive PIN/Password Recovery Simulation and Reset
    fun recoverPasswordAndReset(
        contact: String,
        newPin: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (contact.isBlank() || newPin.isBlank()) {
                onError("Por favor, preencha o número de telemóvel e o novo PIN de acesso.")
                return@launch
            }
            if (newPin.length < 4) {
                onError("O PIN de segurança deve ter pelo menos 4 dígitos.")
                return@launch
            }

            // Check details against existing session row
            val currentSession = repository.getUserSession()
            if (currentSession != null && (currentSession.contact == contact || currentSession.contact.replace(" ", "") == contact.replace(" ", ""))) {
                repository.saveUserSession(currentSession.copy(password = newPin))
                addNotification(
                    title = "PIN Redefinido",
                    description = "Redefiniste o seu código PIN de acesso com sucesso no telemóvel $contact.",
                    category = "SISTEMA"
                )
                onSuccess("Sucesso! O seu PIN foi redefinido para '$newPin'. Pode efetuar login agora.")
                return@launch
            }

            // Check if matches seeded worker
            val bWorker = allBiscateiros.value.find { it.contact == contact || it.contact.replace(" ", "") == contact.replace(" ", "") }
            if (bWorker != null) {
                repository.saveUserSession(
                    UserSession(
                        id = 1,
                        userRole = "PRESTADOR",
                        name = bWorker.name,
                        contact = bWorker.contact,
                        walletCredits = 15,
                        isLoggedIn = false,
                        password = newPin,
                        bairro = bWorker.bairro,
                        experiencia = bWorker.experiencia,
                        competencias = bWorker.competencias,
                        provasTrabalho = bWorker.provasTrabalho
                    )
                )
                addNotification(
                    title = "PIN Recuperado (Prestador)",
                    description = "O PIN do prestador parceiro ${bWorker.name} foi atualizado.",
                    category = "SISTEMA"
                )
                onSuccess("Código PIN do prestador ${bWorker.name} redefinido para '$newPin' com êxito!")
                return@launch
            }

            // Let's create or update a custom user session so they are guaranteed to be able to log in immediately!
            repository.saveUserSession(
                UserSession(
                    id = 1,
                    userRole = "CLIENTE",
                    name = "Utilizador Angola",
                    contact = contact,
                    walletCredits = 50,
                    isLoggedIn = false,
                    password = newPin
                )
            )
            addNotification(
                title = "Código Criado",
                description = "Contacto registado e PIN'$newPin' configurado.",
                category = "SISTEMA"
            )
            onSuccess("Contacto não encontrado no histórico, mas criámos uma nova credencial com o PIN '$newPin'. Pode entrar já!")
        }
    }
}
