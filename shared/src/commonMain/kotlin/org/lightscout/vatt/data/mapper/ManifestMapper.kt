package org.lightscout.vatt.data.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.lightscout.vatt.data.remote.dto.ClassInstanceDto
import org.lightscout.vatt.data.remote.dto.HomeManifestDto
import org.lightscout.vatt.data.remote.dto.HomeTileDto
import org.lightscout.vatt.domain.model.HomeBlock
import org.lightscout.vatt.domain.model.HomeManifest
import org.lightscout.vatt.domain.model.HomeTile

/**
 * Maps the polymorphic manifest into typed [HomeBlock]s. Each block is dispatched on its `type` string;
 * anything unrecognised (e.g. the live `experimental` block) becomes [HomeBlock.Unknown] and is later
 * skipped during rendering — the client renders what it knows and ignores the rest.
 */
class ManifestMapper(private val json: Json) {

    fun toDomain(dto: HomeManifestDto): HomeManifest =
        HomeManifest(blocks = dto.blocks.mapNotNull { mapBlock(it) })

    private fun mapBlock(obj: JsonObject): HomeBlock? {
        val type = obj["type"]?.jsonPrimitive?.contentOrNull() ?: return null
        return when (type) {
            "greeting" -> HomeBlock.Greeting(title = obj.str("title").orEmpty())
            "hero" -> HomeBlock.Hero(title = obj.str("title").orEmpty(), subtitle = obj.str("subtitle"))
            "myClub" -> HomeBlock.MyClub(
                name = obj.str("name").orEmpty(),
                addressLine = obj.str("addressLine"),
                openingHoursToday = obj.str("openingHoursToday"),
                phoneNumber = obj.str("phoneNumber"),
            )
            "classCarousel" -> HomeBlock.ClassCarousel(
                title = obj.str("title"),
                openTimetableClubId = obj["viewAllAction"]?.jsonObjectOrNull()?.str("clubId"),
                items = obj.decodeList<ClassInstanceDto>("items").mapNotNull { it.toDomainOrNull() },
            )
            "myRewards" -> HomeBlock.Rewards(
                title = obj.str("title"),
                items = obj.decodeList<HomeTileDto>("items").map { it.toTile() },
            )
            "myGoals" -> HomeBlock.Goals(
                title = obj.str("title"),
                items = obj.decodeList<HomeTileDto>("items").map { it.toTile() },
            )
            "promotion" -> HomeBlock.Promotion(
                title = obj.str("title").orEmpty(),
                subtitle = obj.str("subtitle"),
                imageRef = obj.str("imageRef"),
            )
            else -> HomeBlock.Unknown(type)
        }
    }

    private fun HomeTileDto.toTile() = HomeTile(id, title, subtitle, imageRef, badge)

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull()

    private inline fun <reified T> JsonObject.decodeList(key: String): List<T> {
        val arr = this[key]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return emptyList()
        return arr.mapNotNull { el ->
            runCatching { json.decodeFromJsonElement<T>(el) }.getOrNull()
        }
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): JsonObject? =
        runCatching { jsonObject }.getOrNull()

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
        if (this is kotlinx.serialization.json.JsonNull) null else content
}
