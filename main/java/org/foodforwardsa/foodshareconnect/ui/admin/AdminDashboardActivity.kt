package org.foodforwardsa.foodshareconnect.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.R
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.repository.BeneficiaryOrgRepository
import org.foodforwardsa.foodshareconnect.data.repository.DonationRepository
import org.foodforwardsa.foodshareconnect.data.repository.FoodRequestRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityAdminDashboardBinding
import org.foodforwardsa.foodshareconnect.session.SessionManager
import org.foodforwardsa.foodshareconnect.ui.auth.EnableAuthenticatorActivity
import org.foodforwardsa.foodshareconnect.ui.auth.LoginActivity
import org.foodforwardsa.foodshareconnect.ui.contact.ContactUsActivity

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var orgRepository: BeneficiaryOrgRepository
    private lateinit var requestRepository: FoodRequestRepository
    private lateinit var donationRepository: DonationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Admin Dashboard"

        session = SessionManager(this)
        if (!session.isLoggedIn) { redirectToLogin(); return }

        val db = AppDatabase.getInstance(this)
        orgRepository = BeneficiaryOrgRepository(db)
        requestRepository = FoodRequestRepository(db)
        donationRepository = DonationRepository(db)

        binding.buttonReviewOrgs.setOnClickListener {
            startActivity(Intent(this, AdminOrganisationsActivity::class.java))
        }
        binding.buttonReviewRequests.setOnClickListener {
            startActivity(Intent(this, AdminFoodRequestsActivity::class.java))
        }
        binding.buttonManageStock.setOnClickListener {
            startActivity(Intent(this, AdminDonationsActivity::class.java))
        }
        binding.buttonSetup2fa.setOnClickListener {
            startActivity(Intent(this, EnableAuthenticatorActivity::class.java))
        }
        binding.swipeRefresh.setOnRefreshListener { loadCounts() }

        loadCounts()
    }

    override fun onResume() {
        super.onResume()
        loadCounts()
    }

    private fun loadCounts() {
        lifecycleScope.launch {
            try {
                binding.textPendingCount.text = orgRepository.countPending().toString()
                binding.textApprovedCount.text = orgRepository.countApproved().toString()
                binding.textSubmittedCount.text = requestRepository.countSubmitted().toString()
                val stock = donationRepository.totalAvailableStockKg()
                binding.textStockKg.text = "${"%.0f".format(stock)} kg"
            } catch (e: Exception) {
                // Swallow and leave previous values on screen rather than crash the dashboard.
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_contact -> {
                startActivity(Intent(this, ContactUsActivity::class.java)); true
            }
            R.id.action_logout -> {
                session.clear()
                redirectToLogin(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
