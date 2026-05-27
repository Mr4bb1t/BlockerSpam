package com.r4bb1t.blockerspam

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.r4bb1t.blockerspam.adapter.BlockedCallsAdapter
import com.r4bb1t.blockerspam.data.CallDatabase
import com.r4bb1t.blockerspam.databinding.ActivityMainBinding
import com.r4bb1t.blockerspam.service.BlockerCallScreeningService
import com.r4bb1t.blockerspam.updater.GithubUpdater
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: BlockedCallsAdapter
    private val db by lazy { CallDatabase.getInstance(this) }
    private val prefs by lazy { getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE) }
    private val updater by lazy { GithubUpdater(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkSetupComplete()
        requestScreeningRole()
    }

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkSetupComplete() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Aplica padding para status bar (câmera/notch) e navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupRecyclerView()
        setupToggle()
        observeData()
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkSetupComplete()
        checkForUpdates()
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            val updateInfo = updater.checkForUpdate()
            if (updateInfo != null) {
                binding.bannerUpdate.visibility = View.VISIBLE
                binding.tvUpdateMsg.text = "Nova versão ${updateInfo.version} disponível!"
                binding.btnUpdateAction.setOnClickListener {
                    binding.btnUpdateAction.isEnabled = false
                    binding.btnUpdateAction.text = "Baixando..."
                    updater.downloadAndInstall(updateInfo)
                }
            } else {
                binding.bannerUpdate.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = BlockedCallsAdapter { summary ->
            val intent = Intent(this, NumberDetailActivity::class.java).apply {
                putExtra("number", summary.number)
            }
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupToggle() {
        val enabled = prefs.getBoolean(BlockerCallScreeningService.PREF_BLOCKING_ENABLED, true)
        binding.switchBlocking.isChecked = enabled
        updateToggleUI(enabled)

        binding.switchBlocking.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(BlockerCallScreeningService.PREF_BLOCKING_ENABLED, isChecked).apply()
            updateToggleUI(isChecked)
        }
    }

    private fun updateToggleUI(enabled: Boolean) {
        binding.tvBlockingStatus.text = if (enabled) "Proteção ativa" else "Proteção desativada"
        binding.cardStatus.setCardBackgroundColor(
            if (enabled) getColor(R.color.accent_red) else getColor(R.color.surface)
        )
    }

    private fun observeData() {
        // Blocked calls list
        lifecycleScope.launch {
            db.callDao().getBlockedCallSummaries().collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.tvTotalBlocked.text = "${list.sumOf { it.callCount }} total"
            }
        }

        // Today's count
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        lifecycleScope.launch {
            db.callDao().getCountSince(startOfDay).collectLatest { count ->
                binding.tvTodayCount.text = "$count hoje"
            }
        }
    }

    private fun requestPermissions() {
        val required = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            requestScreeningRole()
        }
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) {
                roleRequestLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        }
    }

    private fun checkSetupComplete() {
        val hasPermissions = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasRole = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else false

        binding.bannerSetup.visibility = if (!hasPermissions || !hasRole) View.VISIBLE else View.GONE
        binding.tvSetupMsg.text = when {
            !hasPermissions -> "⚠ Permissões necessárias não concedidas"
            !hasRole -> "⚠ Defina BlockerSpam como app de triagem de chamadas"
            else -> ""
        }
        binding.btnSetupAction.visibility = if (!hasRole && hasPermissions) View.VISIBLE else View.GONE
        binding.btnSetupAction.setOnClickListener { requestScreeningRole() }
    }
}