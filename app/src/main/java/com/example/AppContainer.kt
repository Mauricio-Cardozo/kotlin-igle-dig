package com.example

import android.content.Context
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.BibliaRepository
import com.example.data.repository.LiturgiaRepository
import com.example.data.user.StreaksPreferencesRepository
import com.example.data.user.UserDatabase
import com.example.data.user.UserPreferencesRepository

/**
 * Manual Dependency Injection Container for the application.
 */
interface AppContainer {
    val iglesiaDatabaseHelper: IglesiaDatabaseHelper
    val userDatabase: UserDatabase
    val bibliaRepository: BibliaRepository
    val liturgiaRepository: LiturgiaRepository
    val streaksPreferencesRepository: StreaksPreferencesRepository
    val userPreferencesRepository: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val iglesiaDatabaseHelper: IglesiaDatabaseHelper by lazy {
        IglesiaDatabaseHelper(context)
    }

    override val userDatabase: UserDatabase by lazy {
        UserDatabase.getInstance(context)
    }

    override val bibliaRepository: BibliaRepository by lazy {
        BibliaRepository(iglesiaDatabaseHelper, userDatabase.favoritoDao())
    }

    override val liturgiaRepository: LiturgiaRepository by lazy {
        LiturgiaRepository(iglesiaDatabaseHelper)
    }

    override val streaksPreferencesRepository: StreaksPreferencesRepository by lazy {
        StreaksPreferencesRepository(context)
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }
}
