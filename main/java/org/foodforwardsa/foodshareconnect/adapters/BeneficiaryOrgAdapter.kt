package org.foodforwardsa.foodshareconnect.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.foodforwardsa.foodshareconnect.data.VettingStatus
import org.foodforwardsa.foodshareconnect.data.display
import org.foodforwardsa.foodshareconnect.data.entity.BeneficiaryOrganisation
import org.foodforwardsa.foodshareconnect.databinding.ItemBeneficiaryOrgBinding

class BeneficiaryOrgAdapter(
    private var items: List<BeneficiaryOrganisation> = emptyList(),
    private val onClick: (BeneficiaryOrganisation) -> Unit
) : RecyclerView.Adapter<BeneficiaryOrgAdapter.ViewHolder>() {

    fun submitList(newItems: List<BeneficiaryOrganisation>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBeneficiaryOrgBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemBeneficiaryOrgBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(org: BeneficiaryOrganisation) {
            binding.textOrgName.text = org.organisationName
            binding.textStatus.text = org.vettingStatus.display()
            binding.textStatus.background.mutate().setTint(statusColor(org.vettingStatus))
            binding.textContact.text = "Contact: ${org.contactPersonName}"
            binding.textProvinceEmail.text = "${org.province} · ${org.email}"
            binding.root.setOnClickListener { onClick(org) }
        }

        private fun statusColor(status: VettingStatus): Int = Color.parseColor(
            when (status) {
                VettingStatus.PENDING_REVIEW -> "#FFB300"
                VettingStatus.APPROVED -> "#2E7D32"
                VettingStatus.REJECTED -> "#C62828"
                VettingStatus.SUSPENDED -> "#757575"
            }
        )
    }
}
