package org.lightscout.vatt.domain.model

/**
 * The personalised home screen is **server-driven**: the API returns an ordered list of [HomeBlock]s and
 * the client renders whatever it understands, in order. Crucially, [HomeBlock.Unknown] captures any block
 * `type` we don't recognise (the live API already ships an `experimental` block) so new server features
 * never crash the client — they're simply skipped.
 */
data class HomeManifest(
    val blocks: List<HomeBlock>,
)

sealed interface HomeBlock {
    data class Greeting(val title: String) : HomeBlock

    data class Hero(val title: String, val subtitle: String?) : HomeBlock

    data class MyClub(
        val name: String,
        val addressLine: String?,
        val openingHoursToday: String?,
        val phoneNumber: String?,
    ) : HomeBlock

    /** Carousel of recommended classes. `items` is often empty late in the rolling week. */
    data class ClassCarousel(
        val title: String?,
        val openTimetableClubId: String?,
        val items: List<ClassSession>,
    ) : HomeBlock

    data class Rewards(val title: String?, val items: List<HomeTile>) : HomeBlock

    data class Goals(val title: String?, val items: List<HomeTile>) : HomeBlock

    data class Promotion(
        val title: String,
        val subtitle: String?,
        val imageRef: String?,
    ) : HomeBlock

    /** Any block type the client doesn't know about — rendered as nothing. */
    data class Unknown(val type: String) : HomeBlock
}

/** Generic content tile used by rewards/goals. */
data class HomeTile(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageRef: String?,
    val badge: String?,
)
