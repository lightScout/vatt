package org.lightscout.vatt.core.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.lightscout.vatt.core.auth.InMemoryTokenStore
import org.lightscout.vatt.core.auth.SessionEvents
import org.lightscout.vatt.core.auth.TokenStore
import org.lightscout.vatt.core.cache.ClassCache
import org.lightscout.vatt.core.config.PlatformConfig
import org.lightscout.vatt.core.session.UserSession
import org.lightscout.vatt.data.mapper.ManifestMapper
import org.lightscout.vatt.data.remote.AppJson
import org.lightscout.vatt.data.remote.api.VirginActiveApi
import org.lightscout.vatt.data.remote.createHttpClient
import org.lightscout.vatt.data.repository.AuthRepositoryImpl
import org.lightscout.vatt.data.repository.BookingRepositoryImpl
import org.lightscout.vatt.data.repository.HomeRepositoryImpl
import org.lightscout.vatt.data.repository.ProfileRepositoryImpl
import org.lightscout.vatt.data.repository.TimetableRepositoryImpl
import org.lightscout.vatt.domain.repository.AuthRepository
import org.lightscout.vatt.domain.repository.BookingRepository
import org.lightscout.vatt.domain.repository.HomeRepository
import org.lightscout.vatt.domain.repository.ProfileRepository
import org.lightscout.vatt.domain.repository.TimetableRepository
import org.lightscout.vatt.domain.usecase.BookClassUseCase
import org.lightscout.vatt.domain.usecase.CancelBookingUseCase
import org.lightscout.vatt.domain.usecase.GetHomeManifestUseCase
import org.lightscout.vatt.domain.usecase.GetTimetableUseCase
import org.lightscout.vatt.domain.usecase.IsWithinCancellationWindowUseCase
import org.lightscout.vatt.domain.usecase.LoginUseCase
import org.lightscout.vatt.domain.usecase.SetReminderUseCase
import org.lightscout.vatt.presentation.booking.BookingViewModel
import org.lightscout.vatt.presentation.home.HomeViewModel
import org.lightscout.vatt.presentation.login.LoginViewModel
import org.lightscout.vatt.presentation.timetable.TimetableViewModel

private val coreModule = module {
    single { AppJson }
    single { ManifestMapper(get()) }
    single { ClassCache() }
    single { UserSession() }
    single { SessionEvents() }
    single<TokenStore> { InMemoryTokenStore() }

    single {
        val sessionEvents: SessionEvents = get()
        createHttpClient(
            baseUrl = PlatformConfig.baseUrl,
            tokenStore = get(),
            onSessionExpired = sessionEvents::notifyExpired,
        )
    }
    single { VirginActiveApi(client = get(), baseUrl = PlatformConfig.baseUrl) }
}

private val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<HomeRepository> { HomeRepositoryImpl(get(), get(), get()) }
    single<TimetableRepository> { TimetableRepositoryImpl(get(), get()) }
    single<BookingRepository> { BookingRepositoryImpl(get(), get()) }
}

private val useCaseModule = module {
    factory { LoginUseCase(get()) }
    factory { GetHomeManifestUseCase(get()) }
    factory { GetTimetableUseCase(get()) }
    factory { BookClassUseCase(get()) }
    factory { CancelBookingUseCase(get()) }
    factory { IsWithinCancellationWindowUseCase() }
    factory { SetReminderUseCase(get(), get()) }
}

private val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::TimetableViewModel)
    viewModelOf(::BookingViewModel)
}

fun appModules(): List<Module> =
    listOf(coreModule, repositoryModule, useCaseModule, viewModelModule, platformModule())

/** Single entry point to start DI. [appDeclaration] lets Android pass platform context if needed. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModules())
}

/** Starts Koin at most once — safe to call from either platform entry point. */
object KoinStarter {
    private var started = false
    fun startOnce(appDeclaration: KoinAppDeclaration = {}) {
        if (started) return
        started = true
        initKoin(appDeclaration)
    }
}
