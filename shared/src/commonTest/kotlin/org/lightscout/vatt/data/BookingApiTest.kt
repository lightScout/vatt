package org.lightscout.vatt.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.lightscout.vatt.domain.model.BookingResult
import org.lightscout.vatt.core.cache.ClassCache
import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.data.remote.AppJson
import org.lightscout.vatt.data.remote.api.VirginActiveApi
import org.lightscout.vatt.data.repository.BookingRepositoryImpl
import org.lightscout.vatt.domain.error.AppError
import org.lightscout.vatt.domain.model.BookingStatus

class BookingApiTest {

    private fun repoReturning(status: HttpStatusCode, body: String): BookingRepositoryImpl {
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json(AppJson) }
        }
        return BookingRepositoryImpl(VirginActiveApi(client, "http://test"), ClassCache())
    }

    @Test
    fun fullClass_returnsWaitlistedResult() = runTest {
        val body = """
            {"bookingId":"b1","status":"waitlisted","waitlistPosition":2,
             "classInstance":{"classId":"c::1","clubId":"club","title":"Spin","trainer":"T","type":"spin",
               "startsAt":"2026-06-22T07:00:00+02:00","endsAt":"2026-06-22T08:00:00+02:00",
               "spots":18,"available":0,"waitlistCount":2,"status":"full","userBookingStatus":"waitlisted"}}
        """.trimIndent()
        val result = repoReturning(HttpStatusCode.OK, body).book("club", "c::1")
        val success = assertIs<ApiResult.Success<BookingResult>>(result)
        assertEquals(BookingStatus.WAITLISTED, success.data.status)
        assertEquals(2, success.data.waitlistPosition)
    }

    @Test
    fun chaosFailure_mapsToTransient() = runTest {
        val body = """{"error":"ChaosFailure","message":"busy","requestId":"r1"}"""
        val result = repoReturning(HttpStatusCode.ServiceUnavailable, body).book("club", "c::1")
        val failure = assertIs<ApiResult.Failure>(result)
        val err = assertIs<AppError.Transient>(failure.error)
        assertEquals("r1", err.requestId)
    }

    @Test
    fun classInPast_mapsToTypedError() = runTest {
        val body = """{"error":"ClassInPast","message":"too late","requestId":"r2"}"""
        val result = repoReturning(HttpStatusCode.UnprocessableEntity, body).book("club", "c::1")
        val failure = assertIs<ApiResult.Failure>(result)
        assertIs<AppError.ClassInPast>(failure.error)
    }

    @Test
    fun alreadyWaitlisted_mapsToAlreadyBookedWaitlisted() = runTest {
        val body = """{"error":"AlreadyWaitlisted","message":"dup","requestId":"r3"}"""
        val result = repoReturning(HttpStatusCode.Conflict, body).book("club", "c::1")
        val failure = assertIs<ApiResult.Failure>(result)
        val err = assertIs<AppError.AlreadyBooked>(failure.error)
        assertTrue(err.waitlisted)
    }
}
