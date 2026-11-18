package org.foodforwardsa.foodshareconnect.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import org.foodforwardsa.foodshareconnect.data.UserRole
import org.foodforwardsa.foodshareconnect.databinding.ActivitySplashBinding
import org.foodforwardsa.foodshareconnect.session.SessionManager
import org.foodforwardsa.foodshareconnect.ui.admin.AdminDashboardActivity
import org.foodforwardsa.foodshareconnect.ui.auth.LoginActivity
import org.foodforwardsa.foodshareconnect.ui.beneficiary.BeneficiaryDashboardActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Short branded pause, then route based on whatever session is saved -
        // wrapped so a failure here can never leave the user stuck on a blank screen.
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                routeNext()
            } catch (e: Exception) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }, 900)
    }

    private fun routeNext() {
        val session = SessionManager(this)
        val next = if (!session.isLoggedIn) {
            LoginActivity::class.java
        } else when (session.role) {
            UserRole.ADMIN -> AdminDashboardActivity::class.java
            UserRole.BENEFICIARY_ORG -> BeneficiaryDashboardActivity::class.java
            null -> LoginActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}
