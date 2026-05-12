package com.example.funnyexpensetracking.ui.auth

import android.content.Intent
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
import com.example.funnyexpensetracking.MainActivity
import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.data.remote.TokenRefresher
import com.example.funnyexpensetracking.databinding.ActivityLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 邮箱密码登录，对接后端 `POST /v1/auth/login`。
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var tokenRefresher: TokenRefresher

    private var forceShowForm: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forceShowForm = intent.getBooleanExtra(EXTRA_FORCE_LOGIN, false)
        if (!forceShowForm && userPreferencesManager.hasBackendSession()) {
            tokenRefresher.ensureAccessOrRefresh()
            if (userPreferencesManager.hasAuthToken()) {
                launchMainAndFinish()
                return
            }
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val lastEmail = userPreferencesManager.getBackendUserEmail()
        if (lastEmail.isNotBlank()) {
            binding.etEmail.setText(lastEmail)
        }

        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.etEmail.text?.toString().orEmpty(),
                binding.etPassword.text?.toString().orEmpty()
            )
        }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        observeUiState()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        LoginUiState.Idle -> {
                            binding.progressLogin.visibility = View.GONE
                            binding.btnLogin.isEnabled = true
                        }
                        LoginUiState.Loading -> {
                            binding.progressLogin.visibility = View.VISIBLE
                            binding.btnLogin.isEnabled = false
                        }
                        LoginUiState.Success -> {
                            binding.progressLogin.visibility = View.GONE
                            binding.btnLogin.isEnabled = true
                            viewModel.resetState()
                            if (forceShowForm && !isTaskRoot) {
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                launchMainAndFinish()
                            }
                        }
                        is LoginUiState.Error -> {
                            binding.progressLogin.visibility = View.GONE
                            binding.btnLogin.isEnabled = true
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun launchMainAndFinish() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    companion object {
        const val EXTRA_FORCE_LOGIN = "extra_force_login"
    }
}
