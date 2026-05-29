package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BiscateRepository(database.appDao())

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
            for (p in pList) {
                if (p.id == candidaturaId) {
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
        }
    }

    // Complete Job Task + star review rating
    fun completeJob(taskId: Int, rating: Int, feedback: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            repository.updateTask(task.copy(status = "CONCLUIDO", rating = rating, feedback = feedback))

            // Update dynamic rating details of the worker profile
            val workerId = task.assignedWorkerId
            if (workerId != null) {
                val workersList = allBiscateiros.value
                val worker = workersList.find { it.id == workerId }
                if (worker != null) {
                    val newCount = worker.ratingCount + 1
                    val newAvg = ((worker.ratingAvg * worker.ratingCount) + rating) / newCount
                    repository.insertBiscateiro(worker.copy(ratingCount = newCount, ratingAvg = newAvg))
                }
            }
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
        }
    }

    // Custom Profile creation simulation
    fun updateProfile(name: String, contact: String) {
        viewModelScope.launch {
            val user = session.value ?: return@launch
            repository.saveUserSession(user.copy(name = name, contact = contact))
        }
    }
}
