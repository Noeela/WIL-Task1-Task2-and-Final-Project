package org.foodforwardsa.foodshareconnect.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.adapters.DonationAdapter
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.UserRole
import org.foodforwardsa.foodshareconnect.data.repository.DonationRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityAdminDonationsBinding
import org.foodforwardsa.foodshareconnect.session.SessionManager

class AdminDonationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDonationsBinding
    private lateinit var repository: DonationRepository
    private val adapter = DonationAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDonationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Available Stock"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        repository = DonationRepository(AppDatabase.getInstance(this))
        binding.recyclerDonations.layoutManager = LinearLayoutManager(this)
        binding.recyclerDonations.adapter = adapter

        // Only FoodForward SA Admin staff can capture new stock - Beneficiary
        // Organisations can view this same screen read-only.
        val isAdmin = SessionManager(this).role == UserRole.ADMIN
        binding.fabAddDonation.visibility = if (isAdmin) View.VISIBLE else View.GONE

        binding.fabAddDonation.setOnClickListener {
            startActivity(Intent(this, CreateDonationActivity::class.java))
        }
        binding.swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                repository.observeAvailable().collect { donations ->
                    adapter.submitList(donations)
                    binding.textEmptyState.visibility = if (donations.isEmpty()) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
