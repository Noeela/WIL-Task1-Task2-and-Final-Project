package org.foodforwardsa.foodshareconnect.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.adapters.AdminFoodRequestAdapter
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.RequestStatus
import org.foodforwardsa.foodshareconnect.data.repository.FoodRequestRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityAdminFoodRequestsBinding

class AdminFoodRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminFoodRequestsBinding
    private lateinit var repository: FoodRequestRepository
    private lateinit var adapter: AdminFoodRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminFoodRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Food Requests"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        repository = FoodRequestRepository(AppDatabase.getInstance(this))
        adapter = AdminFoodRequestAdapter { request ->
            if (request.status == RequestStatus.SUBMITTED) {
                startActivity(Intent(this, ReviewFoodRequestActivity::class.java).apply {
                    putExtra(ReviewFoodRequestActivity.EXTRA_REQUEST_ID, request.id)
                })
            }
        }
        binding.recyclerRequests.layoutManager = LinearLayoutManager(this)
        binding.recyclerRequests.adapter = adapter

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ -> loadData() }
        binding.swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val status = when {
            binding.chipApproved.isChecked -> RequestStatus.APPROVED
            binding.chipDeclined.isChecked -> RequestStatus.DECLINED
            else -> RequestStatus.SUBMITTED
        }

        lifecycleScope.launch {
            try {
                repository.observeByStatusWithOrgName(status).collect { requests ->
                    adapter.submitList(requests)
                    binding.textEmptyState.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
