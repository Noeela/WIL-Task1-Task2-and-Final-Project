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
import org.foodforwardsa.foodshareconnect.data.VettingStatus
import org.foodforwardsa.foodshareconnect.data.display
import org.foodforwardsa.foodshareconnect.data.entity.BeneficiaryOrganisation
import org.foodforwardsa.foodshareconnect.data.repository.BeneficiaryOrgRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityVetOrganisationBinding
import org.foodforwardsa.foodshareconnect.util.OperationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VetOrganisationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVetOrganisationBinding
    private lateinit var repository: BeneficiaryOrgRepository
    private var orgId: Long = -1
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVetOrganisationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Vet Organisation"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        repository = BeneficiaryOrgRepository(AppDatabase.getInstance(this))
        binding.spinnerStatus.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            VettingStatus.entries.map { it.display() }
        )

        orgId = intent.getLongExtra(EXTRA_ORG_ID, -1)
        if (orgId == -1L) { finish(); return }

        binding.buttonSave.setOnClickListener { attemptSave() }
        loadOrg()
    }

    private fun loadOrg() {
        lifecycleScope.launch {
            val org = repository.getById(orgId)
            if (org == null) {
                Toast.makeText(this@VetOrganisationActivity, "Organisation not found.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            bind(org)
        }
    }

    private fun bind(org: BeneficiaryOrganisation) {
        binding.textOrgName.text = org.organisationName
        binding.textStatusBadge.text = org.vettingStatus.display()
        binding.textStatusBadge.background.mutate().setTint(statusColor(org.vettingStatus))
        binding.textContact.text = "Contact person: ${org.contactPersonName}"
        binding.textEmail.text = "Email: ${org.email}"
        binding.textPhone.text = "Phone: ${org.phoneNumber}"
        binding.textAddress.text = "Address: ${org.physicalAddress}, ${org.province}"
        binding.textNpoRegNo.text = "NPO registration no.: ${org.npoRegistrationNumber}"
        binding.textBeneficiaries.text = "Estimated beneficiaries: ${org.estimatedBeneficiaries}"
        binding.textRegisteredDate.text = "Registered: ${dateFormat.format(Date(org.registeredAtEpochMillis))}"
        binding.spinnerStatus.setSelection(VettingStatus.entries.indexOf(org.vettingStatus))
        binding.inputNote.setText(org.adminNote.orEmpty())
    }

    private fun attemptSave() {
        binding.textError.visibility = View.GONE
        val newStatus = VettingStatus.entries[binding.spinnerStatus.selectedItemPosition]
        val note = binding.inputNote.text?.toString()?.trim()

        setLoading(true)
        lifecycleScope.launch {
            try {
                when (val result = repository.updateVettingStatus(orgId, newStatus, note)) {
                    is OperationResult.Success -> {
                        Toast.makeText(this@VetOrganisationActivity, "Organisation updated.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is OperationResult.Failure -> {
                        binding.textError.text = result.message
                        binding.textError.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                binding.textError.text = "Something went wrong while saving. Please try again."
                binding.textError.visibility = View.VISIBLE
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSave.isEnabled = !loading
    }

    private fun statusColor(status: VettingStatus): Int = android.graphics.Color.parseColor(
        when (status) {
            VettingStatus.PENDING_REVIEW -> "#FFB300"
            VettingStatus.APPROVED -> "#2E7D32"
            VettingStatus.REJECTED -> "#C62828"
            VettingStatus.SUSPENDED -> "#757575"
        }
    )

    companion object {
        const val EXTRA_ORG_ID = "extra_org_id"
    }
}
