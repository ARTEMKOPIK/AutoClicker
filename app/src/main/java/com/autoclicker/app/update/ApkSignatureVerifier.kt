package com.autoclicker.app.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Утилита для проверки подписи APK файла
 * 
 * Обеспечивает безопасность обновлений путём проверки что APK подписан тем же сертификатом
 * что и текущая установленная версия приложения.
 * 
 * Это защищает от:
 * - Подмены APK злоумышленником
 * - Man-in-the-middle атак
 * - Компрометации GitHub релизов
 */
object ApkSignatureVerifier {
    
    /**
     * Проверяет что APK файл подписан тем же сертификатом что и текущее приложение
     * 
     * @param context Context приложения
     * @param apkFile Файл APK для проверки
     * @return true если подпись совпадает, false если нет или произошла ошибка
     */
    fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists()) {
                com.autoclicker.app.util.CrashHandler.logError(
                    "ApkSignatureVerifier",
                    "APK file does not exist: ${apkFile.absolutePath}",
                    null
                )
                return false
            }
            
            // Получаем подпись текущего приложения
            val currentSignature = getCurrentAppSignature(context)
            if (currentSignature == null) {
                com.autoclicker.app.util.CrashHandler.logError(
                    "ApkSignatureVerifier",
                    "Failed to get current app signature",
                    null
                )
                return false
            }
            
            // Получаем подпись APK файла
            val apkSignature = getApkSignature(context, apkFile)
            if (apkSignature == null) {
                com.autoclicker.app.util.CrashHandler.logError(
                    "ApkSignatureVerifier",
                    "Failed to get APK signature from: ${apkFile.absolutePath}",
                    null
                )
                return false
            }
            
            // Сравниваем SHA-256 хеши подписей
            val currentHash = getSignatureHash(currentSignature)
            val apkHash = getSignatureHash(apkSignature)
            
            val matches = currentHash.contentEquals(apkHash)
            
            if (matches) {
                com.autoclicker.app.util.CrashHandler.logInfo(
                    "ApkSignatureVerifier",
                    "✅ APK signature verified successfully"
                )
            } else {
                com.autoclicker.app.util.CrashHandler.logError(
                    "ApkSignatureVerifier",
                    "❌ APK signature mismatch! Current: ${currentHash.toHexString()}, APK: ${apkHash.toHexString()}",
                    null
                )
            }
            
            matches
            
        } catch (e: Exception) {
            com.autoclicker.app.util.CrashHandler.logError(
                "ApkSignatureVerifier",
                "Exception during signature verification: ${e.message}",
                e
            )
            false
        }
    }
    
    /**
     * Получает подпись текущего установленного приложения
     */
    private fun getCurrentAppSignature(context: Context): Signature? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            getSignatureFromPackageInfo(packageInfo)
            
        } catch (e: Exception) {
            com.autoclicker.app.util.CrashHandler.logError(
                "ApkSignatureVerifier",
                "Failed to get current app signature: ${e.message}",
                e
            )
            null
        }
    }
    
    /**
     * Получает подпись из APK файла
     */
    private fun getApkSignature(context: Context, apkFile: File): Signature? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            if (packageInfo == null) {
                com.autoclicker.app.util.CrashHandler.logError(
                    "ApkSignatureVerifier",
                    "getPackageArchiveInfo returned null for: ${apkFile.absolutePath}",
                    null
                )
                return null
            }
            
            getSignatureFromPackageInfo(packageInfo)
            
        } catch (e: Exception) {
            com.autoclicker.app.util.CrashHandler.logError(
                "ApkSignatureVerifier",
                "Failed to get APK signature: ${e.message}",
                e
            )
            null
        }
    }
    
    /**
     * Извлекает подпись из PackageInfo (version-aware)
     */
    private fun getSignatureFromPackageInfo(packageInfo: PackageInfo): Signature? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9+ (API 28+)
            val signingInfo = packageInfo.signingInfo
            if (signingInfo == null) {
                com.autoclicker.app.util.CrashHandler.logError(
                    "ApkSignatureVerifier",
                    "signingInfo is null",
                    null
                )
                return null
            }
            
            // Проверяем схему подписи
            val signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            
            if (signatures.isEmpty()) {
                com.autoclicker.app.util.CrashHandler.logError(
                    "ApkSignatureVerifier",
                    "No signatures found in signingInfo",
                    null
                )
                return null
            }
            
            signatures[0]
        } else {
            // Android 8.1 и ниже (API 27 и ниже)
            @Suppress("DEPRECATION")
            val signatures = packageInfo.signatures
            if (signatures.isNullOrEmpty()) {
                com.autoclicker.app.util.CrashHandler.logError(
                    "ApkSignatureVerifier",
                    "No signatures found in packageInfo",
                    null
                )
                return null
            }
            signatures[0]
        }
    }
    
    /**
     * Вычисляет SHA-256 хеш подписи
     */
    private fun getSignatureHash(signature: Signature): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(signature.toByteArray())
    }
    
    /**
     * Конвертирует ByteArray в hex строку для логирования
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}

