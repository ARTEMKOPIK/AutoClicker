package com.autoclicker.app.update

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.BuildConfig
import com.autoclicker.app.R
import com.google.android.material.button.MaterialButton

/**
 * Красивый диалог обновления с анимациями
 */
class UpdateDialog(
    context: Context,
    private val updateInfo: UpdateInfo,
    private val onUpdate: () -> Unit,
    private val onLater: () -> Unit,
    private val onSkip: () -> Unit
) : Dialog(context, R.style.Theme_AutoClicker_Dialog) {

    private lateinit var updateIconBg: View
    private lateinit var ivUpdateIcon: ImageView
    private lateinit var tvCurrentVersion: TextView
    private lateinit var tvNewVersion: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var rvChangelog: RecyclerView
    private lateinit var downloadProgressContainer: LinearLayout
    private lateinit var tvDownloadStatus: TextView
    private lateinit var tvDownloadPercent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvDownloadSize: TextView
    private lateinit var buttonsContainer: LinearLayout
    private lateinit var btnUpdate: MaterialButton
    private lateinit var btnLater: MaterialButton
    private lateinit var btnSkip: MaterialButton

    private var pulseAnimator: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_update)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        initViews()
        setupData()
        setupButtons()
        startEntranceAnimation()
    }

    private fun initViews() {
        updateIconBg = findViewById(R.id.updateIconBg)
        ivUpdateIcon = findViewById(R.id.ivUpdateIcon)
        tvCurrentVersion = findViewById(R.id.tvCurrentVersion)
        tvNewVersion = findViewById(R.id.tvNewVersion)
        tvFileSize = findViewById(R.id.tvFileSize)
        rvChangelog = findViewById(R.id.rvChangelog)
        downloadProgressContainer = findViewById(R.id.downloadProgressContainer)
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus)
        tvDownloadPercent = findViewById(R.id.tvDownloadPercent)
        progressBar = findViewById(R.id.progressBar)
        tvDownloadSize = findViewById(R.id.tvDownloadSize)
        buttonsContainer = findViewById(R.id.buttonsContainer)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnLater = findViewById(R.id.btnLater)
        btnSkip = findViewById(R.id.btnSkip)
    }

    private fun setupData() {
        tvCurrentVersion.text = BuildConfig.VERSION_NAME
        tvNewVersion.text = updateInfo.versionName
        tvFileSize.text = "Размер: ${updateInfo.fileSizeFormatted}"

        // Настраиваем список изменений
        val changelogItems = updateInfo.getChangelogItems()
        if (changelogItems.isNotEmpty()) {
            rvChangelog.layoutManager = LinearLayoutManager(context)
            rvChangelog.adapter = ChangelogAdapter(changelogItems)
        } else {
            // Если нет структурированного changelog, показываем как текст
            rvChangelog.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        btnUpdate.setOnClickListener {
            onUpdate()
        }

        btnLater.setOnClickListener {
            onLater()
            dismiss()
        }

        btnSkip.setOnClickListener {
            onSkip()
            dismiss()
        }
    }

    /**
     * Анимация появления диалога
     */
    private fun startEntranceAnimation() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.alpha = 0f
        rootView.scaleX = 0.8f
        rootView.scaleY = 0.8f

        rootView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        // Анимация иконки
        updateIconBg.alpha = 0f
        updateIconBg.scaleX = 0f
        updateIconBg.scaleY = 0f

        updateIconBg.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setStartDelay(150)
            .setInterpolator(OvershootInterpolator(2f))
            .withEndAction { startPulseAnimation() }
            .start()

        // Анимация иконки внутри
        ivUpdateIcon.rotation = -180f
        ivUpdateIcon.animate()
            .rotation(0f)
            .setDuration(500)
            .setStartDelay(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * Пульсирующая анимация иконки
     */
    private fun startPulseAnimation() {
        val scaleX = ObjectAnimator.ofFloat(updateIconBg, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(updateIconBg, "scaleY", 1f, 1.1f, 1f)

        pulseAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1500
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (isShowing) {
                        startDelay = 500
                        start()
                    }
                }
            })
            start()
        }
    }

    /**
     * Показать прогресс загрузки
     */
    fun showDownloadProgress() {
        downloadProgressContainer.visibility = View.VISIBLE
        btnUpdate.isEnabled = false
        btnUpdate.text = "Загрузка..."
        btnLater.visibility = View.GONE
        btnSkip.visibility = View.GONE
        setCancelable(false)

        // Анимация появления прогресса
        downloadProgressContainer.alpha = 0f
        downloadProgressContainer.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    /**
     * Обновить прогресс загрузки
     */
    fun updateDownloadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long) {
        tvDownloadPercent.text = "$progress%"
        
        // Анимированное обновление прогресса
        val animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, progress)
        animator.duration = 200
        animator.start()

        val downloadedMb = downloadedBytes / (1024.0 * 1024.0)
        val totalMb = totalBytes / (1024.0 * 1024.0)
        tvDownloadSize.text = String.format("%.1f MB / %.1f MB", downloadedMb, totalMb)
    }

    /**
     * Загрузка завершена
     */
    fun showDownloadComplete() {
        tvDownloadStatus.text = "Загрузка завершена!"
        tvDownloadPercent.text = "100%"
        progressBar.progress = 100
        
        btnUpdate.isEnabled = true
        btnUpdate.text = "Установить"
        btnUpdate.setIconResource(R.drawable.ic_check)
        
        // Анимация успеха
        btnUpdate.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(100)
            .withEndAction {
                btnUpdate.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    /**
     * Ошибка загрузки
     */
    fun showDownloadError(message: String) {
        tvDownloadStatus.text = "Ошибка: $message"
        tvDownloadStatus.setTextColor(context.getColor(R.color.error))
        
        btnUpdate.isEnabled = true
        btnUpdate.text = "Повторить"
        btnLater.visibility = View.VISIBLE
        setCancelable(true)
    }

    override fun dismiss() {
        pulseAnimator?.cancel()
        super.dismiss()
    }

    /**
     * Адаптер для списка изменений
     */
    private class ChangelogAdapter(
        private val items: List<ChangelogItem>
    ) : RecyclerView.Adapter<ChangelogAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvIcon)
            val tvText: TextView = view.findViewById(R.id.tvText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_changelog, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvIcon.text = item.icon
            holder.tvText.text = item.text

            // Анимация появления элементов
            holder.itemView.alpha = 0f
            holder.itemView.translationX = 50f
            holder.itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200)
                .setStartDelay((position * 50).toLong())
                .start()
        }

        override fun getItemCount() = items.size
    }
}
