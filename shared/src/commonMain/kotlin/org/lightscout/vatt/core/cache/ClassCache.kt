package org.lightscout.vatt.core.cache

import org.lightscout.vatt.domain.model.ClassSession

/**
 * In-memory lookup of the classes the user has seen (timetable + home carousel). The API has no
 * single-class GET endpoint, so the booking screen resolves a tapped class by id from here instead of
 * passing the whole [ClassSession] (which contains non-trivially-serializable time types) through
 * navigation arguments.
 *
 * Trade-off: not durable across process death. Acceptable for this assessment (the mock also wipes state
 * on restart); a production version would refetch or persist. Documented in the write-up.
 */
class ClassCache {
    private val byId = mutableMapOf<String, ClassSession>()

    fun put(sessions: List<ClassSession>) {
        sessions.forEach { byId[it.classId] = it }
    }

    fun put(session: ClassSession) {
        byId[session.classId] = session
    }

    fun get(classId: String): ClassSession? = byId[classId]
}
