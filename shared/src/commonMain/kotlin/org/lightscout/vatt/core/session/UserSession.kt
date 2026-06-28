package org.lightscout.vatt.core.session

import org.lightscout.vatt.domain.model.User

/**
 * Holds the signed-in [User] for the lifetime of the session. Populated by the auth repository on login;
 * read by screens that need the user's `homeClub.id` (timetable, booking) without re-fetching `/me`.
 */
class UserSession {
    var user: User? = null
        private set

    val homeClubId: String? get() = user?.homeClub?.id
    val homeClubName: String? get() = user?.homeClub?.name

    fun set(user: User) { this.user = user }
    fun clear() { user = null }
}
