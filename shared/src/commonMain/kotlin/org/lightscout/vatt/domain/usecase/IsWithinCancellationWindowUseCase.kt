package org.lightscout.vatt.domain.usecase

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.lightscout.vatt.domain.model.ClassSession

/**
 * Business rule (client-side only — the API does not enforce it): if a member cancels **within 12 hours**
 * of a class starting, they may forfeit any cost. The booking screen uses this to decide whether to show
 * a forfeit warning before cancelling.
 *
 * Comparison is done on the absolute [ZonedTime.instant], so venue timezone offsets are handled correctly.
 * A [Clock] is injected so the rule is unit-testable without depending on wall-clock time.
 */
class IsWithinCancellationWindowUseCase(
    private val clock: Clock = Clock.System,
    private val window: Duration = 12.hours,
) {
    operator fun invoke(session: ClassSession, now: Instant = clock.now()): Boolean {
        // Within the window if the class starts at or before (now + 12h). Classes already under way
        // (start <= now) are also "within" — cancelling those certainly risks a forfeit.
        return session.startsAt.instant <= now + window
    }
}
