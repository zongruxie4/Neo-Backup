package com.machiav3lli.backup.utils.extensions

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.getScopeId
import timber.log.Timber

open class NeoViewModel : ViewModel() {
    init {
        Timber.w("neoviewmodel@koinscope: ${getScopeId()}")
    }
}

@Composable
inline fun <reified T : ViewModel> koinNeoViewModel() = koinViewModel<T>(
    viewModelStoreOwner = LocalContext.current as ComponentActivity
)