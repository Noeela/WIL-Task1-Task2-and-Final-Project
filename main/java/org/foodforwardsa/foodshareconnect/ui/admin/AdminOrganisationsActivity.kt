package org.foodforwardsa.foodshareconnect.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.adapters.BeneficiaryOrgAdapter
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.VettingStatus
import org.foodforwardsa.foodshareconnect.data.repository.BeneficiaryOrgRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityAdminOrganisationsBinding

class AdminOrganisationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminOrganisationsBinding
    private lateinit var repository: BeneficiaryOrgRepository
    private lateinit var adapter: BeneficiaryOrgAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrganisationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Beneficiary Organisations"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        repository = BeneficiaryOrgRepository(AppDatabase.getInstance(this))
        adapter = BeneficiaryOrgAdapter { org ->
            startActivity(Intent(this, VetOrganisationActivity::class.java).apply {
                putExtra(VetOrganisationActivity.EXTRA_ORG_ID, org.id)
            })
        }
        binding.recyclerOrgs.layoutManager = LinearLayoutManager(this)
        binding.recyclerOrgs.adapter = adapter

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ -> loadData() }
        binding.swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val flow = when {
            binding.chipPending.isChecked -> repository.observeByStatus(VettingStatus.PENDING_REVIEW)
            binding.chipApproved.isChecked -> repository.observeByStatus(VettingStatus.APPROVED)
            binding.chipRejected.isChecked -> repository.observeByStatus(VettingStatus.REJECTED)
            else -> repository.observeAll()
        }

        lifecycleScope.launch {
            try {
                flow.collect { orgs ->
                    adapter.submitList(orgs)
                    binding.textEmptyState.visibility = if (orgs.isEmpty()) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
