package com.github.dfedez920912.anivexa.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import com.github.dfedez920912.anivexa.util.AppLogger
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.SecureRandom
import kotlin.coroutines.resume

enum class PlayIntegrityStatus(val label: String, val detail: String) {
    CHECKING("Comprobando...", "Evaluando integridad del dispositivo"),
    VERIFIED("Verificada (Play Integrity)", "Token de integridad generado por Google Play"),
    AVAILABLE("API disponible", "Servicios de Google Play presentes en el dispositivo"),
    UNAVAILABLE("No disponible", "Sin Google Play Store o sin conexión; la app funciona normalmente"),
    ERROR("No verificada", "No se pudo completar la comprobación; la app sigue operativa")
}

data class DeviceSecurityReport(
    val isRooted: Boolean = false,
    val rootIndicators: List<String> = emptyList(),
    val playIntegrityStatus: PlayIntegrityStatus = PlayIntegrityStatus.CHECKING,
    val checkedAtMillis: Long = 0L
)

/**
 * Non-blocking device root and Play Integrity verification helper.
 * Detects compromised environments and surfaces diagnostic status without
 * locking out core anime calendar, watchlist, notification, or widget features.
 */
object DeviceSecurityManager {

    private const val TAG = "DeviceSecurityManager"

    private val suBinaryPaths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/vendor/bin/su",
        "/system/xbin/daemonsu"
    )

    private val rootManagementPackages = listOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "me.weishu.kernelsu",
        "com.yellowes.su"
    )

    fun isDeviceRooted(context: Context? = null): Boolean {
        return detectRootIndicators(context).isNotEmpty()
    }

    fun detectRootIndicators(context: Context? = null): List<String> {
        val indicators = mutableListOf<String>()

        // 1. Check test-keys build tag
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            indicators.add("Build firmado con test-keys")
        }

        // 2. Check standard su binaries & Superuser files
        val foundPaths = suBinaryPaths.filter { path ->
            try {
                File(path).exists()
            } catch (_: SecurityException) {
                false
            }
        }
        if (foundPaths.isNotEmpty()) {
            indicators.add("Binarios de privilegios elevados detectados")
        }

        // 3. Check `which su` in PATH
        if (checkSuInPath()) {
            indicators.add("Ejecutable su accesible en PATH")
        }

        // 4. Check known root manager packages if context is available
        if (context != null) {
            val pm = context.packageManager
            val installedRootApps = rootManagementPackages.any { pkg ->
                isPackageInstalled(pm, pkg)
            }
            if (installedRootApps) {
                indicators.add("Gestor de permisos root instalado")
            }
        }

        return indicators.distinct()
    }

    private fun checkSuInPath(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readLine() != null
            }
        } catch (_: Throwable) {
            false
        } finally {
            try {
                process?.destroy()
            } catch (_: Throwable) {
            }
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun evaluateDeviceSecurity(context: Context): DeviceSecurityReport =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val indicators = detectRootIndicators(appContext)
            val integrityStatus = checkPlayIntegrity(appContext)

            if (indicators.isNotEmpty()) {
                AppLogger.w(TAG, "Root indicators detected: ${indicators.joinToString()}")
            } else {
                AppLogger.d(TAG, "No root indicators detected. Integrity status=$integrityStatus")
            }

            DeviceSecurityReport(
                isRooted = indicators.isNotEmpty(),
                rootIndicators = indicators,
                playIntegrityStatus = integrityStatus,
                checkedAtMillis = System.currentTimeMillis()
            )
        }

    private suspend fun checkPlayIntegrity(context: Context): PlayIntegrityStatus {
        val hasPlayStore = isPackageInstalled(context.packageManager, "com.android.vending")
        if (!hasPlayStore) {
            return PlayIntegrityStatus.UNAVAILABLE
        }

        return try {
            val integrityManager = IntegrityManagerFactory.create(context)
            val nonceBytes = ByteArray(24)
            SecureRandom().nextBytes(nonceBytes)
            val nonce = Base64.encodeToString(
                nonceBytes,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()

            suspendCancellableCoroutine { cont ->
                integrityManager.requestIntegrityToken(request)
                    .addOnSuccessListener { response ->
                        val hasToken = !response.token().isNullOrBlank()
                        if (cont.isActive) {
                            cont.resume(
                                if (hasToken) PlayIntegrityStatus.VERIFIED else PlayIntegrityStatus.AVAILABLE
                            )
                        }
                    }
                    .addOnFailureListener { error ->
                        AppLogger.w(TAG, "Play Integrity check returned non-fatal status: ${error.message}")
                        if (cont.isActive) {
                            // Play Integrity requires Play Store enrollment/cloud project link;
                            // gracefully report AVAILABLE when Play Store exists without blocking app usage.
                            cont.resume(PlayIntegrityStatus.AVAILABLE)
                        }
                    }
            }
        } catch (t: Throwable) {
            AppLogger.w(TAG, "Play Integrity unavailable in current environment: ${t.message}")
            PlayIntegrityStatus.UNAVAILABLE
        }
    }
}
