package org.foodforwardsa.foodshareconnect.ui.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import org.foodforwardsa.foodshareconnect.databinding.ActivityContactUsBinding

/**
 * Real FoodForward SA contact details (sourced from foodforwardsa.org/contact-us):
 * National office +27 21 531 5670 / info@foodforwardsa.org, plus all five
 * regional branch numbers. Every phone/email row is tap-to-call / tap-to-email.
 */
class ContactUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Contact FoodForward SA"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        wireCall(binding.textNationalPhone)
        wireCall(binding.textWcPhone)
        wireCall(binding.textGpPhone)
        wireCall(binding.textKznPhone)
        wireCall(binding.textFsPhone)
        wireCall(binding.textEcPhone)
        wireEmail(binding.textNationalEmail)
    }

    private fun wireCall(view: TextView) {
        view.setOnClickListener {
            try {
                val number = view.text.toString()
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            } catch (e: Exception) {
                // No dialler app available on this device/emulator - fail silently
                // rather than crash, since the number is still visible to copy manually.
            }
        }
    }

    private fun wireEmail(view: TextView) {
        view.setOnClickListener {
            try {
                val email = view.text.toString()
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
            } catch (e: Exception) {
                // No mail app available - fail silently, the address is still visible to copy.
            }
        }
    }
}
