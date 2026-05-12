package com.example.funnyexpensetracking.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.databinding.ActivityRegisterBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 注册账号，对接后端 `POST /v1/auth/register`。
 */
@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.btnRegister.setOnClickListener {
            viewModel.register(
                binding.etEmail.text?.toString().orEmpty(),
                binding.etNickname.text?.toString().orEmpty(),
                binding.etPassword.text?.toString().orEmpty()
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        RegisterUiState.Idle -> {
                            binding.progressRegister.visibility = View.GONE
                            binding.btnRegister.isEnabled = true
                        }
                        RegisterUiState.Loading -> {
                            binding.progressRegister.visibility = View.VISIBLE
                            binding.btnRegister.isEnabled = false
                        }
                        RegisterUiState.Success -> {
                            binding.progressRegister.visibility = View.GONE
                            binding.btnRegister.isEnabled = true
                            Toast.makeText(
                                this@RegisterActivity,
                                R.string.register_success_toast,
                                Toast.LENGTH_LONG
                            ).show()
                            viewModel.resetState()
                            finish()
                        }
                        is RegisterUiState.Error -> {
                            binding.progressRegister.visibility = View.GONE
                            binding.btnRegister.isEnabled = true
                            Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }
}
