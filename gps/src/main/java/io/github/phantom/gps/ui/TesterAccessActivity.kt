package io.github.phantom.gps.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.phantom.gps.databinding.ActivityTesterAccessBinding
import io.github.phantom.gps.utils.TesterAccess
import kotlinx.coroutines.launch

class TesterAccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityTesterAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.retry.setOnClickListener { TesterAccess.retry(this) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TesterAccess.state.collect { state ->
                    when (state) {
                        TesterAccess.State.Loading -> {
                            binding.loading.visibility = View.VISIBLE
                            binding.errorPanel.visibility = View.GONE
                        }
                        TesterAccess.State.Unavailable -> {
                            binding.loading.visibility = View.GONE
                            binding.errorPanel.visibility = View.VISIBLE
                        }
                        TesterAccess.State.Available -> finish()
                    }
                }
            }
        }
    }
}
