package com.r4bb1t.blockerspam

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.r4bb1t.blockerspam.adapter.KeywordAdapter

class MessageSettingsActivity : AppCompatActivity() {

    private lateinit var adapter: KeywordAdapter
    private val prefs by lazy { getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE) }
    private val defaultWords = setOf("vivo", "tim", "claro", "oi", "bet")
    private var currentKeywords = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_settings)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val etNewWord = findViewById<EditText>(R.id.etNewWord)
        val rvKeywords = findViewById<RecyclerView>(R.id.rvKeywords)

        btnBack.setOnClickListener { finish() }

        val savedKeywords = prefs.getStringSet("pref_message_keywords", defaultWords) ?: defaultWords
        currentKeywords.addAll(savedKeywords.sorted())

        adapter = KeywordAdapter(currentKeywords) { keywordToRemove ->
            currentKeywords.remove(keywordToRemove)
            saveKeywords()
            adapter.notifyDataSetChanged()
        }

        rvKeywords.layoutManager = LinearLayoutManager(this)
        rvKeywords.adapter = adapter

        btnAdd.setOnClickListener {
            val newWord = etNewWord.text.toString().trim()
            if (newWord.isNotEmpty() && !currentKeywords.contains(newWord)) {
                currentKeywords.add(newWord)
                currentKeywords.sort()
                saveKeywords()
                adapter.notifyDataSetChanged()
                etNewWord.text.clear()
            }
        }
    }

    private fun saveKeywords() {
        prefs.edit().putStringSet("pref_message_keywords", currentKeywords.toSet()).apply()
    }
}
