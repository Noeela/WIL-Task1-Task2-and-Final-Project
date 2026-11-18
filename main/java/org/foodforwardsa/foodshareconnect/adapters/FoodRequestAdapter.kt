package org.foodforwardsa.foodshareconnect.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.foodforwardsa.foodshareconnect.data.RequestStatus
import org.foodforwardsa.foodshareconnect.data.display
import org.foodforwardsa.foodshareconnect.data.entity.FoodRequest
import org.foodforwardsa.foodshareconnect.databinding.ItemFoodRequestBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodRequestAdapter(private var items: List<FoodRequest> = emptyList()) :
    RecyclerView.Adapter<FoodRequestAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun submitList(newItems: List<FoodRequest>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemFoodRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: FoodRequest) {
            binding.textCategory.text = request.category.display()
            binding.textStatus.text = request.status.display()
            binding.textStatus.background.mutate().setTint(statusColor(request.status))
            binding.textQuantity.text = "${request.requestedQuantityKg} kg requested for ${request.numberOfBeneficiaries} people"
            binding.textNeededBy.text = "Needed by: ${dateFormat.format(Date(request.neededByEpochMillis))}"

            if (!request.adminNote.isNullOrBlank()) {
                binding.textAdminNote.visibility = android.view.View.VISIBLE
                binding.textAdminNote.text = "Note: ${request.adminNote}"
            } else {
                binding.textAdminNote.visibility = android.view.View.GONE
            }
        }

        private fun statusColor(status: RequestStatus): Int = when (status) {
            RequestStatus.SUBMITTED -> Color.parseColor("#FFB300")
            RequestStatus.APPROVED -> Color.parseColor("#2E7D32")
            RequestStatus.DECLINED -> Color.parseColor("#C62828")
            RequestStatus.CANCELLED -> Color.parseColor("#757575")
        }
    }
}
