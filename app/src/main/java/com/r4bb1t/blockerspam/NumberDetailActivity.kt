package com.r4bb1t.blockerspam

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.r4bb1t.blockerspam.data.CallDatabase
import com.r4bb1t.blockerspam.data.WhitelistEntry
import com.r4bb1t.blockerspam.databinding.ActivityNumberDetailBinding
import com.r4bb1t.blockerspam.helper.NumberInfoHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NumberDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNumberDetailBinding
    private val db by lazy { CallDatabase.getInstance(this) }
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy  HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNumberDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Adiciona a margem da status bar/câmera
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val number = intent.getStringExtra("number") ?: run { finish(); return }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadInfo(number)
        observeCalls(number)
        setupButtons(number)
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }

    private fun loadInfo(number: String) {
        val info = NumberInfoHelper.getInfo(this, number)
        binding.tvNumber.text = info.formatted
        binding.tvLineType.text = info.lineType
        binding.tvRegion.text = info.region ?: (info.country ?: "—")
        binding.tvDdd.text = if (info.ddd != null) "DDD ${info.ddd}" else "—"
        binding.tvRaw.text = number
    }

    private fun observeCalls(number: String) {
        lifecycleScope.launch {
            db.callDao().getCallsForNumber(number).collectLatest { calls ->
                binding.tvTotalAttempts.text = "${calls.size} tentativa(s)"
                val sb = StringBuilder()
                calls.forEach { call ->
                    sb.appendLine("• ${dateFmt.format(Date(call.timestamp))}")
                }
                binding.tvHistory.text = sb.toString().trimEnd()
                binding.tvHistory.visibility = if (calls.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            val isWl = db.callDao().isWhitelisted(number)
            updateWhitelistBtn(isWl, number)
        }
    }

    private fun setupButtons(number: String) {
        binding.btnAddContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, number)
            }
            startActivity(intent)
        }

        binding.btnDelete.setOnClickListener {
            lifecycleScope.launch {
                db.callDao().deleteCallsForNumber(number)
                finish()
            }
        }
    }

    private fun updateWhitelistBtn(isWhitelisted: Boolean, number: String) {
        if (isWhitelisted) {
            binding.btnWhitelist.text = "Remover da whitelist"
            binding.btnWhitelist.setOnClickListener {
                lifecycleScope.launch {
                    db.callDao().deleteWhitelist(WhitelistEntry(number))
                    updateWhitelistBtn(false, number)
                }
            }
        } else {
            binding.btnWhitelist.text = "Adicionar à whitelist"
            binding.btnWhitelist.setOnClickListener {
                lifecycleScope.launch {
                    db.callDao().insertWhitelist(WhitelistEntry(number))
                    updateWhitelistBtn(true, number)
                }
            }
        }
    }
}
