package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BiscateRepository(private val appDao: AppDao) {

    val allTasks: Flow<List<JobTask>> = appDao.getAllTasks()
    val allBiscateiros: Flow<List<Biscateiro>> = appDao.getAllBiscateiros()
    val userSession: Flow<UserSession?> = appDao.getUserSessionFlow()

    fun getCandidaturasForTask(taskId: Int): Flow<List<Candidatura>> =
        appDao.getCandidaturasForTask(taskId)

    fun getCandidaturasForWorker(workerId: Int): Flow<List<Candidatura>> =
        appDao.getCandidaturasForWorker(workerId)

    fun getMessagesForTask(taskId: Int): Flow<List<ChatMessage>> =
        appDao.getMessagesForTask(taskId)

    suspend fun getTaskById(taskId: Int): JobTask? = withContext(Dispatchers.IO) {
        appDao.getTaskById(taskId)
    }

    suspend fun insertTask(task: JobTask): Long = withContext(Dispatchers.IO) {
        appDao.insertTask(task)
    }

    suspend fun updateTask(task: JobTask) = withContext(Dispatchers.IO) {
        appDao.updateTask(task)
    }

    suspend fun deleteTask(task: JobTask) = withContext(Dispatchers.IO) {
        appDao.deleteTask(task)
    }

    suspend fun insertBiscateiro(biscateiro: Biscateiro): Long = withContext(Dispatchers.IO) {
        appDao.insertBiscateiro(biscateiro)
    }

    suspend fun insertCandidatura(candidatura: Candidatura) = withContext(Dispatchers.IO) {
        appDao.insertCandidatura(candidatura)
    }

    suspend fun updateCandidatura(candidatura: Candidatura) = withContext(Dispatchers.IO) {
        appDao.updateCandidatura(candidatura)
    }

    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        appDao.insertChatMessage(message)
    }

    suspend fun getUserSession(): UserSession? = withContext(Dispatchers.IO) {
        appDao.getUserSession()
    }

    suspend fun saveUserSession(session: UserSession) = withContext(Dispatchers.IO) {
        appDao.insertUserSession(session)
    }

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        // Ensure initial session state
        val existingSession = appDao.getUserSession()
        if (existingSession == null) {
            appDao.insertUserSession(
                UserSession(
                    id = 1,
                    userRole = "CLIENTE",
                    name = "Mário Santos",
                    contact = "+244 931 445 221",
                    walletCredits = 50,
                    selectedCategory = "Todos"
                )
            )
        }

        // Seed biscateiros/workers if database table is empty
        val counts = appDao.getBiscateirosCount()
        if (counts == 0) {
            val seedWorkers = listOf(
                Biscateiro(
                    name = "Manuel Sebastião",
                    role = "Pintor Profissional",
                    category = "Pintura",
                    bairro = "Talatona",
                    rate = "20.000 Kz/dia",
                    desc = "Pintura de interiores e exteriores. Trabalho limpo, pontual e acabamento premium (gesso acartonado e texturados). 10 anos de experiência real.",
                    initials = "MS",
                    bgColor = "#FDE8DA",
                    textColor = "#E8642A",
                    tags = "Interiores,Exteriores,Texturados,Reboco",
                    contact = "+244 923 111 001",
                    verified = true,
                    ratingAvg = 4.8f,
                    ratingCount = 34
                ),
                Biscateiro(
                    name = "Rosa Catarina",
                    role = "Limpeza Geral & Pós-Obra",
                    category = "Limpeza",
                    bairro = "Kilamba",
                    rate = "8.000 Kz/turno",
                    desc = "Limpeza doméstica, comercial e tratamento pós-obra. Trago os meus próprios equipamentos e desinfetantes ecológicos. Rapidez nos prazos.",
                    initials = "RC",
                    bgColor = "#D4EDE3",
                    textColor = "#2D6A4F",
                    tags = "Limpeza,Pós-Obra,Escritórios,Vidros",
                    contact = "+244 923 222 002",
                    verified = true,
                    ratingAvg = 4.9f,
                    ratingCount = 41
                ),
                Biscateiro(
                    name = "António Feliciano",
                    role = "Eletricista de Baixa Tensão",
                    category = "Electricidade",
                    bairro = "Viana",
                    rate = "25.000 Kz/dia",
                    desc = "Instalações elétricas domésticas, reparação de curtos-circuitos, quadros de distribuição, montagem de luminárias e bombas de água. Atendimento de emergências 24h.",
                    initials = "AF",
                    bgColor = "#D0EAF4",
                    textColor = "#1A6B8A",
                    tags = "Fios,Fusiveis,Manutenção,Montagem",
                    contact = "+244 923 333 003",
                    verified = false,
                    ratingAvg = 4.7f,
                    ratingCount = 21
                ),
                Biscateiro(
                    name = "Pedro Luvualu",
                    role = "Mestre de Obras & Pedreiro",
                    category = "Construção",
                    bairro = "Cazenga",
                    rate = "30.000 Kz/dia",
                    desc = "Alvenaria pesada, betonilha, reboco fino, assentamento de mosaicos e tijolos. Trabalho sério e com prazos respeitados. Coordeno equipe se necessário.",
                    initials = "PL",
                    bgColor = "#EDD9C0",
                    textColor = "#7B3F00",
                    tags = "Pedreiro,Alvenaria,Mosaico,Tijolos",
                    contact = "+244 923 444 004",
                    verified = true,
                    ratingAvg = 4.8f,
                    ratingCount = 28
                ),
                Biscateiro(
                    name = "Filomena Neto",
                    role = "Canalizadora Hidráulica",
                    category = "Canalização",
                    bairro = "Sambizanga",
                    rate = "18.000 Kz/dia",
                    desc = "Detecção de infiltrações, desentupimentos rápidos, instalação de louças sanitárias, torneiras e esgotos domésticos. Disponível para fins de semana.",
                    initials = "FN",
                    bgColor = "#F2DBC8",
                    textColor = "#8B4513",
                    tags = "Cano,Pias,Infiltração,Torneiras",
                    contact = "+244 923 555 005",
                    verified = true,
                    ratingAvg = 4.6f,
                    ratingCount = 19
                ),
                Biscateiro(
                    name = "Domingos Carvalho",
                    role = "Carpinteiro & Marceneiro",
                    category = "Carpintaria",
                    bairro = "Maianga",
                    rate = "22.000 Kz/dia",
                    desc = "Móveis planejados sob medida, portas, janelas, reparação de decks e decks de madeira maciça. Restauro mobiliário desgastado.",
                    initials = "DC",
                    bgColor = "#DDD0F5",
                    textColor = "#4A0E8F",
                    tags = "Móveis,Madeira,Portas,Restauro",
                    contact = "+244 923 666 006",
                    verified = false,
                    ratingAvg = 4.8f,
                    ratingCount = 15
                )
            )

            for (worker in seedWorkers) {
                appDao.insertBiscateiro(worker)
            }

            // Seed initial active tasks for clients
            val seedTasks = listOf(
                JobTask(
                    title = "Pintura Completa de T3",
                    description = "Preciso de pintar um apartamento T3 completo em Talatona, tetos inclusive. Já comprei as tintas (Cin branca). Necessário trazer lixas, rolos e fita de proteção. Trabalho limpo e caprichado.",
                    category = "Pintura",
                    location = "Talatona",
                    budget = 35000.0,
                    duration = "3-4 dias",
                    clientName = "Mateus Pedro",
                    clientContact = "+244 924 999 011",
                    status = "ABERTO"
                ),
                JobTask(
                    title = "Instalação de Torneiras de Cozinha e Lavatório",
                    description = "As minhas torneiras estão antigas e com fugas constantes. Preciso de canalizador especialista para as substituir por novas (já compradas) na cozinha e num lavatório de WC do apartamento.",
                    category = "Canalização",
                    location = "Kilamba",
                    budget = 15000.0,
                    duration = "1 dia",
                    clientName = "Sofia Bento",
                    clientContact = "+244 924 999 022",
                    status = "ABERTO"
                ),
                JobTask(
                    title = "Instalação Elétrica de Cozinha com Fornos",
                    description = "Desejo reconfigurar a fiação da cozinha para um disjuntor reforçado de forma a suportar forno de encastrar e micro-ondas novos sem disparar o quadro primário.",
                    category = "Electricidade",
                    location = "Viana",
                    budget = 22000.0,
                    duration = "2 dias",
                    clientName = "Carlos Alberto",
                    clientContact = "+244 924 999 033",
                    status = "ABERTO"
                ),
                JobTask(
                    title = "Reboco exterior de parede lateral",
                    description = "Preciso de um mestre pedreiro ou ajudantes para rebocar uma face de muro e aplicar cimento areado numa parede lateral da residência com aproximadamente 15 metros quadrados.",
                    category = "Construção",
                    location = "Cazenga",
                    budget = 60000.0,
                    duration = "1 semana",
                    clientName = "Isabel Lucas",
                    clientContact = "+244 924 999 044",
                    status = "ABERTO"
                )
            )

            for (task in seedTasks) {
                appDao.insertTask(task)
            }
        }
    }
}
