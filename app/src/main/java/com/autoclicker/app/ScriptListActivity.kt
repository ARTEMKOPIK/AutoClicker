package com.autoclicker.app

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.util.HapticFeedback
import com.autoclicker.app.util.ScriptExporter
import com.autoclicker.app.util.ScriptStorage
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class ScriptListActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var searchContainer: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var adapter: ScriptAdapter
    private lateinit var storage: ScriptStorage
    private lateinit var exporter: ScriptExporter

    private var allScripts = listOf<ScriptStorage.Script>()
    private var currentSortMode = SortMode.DATE_DESC
    private var isSearchVisible = false
    private var textWatcher: TextWatcher? = null

    // Для импорта файла
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val script = exporter.importScript(uri)
                    if (script != null) {
                        storage.saveScript(script)
                        loadScripts()
                        Toast.makeText(this, "Скрипт '${script.name}' импортирован", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Не удалось распознать файл скрипта", Toast.LENGTH_LONG).show()
                        com.autoclicker.app.util.CrashHandler.logWarning("ScriptList", "Import failed for uri: $uri")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
                    com.autoclicker.app.util.CrashHandler.logError("ScriptList", "Import exception", e)
                }
            }
        }
    }

    enum class SortMode {
        NAME, DATE, DATE_DESC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_script_list)

        storage = ScriptStorage(this)
        exporter = ScriptExporter(this)

        initViews()
        setupListeners()
        setupSwipeToDelete()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.tvEmpty)
        searchContainer = findViewById(R.id.searchContainer)
        etSearch = findViewById(R.id.etSearch)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ScriptAdapter(
            onItemClick = { script -> openScript(script) },
            onDeleteClick = { script -> deleteScript(script) },
            onShareClick = { script -> shareScript(script) },
            onQRClick = { script -> showQRCode(script) }
        )
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        // Назад
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            HapticFeedback.light(it)
            finish()
        }

        // Добавить скрипт
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            HapticFeedback.light(it)
            startActivity(Intent(this, ScriptEditorActivity::class.java))
        }

        // Поиск
        findViewById<ImageView>(R.id.btnSearch).setOnClickListener {
            HapticFeedback.light(it)
            toggleSearch()
        }

        // Закрыть поиск
        findViewById<ImageView>(R.id.btnCloseSearch).setOnClickListener {
            HapticFeedback.light(it)
            toggleSearch()
        }

        // Сортировка
        findViewById<ImageView>(R.id.btnSort).setOnClickListener { view ->
            HapticFeedback.light(view)
            showSortMenu(view)
        }

        // Меню (импорт)
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener { view ->
            HapticFeedback.light(view)
            showImportMenu(view)
        }

        // Поиск по тексту
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterScripts(s?.toString() ?: "")
            }
        }
        etSearch.addTextChangedListener(textWatcher)
    }

    override fun onDestroy() {
        textWatcher?.let { etSearch.removeTextChangedListener(it) }
        textWatcher = null
        super.onDestroy()
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            
            private val deleteIcon = ContextCompat.getDrawable(this@ScriptListActivity, R.drawable.ic_delete)
            private val background = ColorDrawable(Color.parseColor("#F44336"))
            private val iconMargin = 32

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val script = adapter.getScriptAt(position)
                
                // Удаляем скрипт
                storage.deleteScript(script.id)
                loadScripts()
                
                // Показываем Snackbar с возможностью отмены
                Snackbar.make(recyclerView, R.string.msg_script_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.msg_undo) {
                        // Восстанавливаем скрипт
                        storage.saveScript(script)
                        loadScripts()
                    }
                    .setActionTextColor(ContextCompat.getColor(this@ScriptListActivity, R.color.primary))
                    .show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                
                if (dX < 0) {
                    // Рисуем красный фон
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    // Рисуем иконку удаления
                    deleteIcon?.let { icon ->
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth

                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        searchContainer.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
        
        if (isSearchVisible) {
            etSearch.requestFocus()
        } else {
            etSearch.setText("")
            filterScripts("")
        }
    }

    private fun showSortMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 0, 0, R.string.sort_by_name)
            menu.add(0, 1, 1, R.string.sort_by_date)
            menu.add(0, 2, 2, R.string.sort_by_date_desc)
            
            setOnMenuItemClickListener { item ->
                currentSortMode = when (item.itemId) {
                    0 -> SortMode.NAME
                    1 -> SortMode.DATE
                    else -> SortMode.DATE_DESC
                }
                sortAndDisplayScripts()
                true
            }
            show()
        }
    }

    private fun filterScripts(query: String) {
        val filtered = if (query.isEmpty()) {
            allScripts
        } else {
            allScripts.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true)
            }
        }
        
        val sorted = sortScripts(filtered)
        adapter.submitList(sorted)
        
        emptyView.text = if (query.isNotEmpty() && filtered.isEmpty()) {
            getString(R.string.msg_no_results)
        } else {
            getString(R.string.msg_no_scripts_hint)
        }
        
        updateEmptyState(sorted.isEmpty())
    }

    private fun sortScripts(scripts: List<ScriptStorage.Script>): List<ScriptStorage.Script> {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return when (currentSortMode) {
            SortMode.NAME -> scripts.sortedBy { it.name.lowercase() }
            SortMode.DATE -> scripts.sortedBy { 
                try { dateFormat.parse(it.date)?.time ?: 0L } catch (e: Exception) { 0L }
            }
            SortMode.DATE_DESC -> scripts.sortedByDescending { 
                try { dateFormat.parse(it.date)?.time ?: Long.MAX_VALUE } catch (e: Exception) { Long.MAX_VALUE }
            }
        }
    }

    private fun sortAndDisplayScripts() {
        val query = etSearch.text?.toString() ?: ""
        filterScripts(query)
    }

    override fun onResume() {
        super.onResume()
        loadScripts()
    }

    private fun loadScripts() {
        allScripts = storage.getAllScripts()
        sortAndDisplayScripts()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun openScript(script: ScriptStorage.Script) {
        val intent = Intent(this, ScriptEditorActivity::class.java)
        intent.putExtra("script_id", script.id)
        startActivity(intent)
    }

    private fun deleteScript(script: ScriptStorage.Script) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_delete) + "?")
            .setMessage("Вы уверены что хотите удалить '${script.name}'?")
            .setPositiveButton(R.string.action_delete) { _, _ ->
                storage.deleteScript(script.id)
                loadScripts()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showImportMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 0, 0, "Импорт из файла")
            menu.add(0, 1, 1, "Сканировать QR-код")

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> importFromFile()
                    1 -> scanQRCode()
                }
                true
            }
            show()
        }
    }

    private fun scanQRCode() {
        val options = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
            .build()
        
        val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient(options)
        
        // Используем ScreenCaptureService для получения скриншота
        val captureService = com.autoclicker.app.service.ScreenCaptureService.instance
        if (captureService == null) {
            Toast.makeText(this, "Включите захват экрана в настройках", Toast.LENGTH_LONG).show()
            return
        }
        
        val bitmap = captureService.takeScreenshot()
        if (bitmap == null) {
            Toast.makeText(this, "Не удалось сделать скриншот", Toast.LENGTH_SHORT).show()
            return
        }
        
        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
        
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                bitmap.recycle()
                if (barcodes.isEmpty()) {
                    Toast.makeText(this, "QR-код не найден на экране", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                val qrData = barcodes.first().rawValue
                if (qrData.isNullOrEmpty()) {
                    Toast.makeText(this, "QR-код пустой", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Пробуем импортировать скрипт
                val script = exporter.importFromQRData(qrData)
                if (script != null) {
                    storage.saveScript(script)
                    loadScripts()
                    Toast.makeText(this, "Скрипт '${script.name}' импортирован!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "QR-код не содержит скрипт AutoClicker", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                bitmap.recycle()
                Toast.makeText(this, "Ошибка сканирования: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun importFromFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        importFileLauncher.launch(intent)
    }

    private fun shareScript(script: ScriptStorage.Script) {
        val intent = exporter.getShareIntent(script)
        if (intent != null) {
            startActivity(Intent.createChooser(intent, "Поделиться скриптом"))
        } else {
            Toast.makeText(this, "Ошибка экспорта", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showQRCode(script: ScriptStorage.Script) {
        val qrBitmap = exporter.generateQRCode(script)
        if (qrBitmap == null) {
            Toast.makeText(this, "Скрипт слишком большой для QR-кода", Toast.LENGTH_LONG).show()
            return
        }

        val imageView = ImageView(this).apply {
            setImageBitmap(qrBitmap)
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("QR-код: ${script.name}")
            .setView(imageView)
            .setPositiveButton("OK", null)
            .setNeutralButton("Поделиться") { _, _ ->
                shareQRCode(script)
            }
            .show()
    }

    private fun shareQRCode(script: ScriptStorage.Script) {
        val intent = exporter.getShareQRIntent(script)
        if (intent != null) {
            startActivity(Intent.createChooser(intent, "Поделиться QR-кодом"))
        } else {
            Toast.makeText(this, "Ошибка создания QR-кода", Toast.LENGTH_SHORT).show()
        }
    }

    inner class ScriptAdapter(
        private val onItemClick: (ScriptStorage.Script) -> Unit,
        private val onDeleteClick: (ScriptStorage.Script) -> Unit,
        private val onShareClick: (ScriptStorage.Script) -> Unit,
        private val onQRClick: (ScriptStorage.Script) -> Unit
    ) : RecyclerView.Adapter<ScriptAdapter.ViewHolder>() {

        private var scripts = listOf<ScriptStorage.Script>()

        fun submitList(newList: List<ScriptStorage.Script>) {
            val diffCallback = ScriptDiffCallback(scripts, newList)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            scripts = newList
            diffResult.dispatchUpdatesTo(this)
        }

        fun getScriptAt(position: Int): ScriptStorage.Script = scripts[position]

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_script, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(scripts[position])
        }

        override fun getItemCount() = scripts.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tvName)
            private val tvDate: TextView = view.findViewById(R.id.tvDate)
            private val btnDelete: ImageView = view.findViewById(R.id.btnDelete)

            fun bind(script: ScriptStorage.Script) {
                tvName.text = script.name
                tvDate.text = script.date

                itemView.setOnClickListener {
                    HapticFeedback.light(it)
                    onItemClick(script)
                }
                itemView.setOnLongClickListener {
                    showScriptMenu(script)
                    true
                }
                btnDelete.setOnClickListener { onDeleteClick(script) }
            }

            private fun showScriptMenu(script: ScriptStorage.Script) {
                PopupMenu(itemView.context, itemView).apply {
                    menu.add(0, 0, 0, "Редактировать")
                    menu.add(0, 1, 1, "Поделиться файлом")
                    menu.add(0, 2, 2, "Показать QR-код")
                    menu.add(0, 3, 3, "Удалить")

                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            0 -> onItemClick(script)
                            1 -> onShareClick(script)
                            2 -> onQRClick(script)
                            3 -> onDeleteClick(script)
                        }
                        true
                    }
                    show()
                }
            }
        }
    }

    class ScriptDiffCallback(
        private val oldList: List<ScriptStorage.Script>,
        private val newList: List<ScriptStorage.Script>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.name == new.name && old.date == new.date && old.code == new.code
        }
    }
}
