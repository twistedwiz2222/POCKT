package com.pockt.app

import android.app.Application
import com.pockt.app.data.PocktDatabase
import com.pockt.app.data.PocktRepository

class PocktApplication : Application() {
    val repository by lazy { PocktRepository(PocktDatabase.get(this).dao()) }
}
