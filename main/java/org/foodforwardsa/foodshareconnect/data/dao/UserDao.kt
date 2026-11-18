package org.foodforwardsa.foodshareconnect.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.foodforwardsa.foodshareconnect.data.entity.User

@Dao
interface UserDao {

    // All parameters below are bound placeholders, not string-concatenated SQL,
    // so this query (and every other one in this file) is not vulnerable to
    // SQL injection - Room compiles and parameterises it at build time.
    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): User?

    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)
}
