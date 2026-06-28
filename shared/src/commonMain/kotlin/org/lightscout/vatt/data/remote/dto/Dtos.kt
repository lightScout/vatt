package org.lightscout.vatt.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Wire DTOs — intentionally separate from domain models. Enum-like fields are kept as raw [String]s so a
 * value the client doesn't recognise never fails decoding; mappers convert them with `fromWire` fallbacks.
 * Nullability is generous for the same defensive reason.
 */

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class TokenPairDto(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: Int? = null,
    val user: UserDto? = null,
)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class UserDto(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val membershipTier: String? = null,
    val homeClub: ClubDto? = null,
)

@Serializable
data class ClubDto(val id: String = "", val name: String = "")

@Serializable
data class ClassInstanceDto(
    val classId: String = "",
    val clubId: String = "",
    val title: String = "",
    val trainer: String = "",
    val type: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val spots: Int = 0,
    val available: Int = 0,
    val waitlistCount: Int = 0,
    val status: String? = null,
    val userBookingStatus: String? = null,
)

@Serializable
data class TimetableDto(
    val clubId: String = "",
    val weekStart: String? = null,
    val weekEnd: String? = null,
    val selectedDate: String? = null,
    val days: List<TimetableDayDto> = emptyList(),
)

@Serializable
data class TimetableDayDto(
    val date: String? = null,
    val classes: List<ClassInstanceDto> = emptyList(),
)

@Serializable
data class BookingResponseDto(
    val bookingId: String = "",
    val status: String? = null,
    val waitlistPosition: Int? = null,
    val classInstance: ClassInstanceDto = ClassInstanceDto(),
)

/** The mock's consistent error envelope. */
@Serializable
data class ApiErrorDto(
    val error: String? = null,
    val message: String? = null,
    val code: String? = null,
    val requestId: String? = null,
)

/**
 * The home manifest is polymorphic. We decode its blocks as raw [JsonObject]s and map them by their
 * `type` field in code, so an unrecognised block becomes `HomeBlock.Unknown` instead of failing the whole
 * response. This is more robust here than sealed-class polymorphic serialization, which throws on
 * unregistered subtypes.
 */
@Serializable
data class HomeManifestDto(val blocks: List<JsonObject> = emptyList())

@Serializable
data class HomeTileDto(
    val id: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val imageRef: String? = null,
    val badge: String? = null,
)
