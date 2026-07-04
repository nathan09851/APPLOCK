package com.nathan.applock.data

import android.content.Context

/**
 * Lightweight manual DI container.
 * Initialize once from Application or Activity context.
 */
object AppLockerModule {

    @Volatile
    private var _lockRepository: LockRepository? = null

    @Volatile
    private var _secureAuthRepository: SecureAuthRepository? = null

    fun init(context: Context) {
        val appContext = context.applicationContext
        if (_lockRepository == null) {
            synchronized(this) {
                if (_lockRepository == null) {
                    _lockRepository = LockRepository(appContext)
                }
            }
        }
        if (_secureAuthRepository == null) {
            synchronized(this) {
                if (_secureAuthRepository == null) {
                    _secureAuthRepository = SecureAuthRepository(appContext)
                }
            }
        }
    }

    val lockRepository: LockRepository
        get() = _lockRepository
            ?: throw IllegalStateException("AppLockerModule not initialized. Call init() first.")

    val secureAuthRepository: SecureAuthRepository
        get() = _secureAuthRepository
            ?: throw IllegalStateException("AppLockerModule not initialized. Call init() first.")
}
