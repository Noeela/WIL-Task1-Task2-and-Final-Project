package org.foodforwardsa.foodshareconnect.ui.beneficiary

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.FoodCategory
import org.foodforwardsa.foodshareconnect.data.display
import org.foodforwardsa.foodshareconnect.data.repository.FoodRequestRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityCreateFoodRequestBinding
import org.foodforwardsa.foodshareconnect.session.SessionManager
import org.foodforwardsa.foodshareconnect.util.OperationResult
import org.foodforwardsa.foodshareconnect.util.ValidationUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateFoodRequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateFoodRequestBinding
    private lateinit var requestRepository: FoodRequestRepository
    private lateinit var session: SessionManager
    private var selectedDateMillis: Long? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateFoodRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "New Food Request"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        requestRepository = FoodRequestRepository(AppDatabase.getInstance(this))
        session = SessionManager(this)

        binding.spinnerCategory.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            FoodCategory.entries.map { it.display() }
        )

        binding.buttonPickDate.setOnClickListener { showDatePicker() }
        binding.buttonSubmit.setOnClickListener { attemptSubmit() }

        // Default the needed-by date to 3 days from now, matching the web app's default.
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 3) }
        selectedDateMillis = cal.timeInMillis
        binding.buttonPickDate.text = dateFormat.format(cal.time)
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        selectedDateMillis?.let { cal.timeInMillis = it }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                selectedDateMillis = picked.timeInMillis
                binding.buttonPickDate.text = dateFormat.format(picked.time)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        }.show()
    }

    private fun attemptSubmit() {
        hideError()

        val orgId = session.beneficiaryOrgId ?: run {
            showError("Your account is not linked to an organisation. Please contact FoodForward SA."); return
        }
        val categoryIndex = binding.spinnerCategory.selectedItemPosition
        if (categoryIndex < 0) { showError("Please select a food category."); return }
        val category = FoodCategory.entries[categoryIndex]

        val quantity = ValidationUtils.parsePositiveDouble(binding.inputQuantity.text?.toString().orEmpty(), 5000.0)
            ?: run { showError("Please enter a valid quantity between 0.1 and 5000 kg."); return }

        val beneficiaries = ValidationUtils.parsePositiveInt(binding.inputBeneficiaries.text?.toString().orEmpty(), 100_000)
            ?: run { showError("Please enter a valid number of beneficiaries."); return }

        val neededBy = selectedDateMillis
        if (neededBy == null || neededBy <= System.currentTimeMillis()) {
            showError("Needed-by date must be in the future."); return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = requestRepository.submitRequest(
                    beneficiaryOrgId = orgId,
                    category = category,
                    requestedQuantityKg = quantity,
                    numberOfBeneficiaries = beneficiaries,
                    neededByEpochMillis = neededBy,
                    motivation = binding.inputMotivation.text?.toString()?.trim()
                )
                when (result) {
                    is OperationResult.Success -> {
                        Toast.makeText(this@CreateFoodRequestActivity, "Request submitted successfully.", Toast.LENGTH_LONG).show()
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

    private fun hideError() {
        binding.textError.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSubmit.isEnabled = !loading
    }
}
