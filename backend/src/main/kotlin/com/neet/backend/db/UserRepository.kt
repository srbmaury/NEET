package com.neet.backend.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class UserRow(val id: Uuid, val email: String, val passwordHash: String)

@OptIn(ExperimentalUuidApi::class)
class UserRepository {

    suspend fun create(email: String, passwordHash: String): Uuid = withContext(Dispatchers.IO) {
        transaction {
            val id = Uuid.random()
            Users.insert {
                it[Users.id] = id
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
                it[createdAt] = Instant.now()
            }
            id
        }
    }

    suspend fun findByEmail(email: String): UserRow? = withContext(Dispatchers.IO) {
        transaction {
            Users.selectAll()
                .where { Users.email eq email }
                .orderBy(Users.createdAt, SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { UserRow(it[Users.id], it[Users.email], it[Users.passwordHash]) }
        }
    }
}
