package de.davis.keygo.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import de.davis.keygo.core.identity.domain.repository.AccountRepository
import de.davis.keygo.core.security.domain.Session
import de.davis.keygo.feature.password_health.domain.PasswordHealthAttention
import de.davis.keygo.feature.settings.domain.model.ThemeSettings
import de.davis.keygo.feature.settings.domain.usecase.ObserveThemeSettingsUseCase
import de.davis.keygo.legacy_migration.domain.usecase.HasMainPasswordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AppViewModel(
    private val accountRepository: AccountRepository,
    private val hasV1Password: HasMainPasswordUseCase,
    session: Session,
    passwordHealthAttention: PasswordHealthAttention,
    observeThemeSettings: ObserveThemeSettingsUseCase,
) : ViewModel() {

    private val _isReturningUser = MutableStateFlow<Boolean?>(null)
    val isReturningUser = _isReturningUser.asStateFlow()

    /**
     * Eager and seeded with [ThemeSettings]' own defaults, so it holds a value from the moment the
     * ViewModel exists. [MainActivity] reads it on the very first frame it composes, and a theme
     * that only arrived once the stored one had been loaded would draw those first frames in
     * colours the user did not pick. Seeding with the defaults costs nothing either: they are what
     * the app drew before there was a setting, so a collection that never catches up looks exactly
     * like the app did before.
     */
    val themeSettings: StateFlow<ThemeSettings> =
        observeThemeSettings().stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSettings())

    /**
     * The session's raw state, which [MainActivity] gates on: no ARK means the user has to
     * authenticate again, whether the session just ended or a restored process never had one.
     */
    val isSessionActive: StateFlow<Boolean> = session.isActive

    val needsAttentionCount: StateFlow<Int> = passwordHealthAttention.needsAttention

    private val _requestedTopLevelRoute = MutableStateFlow<NavKey?>(null)
    val requestedTopLevelRoute = _requestedTopLevelRoute.asStateFlow()

    init {
        viewModelScope.launch {
            _isReturningUser.update { accountRepository.getOrNull() != null || hasV1Password() }
        }
    }

    fun onIntentAction(action: String?) {
        AppDestinations.fromIntentAction(action)
            ?.let { dest -> _requestedTopLevelRoute.update { dest.route } }
    }

    fun onTopLevelRouteRequestHandled() {
        _requestedTopLevelRoute.value = null
    }
}
