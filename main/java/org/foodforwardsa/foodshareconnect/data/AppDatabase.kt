package org.foodforwardsa.foodshareconnect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.data.dao.BeneficiaryOrganisationDao
import org.foodforwardsa.foodshareconnect.data.dao.FoodDonationDao
import org.foodforwardsa.foodshareconnect.data.dao.FoodRequestDao
import org.foodforwardsa.foodshareconnect.data.dao.UserDao
import org.foodforwardsa.foodshareconnect.data.entity.BeneficiaryOrganisation
import org.foodforwardsa.foodshareconnect.data.entity.FoodDonation
import org.foodforwardsa.foodshareconnect.data.entity.FoodRequest
import org.foodforwardsa.foodshareconnect.data.entity.User
import org.foodforwardsa.foodshareconnect.util.PasswordHasher

@Database(
    entities = [User::class, BeneficiaryOrganisation::class, FoodDonation::class, FoodRequest::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun beneficiaryOrganisationDao(): BeneficiaryOrganisationDao
    abstract fun foodDonationDao(): FoodDonationDao
    abstract fun foodRequestDao(): FoodRequestDao

    companion object {
        private const val DEFAULT_ADMIN_EMAIL = "admin@foodforwardsa.org.za"
        private const val DEFAULT_ADMIN_PASSWORD = "ChangeMe!2026"

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "foodshareconnect.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed a working Admin login on first run, since Admin accounts
                        // are intentionally not self-registrable from the public app.
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            val existing = database.userDao().findByEmail(DEFAULT_ADMIN_EMAIL)
                            if (existing == null) {
                                val (hash, salt) = PasswordHasher.hash(DEFAULT_ADMIN_PASSWORD)
                                database.userDao().insert(
                                    User(
                                        email = DEFAULT_ADMIN_EMAIL,
                                        passwordHash = hash,
                                        passwordSalt = salt,
                                        displayName = "FoodForward SA Admin",
                                        role = UserRole.ADMIN,
                                        twoFactorEnabled = false
                                    )
                                )
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
    }
}
