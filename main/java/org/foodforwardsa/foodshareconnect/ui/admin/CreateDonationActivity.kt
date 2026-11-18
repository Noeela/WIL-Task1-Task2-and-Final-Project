package org.foodforwardsa.foodshareconnect.ui.admin

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
import org.foodforwardsa.foodshareconnect.data.repository.DonationRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityCreateDonationBinding
import org.foodforwardsa.foodshareconnect.util.OperationResult
import org.foodforwardsa.foodshareconnect.util.ValidationUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateDonationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateDonationBinding
    private lateinit var repository: DonationRepository
    private var selectedDateMillis: Long? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateDonationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Capture Donation"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        repository = DonationRepository(AppDatabase.getInstance(this))
        binding.spinnerCategory.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            FoodCategory.entries.map { it.display() }
        )

        binding.buttonPickDate.setOnClickListener { showDatePicker() }
        binding.buttonSave.setOnClickListener { attemptSave() }

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
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

    private fun attemptSave() {
        hideError()

        val donorName = binding.inputDonorName.text?.toString()?.trim().orEmpty()
        if (donorName.isBlank()) { showError("Please enter the donor's name."); return }

        val categoryIndex = binding.spinnerCategory.selectedItemPosition
        if (categoryIndex < 0) { showError("Please select a category."); return }
        val category = FoodCategory.entries[categoryIndex]

        val quantity = ValidationUtils.parsePositiveDouble(binding.inputQuantity.text?.toString().orEmpty(), 50000.0)
            ?: run { showError("Please enter a valid quantity between 0.1 and 50,000 kg."); return }

        val expiry = selectedDateMillis
        if (expiry == null || expiry <= System.currentTimeMillis()) {
            showError("Expiry date must be in the future."); return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = repository.createDonation(
                    donorName = donorName,
                    category = category,
                    quantityKg = quantity,
                    expiryDateEpochMillis = expiry,
                    notes = binding.inputNotes.text?.toString()?.trim()
                )
                when (result) {
                    is OperationResult.Success -> {
                        Toast.makeText(this@CreateDonationActivity, "Donation captured successfully.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    is OperationResult.Failure -> showError(result.message)
                }
            } catch (e: Exception) {
                showError("Something went wrong while saving this donation. Please try again.")
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
        binding.buttonSave.isEnabled = !loading
    }
}
