package org.foodforwardsa.foodshareconnect.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.display
import org.foodforwardsa.foodshareconnect.data.entity.FoodDonation
import org.foodforwardsa.foodshareconnect.data.entity.FoodRequestWithOrgName
import org.foodforwardsa.foodshareconnect.data.repository.DonationRepository
import org.foodforwardsa.foodshareconnect.data.repository.FoodRequestRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityReviewFoodRequestBinding
import org.foodforwardsa.foodshareconnect.util.OperationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewFoodRequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewFoodRequestBinding
    private lateinit var requestRepository: FoodRequestRepository
    private lateinit var donationRepository: DonationRepository
    private var requestId: Long = -1
    private var matchingDonations: List<FoodDonation> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewFoodRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Review Request"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        val db = AppDatabase.getInstance(this)
        requestRepository = FoodRequestRepository(db)
        donationRepository = DonationRepository(db)

        requestId = intent.getLongExtra(EXTRA_REQUEST_ID, -1)
        if (requestId == -1L) { finish(); return }

        binding.buttonApprove.setOnClickListener { attemptApprove() }
        binding.buttonDecline.setOnClickListener { attemptDecline() }

        loadRequest()
    }

    private fun loadRequest() {
        lifecycleScope.launch {
            val request = requestRepository.getWithOrgName(requestId)
            if (request == null) {
                Toast.makeText(this@ReviewFoodRequestActivity, "Request not found.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            bind(request)

            matchingDonations = donationRepository.findMatching(request.category, request.requestedQuantityKg)
            if (matchingDonations.isEmpty()) {
                binding.textNoStockWarning.visibility = View.VISIBLE
                binding.spinnerDonation.visibility = View.GONE
                binding.buttonApprove.isEnabled = false
            } else {
                binding.textNoStockWarning.visibility = View.GONE
                binding.spinnerDonation.visibility = View.VISIBLE
                binding.buttonApprove.isEnabled = true
                binding.spinnerDonation.adapter = ArrayAdapter(
                    this@ReviewFoodRequestActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    matchingDonations.map { "${it.donorName} · ${it.remainingQuantityKg}kg remaining · expires ${dateFormat.format(Date(it.expiryDateEpochMillis))}" }
                )
            }
        }
    }

    private fun bind(request: FoodRequestWithOrgName) {
        binding.textOrgName.text = "Organisation: ${request.organisationName}"
        binding.textCategory.text = "Category: ${request.category.display()}"
        binding.textQuantity.text = "Requested quantity: ${request.requestedQuantityKg} kg"
        binding.textBeneficiaries.text = "Beneficiaries: ${request.numberOfBeneficiaries}"
        binding.textNeededBy.text = "Needed by: ${dateFormat.format(Date(request.neededByEpochMillis))}"
        if (!request.motivation.isNullOrBlank()) {
            binding.textMotivation.visibility = View.VISIBLE
            binding.textMotivation.text = "Motivation: ${request.motivation}"
        }
    }

    private fun attemptApprove() {
        val selectedIndex = binding.spinnerDonation.selectedItemPosition
        if (selectedIndex < 0 || selectedIndex >= matchingDonations.size) {
            showError("Please select a donation to link before approving this request."); return
        }
        val donationId = matchingDonations[selectedIndex].id
        val note = binding.inputNote.text?.toString()?.trim()

        setLoading(true)
        lifecycleScope.launch {
            try {
                when (val result = requestRepository.approve(requestId, donationId, note)) {
                    is OperationResult.Success -> {
                        Toast.makeText(this@ReviewFoodRequestActivity, "Request approved.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is OperationResult.Failure -> showError(result.message)
                }
            } catch (e: Exception) {
                showError("An unexpected error occurred while approving this request. Please try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun attemptDecline() {
        val note = binding.inputNote.text?.toString()?.trim()
        setLoading(true)
        lifecycleScope.launch {
            try {
                when (val result = requestRepository.decline(requestId, note)) {
                    is OperationResult.Success -> {
                        Toast.makeText(this@ReviewFoodRequestActivity, "Request declined.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is OperationResult.Failure -> showError(result.message)
                }
            } catch (e: Exception) {
                showError("An unexpected error occurred. Please try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonApprove.isEnabled = !loading && matchingDonations.isNotEmpty()
        binding.buttonDecline.isEnabled = !loading
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
