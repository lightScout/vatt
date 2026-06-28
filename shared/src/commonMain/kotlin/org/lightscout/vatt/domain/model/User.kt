package org.lightscout.vatt.domain.model

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val membershipTier: MembershipTier,
    val homeClub: Club,
)

data class Club(
    val id: String,
    val name: String,
)

/** `ESSENTIAL`, `PREMIUM`, `CLUB` are the known tiers; [Unknown] guards against new tiers being added. */
enum class MembershipTier {
    ESSENTIAL, PREMIUM, CLUB, UNKNOWN;

    companion object {
        fun fromWire(raw: String?): MembershipTier = when (raw?.lowercase()) {
            "essential" -> ESSENTIAL
            "premium" -> PREMIUM
            "club" -> CLUB
            else -> UNKNOWN
        }
    }
}
