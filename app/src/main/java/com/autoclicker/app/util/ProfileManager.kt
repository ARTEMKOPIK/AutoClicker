package com.autoclicker.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Менеджер профилей - группировка скриптов по приложениям/задачам
 */
class ProfileManager(context: Context) {

    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val lock = Any()

    data class Profile(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val packageName: String? = null, // Привязка к приложению
        val scriptIds: List<String> = emptyList(),
        val icon: String? = null,
        val color: Int = 0xFFFF5722.toInt(),
        val createdAt: Long = System.currentTimeMillis()
    )

    fun saveProfile(profile: Profile) {
        synchronized(lock) {
            val profiles = getAllProfilesInternal().toMutableList()
            val existingIndex = profiles.indexOfFirst { it.id == profile.id }

            if (existingIndex >= 0) {
                profiles[existingIndex] = profile
            } else {
                profiles.add(profile)
            }

            prefs.edit().putString("profiles_list", gson.toJson(profiles)).apply()
        }
    }

    fun getProfile(id: String): Profile? {
        synchronized(lock) {
            return getAllProfilesInternal().find { it.id == id }
        }
    }

    fun getProfileByPackage(packageName: String): Profile? {
        synchronized(lock) {
            return getAllProfilesInternal().find { it.packageName == packageName }
        }
    }

    fun getAllProfiles(): List<Profile> {
        synchronized(lock) {
            return getAllProfilesInternal()
        }
    }

    private fun getAllProfilesInternal(): List<Profile> {
        val json = prefs.getString("profiles_list", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Profile>>() {}.type
            gson.fromJson<List<Profile>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteProfile(id: String) {
        synchronized(lock) {
            val profiles = getAllProfilesInternal().filter { it.id != id }
            prefs.edit().putString("profiles_list", gson.toJson(profiles)).apply()
        }
    }

    /**
     * Add a script to a profile.
     * 
     * @return true if script was added successfully, false otherwise
     */
    fun addScriptToProfile(profileId: String, scriptId: String): Boolean {
        synchronized(lock) {
            val profile = getProfile(profileId) ?: run {
                // Профиль не найден - логируем предупреждение
                CrashHandler.logWarning("ProfileManager", "Profile not found: $profileId")
                return false
            }
            
            if (scriptId in profile.scriptIds) {
                // Скрипт уже в профиле
                return false
            }
            
            val updated = profile.copy(scriptIds = profile.scriptIds + scriptId)
            saveProfile(updated)
            return true
        }
    }

    fun removeScriptFromProfile(profileId: String, scriptId: String) {
        synchronized(lock) {
            val profile = getProfile(profileId) ?: return
            val updated = profile.copy(scriptIds = profile.scriptIds - scriptId)
            saveProfile(updated)
        }
    }

    // Активный профиль
    var activeProfileId: String?
        get() = prefs.getString("active_profile_id", null)
        set(value) = prefs.edit().putString("active_profile_id", value).apply()

    fun getActiveProfile(): Profile? {
        val id = activeProfileId ?: return null
        return getProfile(id)
    }
}
