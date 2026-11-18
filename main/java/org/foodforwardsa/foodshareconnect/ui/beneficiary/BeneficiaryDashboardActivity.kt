package org.foodforwardsa.foodshareconnect.ui.beneficiary

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.R
import org.foodforwardsa.foodshareconnect.adapters.FoodRequestAdapter
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.VettingStatus
import org.foodforwardsa.foodshareconnect.data.display
import org.foodforwardsa.foodshareconnect.data.repository.BeneficiaryOrgRepository
import org.foodforwardsa.foodshareconnect.data.repository.FoodRequestRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityBeneficiaryDashboardBinding
import org.foodforwardsa.foodshareconnect.session.SessionManager
import org.foodforwardsa.foodshareconnect.ui.auth.LoginActivity
import org.foodforwardsa.foodshareconnect.ui.contact.ContactUsActivity

class BeneficiaryDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBeneficiaryDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var orgRepository: BeneficiaryOrgRepository
    private lateinit var requestRepository: FoodRequestRepository
    private val adapter = FoodRequestAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBeneficiaryDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "My Organisation"

        session = SessionManager(this)
        if (!session.isLoggedIn) { redirectToLogin(); return }

        val db = AppDatabase.getInstance(this)
        orgRepository = BeneficiaryOrgRepository(db)
        requestRepository = FoodRequestRepository(db)

        binding.recyclerRequests.layoutManager = LinearLayoutManager(this)
        binding.recyclerRequests.adapter = adapter

        binding.buttonNewRequest.setOnClickListener {
            startActivity(Intent(this, CreateFoodRequestActivity::class.java))
        }
        binding.swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val orgId = session.beneficiaryOrgId ?: run {
            binding.swipeRefresh.isRefreshing = false
            return
        }

        lifecycleScope.launch {
            try {
                val org = orgRepository.getById(orgId)
                if (org == null) {
                    binding.textOrgName.text = "Organisation not found"
                    return@launch
                }

                binding.textOrgName.text = org.organisationName
                binding.textOrgStatus.text = org.vettingStatus.display()
                binding.textOrgStatus.background.mutate().setTint(statusColor(org.vettingStatus))

                if (org.vettingStatus != VettingStatus.APPROVED) {
                    binding.textOrgHint.visibility = View.VISIBLE
                    binding.textOrgHint.text =
                        "Your organisation is not yet approved by FoodForward SA, so you can't submit food requests yet. " +
                        "You'll be able to once your registration has been reviewed."
                    binding.buttonNewRequest.isEnabled = false
                } else {
                    binding.textOrgHint.visibility = View.GONE
                    binding.buttonNewRequest.isEnabled = true
                }

                requestRepository.observeForOrganisation(orgId).collect { requests ->
                    adapter.submitList(requests)
                    binding.textEmptyState.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun statusColor(status: VettingStatus): Int = android.graphics.Color.parseColor(
        when (status) {
            VettingStatus.PENDING_REVIEW -> "#FFB300"
            VettingStatus.APPROVED -> "#2E7D32"
            VettingStatus.REJECTED -> "#C62828"
            VettingStatus.SUSPENDED -> "#757575"
        }
    )

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_beneficiary, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_stock -> {
                startActivity(Intent(this, org.foodforwardsa.foodshareconnect.ui.admin.AdminDonationsActivity::class.java)); true
            }
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
