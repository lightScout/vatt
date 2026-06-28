package org.lightscout.vatt.data.remote

import kotlinx.serialization.json.Json

/**
 * Deliberately lenient JSON. The brief warns the API docs are stale and shapes drift, and the live
 * manifest already carries an `experimental` block we can't model. These settings let the client tolerate
 * extra/missing/renamed fields rather than throwing:
 *  - [ignoreUnknownKeys]: new server fields don't break decoding.
 *  - [coerceInputValues]: nulls for non-null Kotlin fields fall back to defaults instead of crashing.
 *  - [isLenient]: accept slightly non-strict JSON.
 *  - [explicitNulls] = false: omit nulls when encoding request bodies.
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
}
