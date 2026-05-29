package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_tasks")
data class JobTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val budget: Double,
    val duration: String,
    val clientName: String,
    val clientContact: String,
    val status: String = "ABERTO", // "ABERTO", "EM_CURSO", "CONCLUIDO"
    val assignedWorkerId: Int? = null,
    val rating: Int? = null,
    val feedback: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "biscateiros")
data class Biscateiro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val category: String, // e.g. "Pintura", "Limpeza", "Electricidade", "Construção", "Canalização", "Carpintaria", "Outros"
    val bairro: String,
    val rate: String,
    val desc: String,
    val initials: String,
    val bgColor: String,
    val textColor: String,
    val tags: String, // Delimited by comma, e.g. "Interior,Exterior,Mural"
    val contact: String,
    val verified: Boolean = false,
    val ratingAvg: Float = 4.8f,
    val ratingCount: Int = 12
)

@Entity(tableName = "candidaturas")
data class Candidatura(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val workerId: Int,
    val workerName: String,
    val workerInitials: String,
    val priceProposal: String,
    val message: String,
    val status: String = "PENDENTE", // "PENDENTE", "ACEITA", "REJEITADA"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val sender: String, // "CLIENTE" or "PRESTADOR"
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val id: Int = 1, // Single active session
    val userRole: String = "CLIENTE", // "CLIENTE" or "PRESTADOR"
    val name: String = "João Baptista",
    val contact: String = "+244 923 456 789",
    val walletCredits: Int = 50, // Initial free credits gift!
    val selectedCategory: String = "Todos"
)
