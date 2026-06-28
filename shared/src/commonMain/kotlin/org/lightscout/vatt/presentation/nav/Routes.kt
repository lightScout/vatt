package org.lightscout.vatt.presentation.nav

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations (Navigation-Compose + kotlinx.serialization). */

@Serializable
object LoginRoute

@Serializable
object HomeRoute

@Serializable
object TimetableRoute

@Serializable
data class ClassDetailRoute(val classId: String)
