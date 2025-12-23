package com.autoclicker.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.util.ProfileManager
import com.autoclicker.app.util.ScriptStorage
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfilesActivity : BaseActivity() {

    private lateinit var profileManager: ProfileManager
    private lateinit var storage: ScriptStorage
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profiles)

        profileManager = ProfileManager(this)
        storage = ScriptStorage(this)

        initViews()
        loadProfiles()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.tvEmpty)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddProfileDialog()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProfileAdapter(
            onSelect = { profile ->
                profileManager.activeProfileId = profile.id
                loadProfiles()
                Toast.makeText(this, "Профиль '${profile.name}' активирован", Toast.LENGTH_SHORT).show()
            },
            onEdit = { profile -> showEditProfileDialog(profile) },
            onDelete = { profile ->
                profileManager.deleteProfile(profile.id)
                loadProfiles()
            }
        )
        recyclerView.adapter = adapter
    }

    private fun loadProfiles() {
        val profiles = profileManager.getAllProfiles()
        val activeId = profileManager.activeProfileId
        adapter.submitList(profiles, activeId)
        emptyView.visibility = if (profiles.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (profiles.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddProfileDialog() {
        val input = EditText(this).apply {
            hint = "Название профиля"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Новый профиль")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val profile = ProfileManager.Profile(name = name)
                    profileManager.saveProfile(profile)
                    loadProfiles()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditProfileDialog(profile: ProfileManager.Profile) {
        val scripts = storage.getAllScripts()
        val selectedIds = profile.scriptIds.toMutableSet()

        val items = scripts.map { it.name }.toTypedArray()
        val checked = scripts.map { it.id in selectedIds }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("Скрипты в профиле '${profile.name}'")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                val scriptId = scripts[which].id
                if (isChecked) {
                    selectedIds.add(scriptId)
                } else {
                    selectedIds.remove(scriptId)
                }
            }
            .setPositiveButton("Сохранить") { _, _ ->
                val updated = profile.copy(scriptIds = selectedIds.toList())
                profileManager.saveProfile(updated)
                loadProfiles()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    inner class ProfileAdapter(
        private val onSelect: (ProfileManager.Profile) -> Unit,
        private val onEdit: (ProfileManager.Profile) -> Unit,
        private val onDelete: (ProfileManager.Profile) -> Unit
    ) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

        private var profiles = listOf<ProfileManager.Profile>()
        private var activeId: String? = null

        fun submitList(newList: List<ProfileManager.Profile>, activeProfileId: String?) {
            profiles = newList
            activeId = activeProfileId
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(profiles[position])
        }

        override fun getItemCount() = profiles.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tvProfileName)
            private val tvCount: TextView = view.findViewById(R.id.tvScriptCount)
            private val ivActive: ImageView = view.findViewById(R.id.ivActive)
            private val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
            private val btnDelete: ImageView = view.findViewById(R.id.btnDelete)

            fun bind(profile: ProfileManager.Profile) {
                tvName.text = profile.name
                tvCount.text = "${profile.scriptIds.size} скриптов"
                ivActive.visibility = if (profile.id == activeId) View.VISIBLE else View.GONE

                itemView.setOnClickListener { onSelect(profile) }
                btnEdit.setOnClickListener { onEdit(profile) }
                btnDelete.setOnClickListener { onDelete(profile) }
            }
        }
    }
}
