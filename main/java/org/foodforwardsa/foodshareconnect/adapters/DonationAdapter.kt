package org.foodforwardsa.foodshareconnect.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.foodforwardsa.foodshareconnect.data.display
import org.foodforwardsa.foodshareconnect.data.entity.FoodDonation
import org.foodforwardsa.foodshareconnect.databinding.ItemDonationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DonationAdapter(private var items: List<FoodDonation> = emptyList()) :
    RecyclerView.Adapter<DonationAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun submitList(newItems: List<FoodDonation>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemDonationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(donation: FoodDonation) {
            binding.textDonor.text = donation.donorName
            binding.textCategory.text = donation.category.display()
            binding.textQuantity.text = "${donation.remainingQuantityKg} kg remaining / ${donation.quantityKg} kg"

            val daysToExpiry = TimeUnit.MILLISECONDS.toDays(donation.expiryDateEpochMillis - System.currentTimeMillis())
            val expiryText = "Expires: ${dateFormat.format(Date(donation.expiryDateEpochMillis))}"
            binding.textExpiry.text = if (daysToExpiry <= 2) "$expiryText ⚠️ expiring soon" else expiryText
        }
    }
}
