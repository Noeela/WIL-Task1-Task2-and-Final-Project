package org.foodforwardsa.foodshareconnect.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.foodforwardsa.foodshareconnect.data.RequestStatus
import org.foodforwardsa.foodshareconnect.data.display
import org.foodforwardsa.foodshareconnect.data.entity.FoodRequestWithOrgName
import org.foodforwardsa.foodshareconnect.databinding.ItemFoodRequestAdminBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminFoodRequestAdapter(
    private var items: List<FoodRequestWithOrgName> = emptyList(),
    private val onClick: (FoodRequestWithOrgName) -> Unit
) : RecyclerView.Adapter<AdminFoodRequestAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun submitList(newItems: List<FoodRequestWithOrgName>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodRequestAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemFoodRequestAdminBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: FoodRequestWithOrgName) {
            binding.textOrgName.text = request.organisationName
            binding.textStatus.text = request.status.display()
            binding.textStatus.background.mutate().setTint(statusColor(request.status))
            binding.textDetails.text = "${request.category.display()} · ${request.requestedQuantityKg} kg for ${request.numberOfBeneficiaries} people"
            binding.textNeededBy.text = "Needed by: ${dateFormat.format(Date(request.neededByEpochMillis))}"
            binding.root.setOnClickListener { onClick(request) }
        }

        private fun statusColor(status: RequestStatus): Int = Color.parseColor(
            when (status) {
                RequestStatus.SUBMITTED -> "#FFB300"
                RequestStatus.APPROVED -> "#2E7D32"
                RequestStatus.DECLINED -> "#C62828"
                RequestStatus.CANCELLED -> "#757575"
            }
        )
    }
}
