package com.autoclicker.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.accessibility.AccessibilityManager
import android.widget.*
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.service.FloatingWindowService
import com.autoclicker.app.service.ScreenCaptureService
import com.autoclicker.app.util.AutoCompleteHelper
import com.autoclicker.app.util.CodeEditor
import com.autoclicker.app.util.ScriptStorage
import com.autoclicker.app.util.ScriptTemplates
import com.autoclicker.app.util.SyntaxHighlighter

class ScriptEditorActivity : BaseActivity() {

    private lateinit var storage: ScriptStorage
    private lateinit var etName: EditText
    private lateinit var etCode: CodeEditor
    private lateinit var tvLog: TextView
    private lateinit var btnUndo: ImageView
    private lateinit var btnRedo: ImageView
    private lateinit var syntaxHighlighter: SyntaxHighlighter
    private lateinit var autoCompleteHelper: AutoCompleteHelper

    // Поиск
    private lateinit var searchPanel: LinearLayout
    private lateinit var replacePanel: LinearLayout
    private lateinit var etSearchQuery: EditText
    private lateinit var etReplaceQuery: EditText
    private lateinit var tvSearchResults: TextView
    private var isSearchVisible = false

    private var scriptId: String? = null
    private var hasUnsavedChanges = false
    private var initialCode = ""
    private var initialName = ""
    
    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private val autoSaveRunnable = Runnable { autoSave() }
    private val undoRedoUpdateRunnable = Runnable { updateUndoRedoButtons() }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.startService(this, result.resultCode, result.data!!)
            tryStartPanel()
        }
    }

    private val defaultScript = """// AutoClicker Script
// 
// Основные функции:
// click(x, y) - клик
// longClick(x, y) - долгий клик  
// tap(x, y, count) - множественный тап
// swipe(x1, y1, x2, y2) - свайп
// sleep(ms) - задержка
//
// Системные кнопки:
// back() / home() / recents()
//
// Ожидание:
// waitForColor(x, y, color, timeout)
// waitForText(x1, y1, x2, y2, "text", timeout)
//
// Данные:
// getColor(x, y) - цвет пикселя
// getText(x1, y1, x2, y2) - OCR текста
// compareColor(x, y, color) - сравнить цвет
// random(min, max) - случайное число
//
// Вывод:
// log("text") - в лог
// toast("text") - уведомление
// sendTelegram("text") - в Telegram
//
// EXIT = true - остановить скрипт

log("Скрипт запущен")

while (!EXIT) {
    // Ваш код здесь
    
    sleep(1000)
}

log("Скрипт завершён")"""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_script_editor)

        storage = ScriptStorage(this)

        initViews()
        setupListeners()
        setupBackPressHandler()

        scriptId = intent.getStringExtra("script_id")
        
        // Проверяем, пришёл ли макрос
        val macroScript = intent.getStringExtra("macro_script")
        val macroActionsCount = intent.getIntExtra("macro_actions_count", 0)
        
        if (!macroScript.isNullOrEmpty() && macroActionsCount > 0) {
            // Загружаем макрос как новый скрипт
            loadMacroScript(macroScript, macroActionsCount)
        } else {
            loadScript()
        }
    }
    
    private fun loadMacroScript(macroScript: String, actionsCount: Int) {
        etName.setText("Макрос ${java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
        etCode.setText(macroScript)
        initialName = ""
        initialCode = ""
        hasUnsavedChanges = true
        etCode.clearHistory()
        updateUndoRedoButtons()
        
        Toast.makeText(this, "Записано $actionsCount действий. Сохраните скрипт!", Toast.LENGTH_LONG).show()
    }

    private fun initViews() {
        etName = findViewById(R.id.etScriptName)
        etCode = findViewById(R.id.etScriptCode)
        tvLog = findViewById(R.id.tvLog)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)

        // Поиск
        searchPanel = findViewById(R.id.searchPanel)
        replacePanel = findViewById(R.id.replacePanel)
        etSearchQuery = findViewById(R.id.etSearchQuery)
        etReplaceQuery = findViewById(R.id.etReplaceQuery)
        tvSearchResults = findViewById(R.id.tvSearchResults)

        // Подсветка синтаксиса
        syntaxHighlighter = SyntaxHighlighter(etCode)
        syntaxHighlighter.attach()

        // Автодополнение
        autoCompleteHelper = AutoCompleteHelper(this, etCode)

        // Слушатель результатов поиска
        etCode.searchListener = object : CodeEditor.OnSearchListener {
            override fun onSearchResults(count: Int, current: Int) {
                tvSearchResults.text = if (count > 0) "$current/$count" else "0/0"
            }
        }
    }

    private fun setupListeners() {
        // Кнопка назад
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            handleBackPress()
        }

        // Сохранить
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveScript(silent = false)
        }

        // Запуск
        findViewById<Button>(R.id.btnFloat).setOnClickListener {
            saveScript(silent = false)
            tryStartPanel()
        }

        // Undo
        btnUndo.setOnClickListener {
            if (etCode.undo()) {
                updateUndoRedoButtons()
            }
        }

        // Redo
        btnRedo.setOnClickListener {
            if (etCode.redo()) {
                updateUndoRedoButtons()
            }
        }

        // Поиск
        findViewById<ImageView>(R.id.btnSearch).setOnClickListener {
            toggleSearch()
        }

        // Меню редактора
        findViewById<ImageView>(R.id.btnEditorMenu).setOnClickListener { view ->
            showEditorMenu(view)
        }

        // Кнопки поиска
        findViewById<ImageView>(R.id.btnSearchPrev).setOnClickListener {
            etCode.findPrevious()
        }

        findViewById<ImageView>(R.id.btnSearchNext).setOnClickListener {
            etCode.findNext()
        }

        findViewById<ImageView>(R.id.btnCloseSearch).setOnClickListener {
            toggleSearch()
        }

        findViewById<Button>(R.id.btnReplace).setOnClickListener {
            etCode.replaceCurrent(etReplaceQuery.text.toString())
        }

        findViewById<Button>(R.id.btnReplaceAll).setOnClickListener {
            val count = etCode.replaceAll(etReplaceQuery.text.toString())
            Toast.makeText(this, "Заменено: $count", Toast.LENGTH_SHORT).show()
        }

        // Поиск при вводе
        etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                etCode.search(s?.toString() ?: "")
            }
        })

        // Автосохранение и отслеживание изменений
        setupTextWatchers()
    }

    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        searchPanel.visibility = if (isSearchVisible) View.VISIBLE else View.GONE

        if (isSearchVisible) {
            etSearchQuery.requestFocus()
            // Если есть выделенный текст, используем его для поиска
            val text = etCode.text?.toString() ?: ""
            if (text.isNotEmpty()) {
                val start = etCode.selectionStart.coerceIn(0, text.length)
                val end = etCode.selectionEnd.coerceIn(0, text.length)
                if (start < end && end - start < 100) {
                    val selectedText = text.substring(start, end)
                    etSearchQuery.setText(selectedText)
                    etCode.search(selectedText)
                }
            }
        } else {
            etCode.clearSearch()
            etSearchQuery.setText("")
        }
    }

    private fun showEditorMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 1, "Найти и заменить")
        popup.menu.add(0, 2, 2, "Форматировать код")
        popup.menu.add(0, 3, 3, "Записать макрос")
        popup.menu.add(0, 4, 4, "Вставить из макроса")
        popup.menu.add(0, 5, 5, "Шаблоны скриптов")
        popup.menu.add(0, 6, 6, "Просмотр логов")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    toggleSearch()
                    replacePanel.visibility = View.VISIBLE
                }
                2 -> {
                    etCode.formatCode()
                    Toast.makeText(this, "Код отформатирован", Toast.LENGTH_SHORT).show()
                }
                3 -> {
                    startMacroRecording()
                }
                4 -> {
                    insertMacro()
                }
                5 -> {
                    showTemplatesDialog()
                }
                6 -> {
                    startActivity(Intent(this, LogsActivity::class.java))
                }
            }
            true
        }
        popup.show()
    }

    private fun showTemplatesDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_templates, null)
        val rvTemplates = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvTemplates)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .create()

        rvTemplates.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvTemplates.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<TemplateViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TemplateViewHolder {
                val view = layoutInflater.inflate(R.layout.item_template, parent, false)
                return TemplateViewHolder(view)
            }

            override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
                val template = ScriptTemplates.templates[position]
                holder.bind(template) {
                    // Вставляем шаблон
                    if (etCode.text.toString() == defaultScript || etCode.text.isNullOrEmpty()) {
                        etCode.setText(template.code)
                    } else {
                        // Спрашиваем пользователя
                        AlertDialog.Builder(this@ScriptEditorActivity)
                            .setTitle("Заменить код?")
                            .setMessage("Текущий код будет заменён на шаблон '${template.name}'")
                            .setPositiveButton("Заменить") { _, _ ->
                                etCode.setText(template.code)
                            }
                            .setNeutralButton("Добавить в конец") { _, _ ->
                                etCode.append("\n\n" + template.code)
                            }
                            .setNegativeButton("Отмена", null)
                            .show()
                    }
                    dialog.dismiss()
                }
            }

            override fun getItemCount() = ScriptTemplates.templates.size
        }

        dialog.show()
    }

    inner class TemplateViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        private val tvIcon: TextView = view.findViewById(R.id.tvIcon)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvDescription: TextView = view.findViewById(R.id.tvDescription)

        fun bind(template: ScriptTemplates.Template, onClick: () -> Unit) {
            tvIcon.text = template.icon
            tvName.text = template.name
            tvDescription.text = template.description
            itemView.setOnClickListener { onClick() }
        }
    }

    private fun startMacroRecording() {
        if (!checkPermissions().isEmpty()) {
            Toast.makeText(this, "Сначала включите все разрешения", Toast.LENGTH_SHORT).show()
            return
        }
        com.autoclicker.app.service.MacroRecorderService.startService(this)
    }

    private fun insertMacro() {
        val script = com.autoclicker.app.service.MacroRecorderService.generateScript()
        if (script.isEmpty()) {
            Toast.makeText(this, "Нет записанного макроса", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Вставить макрос?")
            .setMessage("Записано ${com.autoclicker.app.service.MacroRecorderService.recordedActions.size} действий")
            .setPositiveButton("Вставить") { _, _ ->
                etCode.insertAtCursor("\n$script\n")
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasUnsavedChanges = isModified()
                
                // Отложенное автосохранение через 3 секунды
                autoSaveHandler.removeCallbacks(autoSaveRunnable)
                autoSaveHandler.postDelayed(autoSaveRunnable, 3000)
                
                // Обновляем кнопки Undo/Redo
                autoSaveHandler.removeCallbacks(undoRedoUpdateRunnable)
                autoSaveHandler.postDelayed(undoRedoUpdateRunnable, 100)
            }
        }

        // Автодополнение при вводе
        val autoCompleteWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > 0) {
                    autoCompleteHelper.showSuggestions(etCode.selectionStart)
                } else {
                    autoCompleteHelper.dismiss()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etCode.addTextChangedListener(textWatcher)
        etCode.addTextChangedListener(autoCompleteWatcher)
        etName.addTextChangedListener(textWatcher)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges && isModified()) {
            showUnsavedChangesDialog()
        } else {
            finish()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.msg_unsaved_changes)
            .setMessage(R.string.msg_discard_changes)
            .setPositiveButton(R.string.action_save) { _, _ ->
                saveScript(silent = false)
                finish()
            }
            .setNegativeButton(R.string.action_discard) { _, _ ->
                finish()
            }
            .setNeutralButton(R.string.action_cancel, null)
            .show()
    }

    private fun isModified(): Boolean {
        return etCode.text.toString() != initialCode || 
               etName.text.toString() != initialName
    }

    private fun updateUndoRedoButtons() {
        btnUndo.alpha = if (etCode.canUndo()) 1.0f else 0.3f
        btnRedo.alpha = if (etCode.canRedo()) 1.0f else 0.3f
        btnUndo.isEnabled = etCode.canUndo()
        btnRedo.isEnabled = etCode.canRedo()
    }

    private fun loadScript() {
        val id = scriptId
        if (id != null) {
            val script = storage.getScript(id)
            if (script != null) {
                etName.setText(script.name)
                etCode.setText(script.code)
                initialName = script.name
                initialCode = script.code
                etCode.clearHistory()
                updateUndoRedoButtons()
                return
            }
        }
        etCode.setText(defaultScript)
        initialCode = defaultScript
        initialName = ""
        etCode.clearHistory()
        updateUndoRedoButtons()
    }

    private fun saveScript(silent: Boolean = false) {
        val name = etName.text.toString().ifEmpty { "Без названия" }
        val code = etCode.text.toString()

        val script = ScriptStorage.Script(
            id = scriptId ?: java.util.UUID.randomUUID().toString(),
            name = name,
            code = code
        )

        storage.saveScript(script)
        scriptId = script.id
        initialName = name
        initialCode = code
        hasUnsavedChanges = false

        if (!silent) {
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun autoSave() {
        if (hasUnsavedChanges && isModified()) {
            saveScript(silent = true)
        }
    }

    private fun tryStartPanel() {
        val missingPermissions = checkPermissions()

        if (missingPermissions.isEmpty()) {
            FloatingWindowService.startService(this, scriptId)
            Toast.makeText(this, "Панель запущена", Toast.LENGTH_SHORT).show()
        } else {
            showPermissionDialog(missingPermissions)
        }
    }

    private fun checkPermissions(): List<String> {
        val missing = mutableListOf<String>()

        if (!isAccessibilityEnabled()) {
            missing.add("Accessibility Service")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            missing.add("Наложение поверх окон")
        }

        if (!ScreenCaptureService.isRunning) {
            missing.add("Захват экрана")
        }

        return missing
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.id.contains(packageName) }
    }

    private fun showPermissionDialog(missing: List<String>) {
        val message = "Для запуска скрипта нужны разрешения:\n\n" +
                missing.joinToString("\n") { "• $it" } +
                "\n\nОткрыть настройки?"

        AlertDialog.Builder(this)
            .setTitle("Нет разрешений")
            .setMessage(message)
            .setPositiveButton("Настройки") { _, _ ->
                openPermissionSettings(missing.first())
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun openPermissionSettings(permission: String) {
        when (permission) {
            "Accessibility Service" -> {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            "Наложение поверх окон" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
            "Захват экрана" -> {
                val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE)
                        as MediaProjectionManager
                screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        autoCompleteHelper.dismiss()
        // Автосохранение только если activity не уничтожается
        if (!isFinishing && hasUnsavedChanges) {
            saveScript(silent = true)
        }
    }

    override fun onDestroy() {
        autoSaveHandler.removeCallbacksAndMessages(null) // Удаляем ВСЕ callbacks
        if (::syntaxHighlighter.isInitialized) {
            syntaxHighlighter.detach()
        }
        if (::autoCompleteHelper.isInitialized) {
            autoCompleteHelper.dismiss()
        }
        super.onDestroy()
    }
}
