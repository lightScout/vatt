package org.lightscout.vatt.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import org.lightscout.vatt.data.mapper.ManifestMapper
import org.lightscout.vatt.data.mapper.toDomainOrNull
import org.lightscout.vatt.data.remote.AppJson
import org.lightscout.vatt.data.remote.dto.BookingResponseDto
import org.lightscout.vatt.data.remote.dto.ClassInstanceDto
import org.lightscout.vatt.data.remote.dto.HomeManifestDto
import org.lightscout.vatt.domain.model.BookingStatus
import org.lightscout.vatt.domain.model.ClassStatus
import org.lightscout.vatt.domain.model.ClassType
import org.lightscout.vatt.domain.model.HomeBlock

class DefensiveParsingTest {

    @Test
    fun unknownEnumValues_fallBackToUnknown_notThrow() {
        // "zumba" / "weird" are values the client doesn't know; an extra field is present too.
        val json = """
            {"classId":"x::1","clubId":"club","title":"Zumba","trainer":"T","type":"zumba",
             "startsAt":"2026-06-22T07:00:00+02:00","endsAt":"2026-06-22T08:00:00+02:00",
             "spots":10,"available":3,"waitlistCount":0,"status":"weird",
             "userBookingStatus":"none","brandNewField":"ignored"}
        """.trimIndent()
        val dto = AppJson.decodeFromString(ClassInstanceDto.serializer(), json)
        val session = dto.toDomainOrNull()!!
        assertEquals(ClassType.UNKNOWN, session.type)
        assertEquals(ClassStatus.UNKNOWN, session.status)
        assertEquals(3, session.available)
    }

    @Test
    fun unknownManifestBlock_isSkipped_knownBlocksParsed() {
        // greeting (known) + experimental (unknown) — the live API really ships this shape.
        val json = """
            {"blocks":[
              {"type":"greeting","title":"Good evening, Avid"},
              {"type":"experimental","payload":{"kind":"futureFeature","data":{"foo":"bar"}}}
            ]}
        """.trimIndent()
        val dto = AppJson.decodeFromString(HomeManifestDto.serializer(), json)
        val manifest = ManifestMapper(AppJson).toDomain(dto)

        assertTrue(manifest.blocks.any { it is HomeBlock.Greeting })
        // The unknown block is represented as Unknown (and rendered as nothing), never crashing.
        assertTrue(manifest.blocks.any { it is HomeBlock.Unknown && it.type == "experimental" })
    }

    @Test
    fun bookingResponse_waitlisted_parsesPositionAndStatus() {
        val json = """
            {"bookingId":"b-123","status":"waitlisted","waitlistPosition":4,
             "classInstance":{"classId":"x::1","clubId":"club","title":"Spin","trainer":"T",
               "type":"spin","startsAt":"2026-06-22T07:00:00+02:00","endsAt":"2026-06-22T08:00:00+02:00",
               "spots":18,"available":0,"waitlistCount":4,"status":"full","userBookingStatus":"waitlisted"}}
        """.trimIndent()
        val dto = AppJson.decodeFromString(BookingResponseDto.serializer(), json)
        val result = dto.toDomainOrNull()!!
        assertEquals(BookingStatus.WAITLISTED, result.status)
        assertEquals(4, result.waitlistPosition)
        assertTrue(result.isWaitlisted)
    }

    @Test
    fun emptyOrMissingFields_doNotThrow() {
        // Manifest with an empty blocks array, and a class missing optional fields.
        val manifest = ManifestMapper(AppJson).toDomain(HomeManifestDto(blocks = emptyList<JsonObject>()))
        assertTrue(manifest.blocks.isEmpty())
    }
}
