package com.nowbarsports.poc

interface MatchDataSource {
    fun events(): List<MockEvent>

    fun findEvent(eventId: String): MockEvent?

    fun initial(eventId: String): MatchSnapshot

    fun restore(eventId: String, step: Int): MatchSnapshot?

    fun next(current: MatchSnapshot): MatchSnapshot
}

object MockMatchDataSource : MatchDataSource {
    override fun events(): List<MockEvent> = MockEventCatalog.events

    override fun findEvent(eventId: String): MockEvent? = MockEventCatalog.find(eventId)

    override fun initial(eventId: String): MatchSnapshot = MatchSimulator.initial(eventId)

    override fun restore(eventId: String, step: Int): MatchSnapshot? = runCatching {
        MatchSimulator.snapshot(eventId, step)
    }.getOrNull()

    override fun next(current: MatchSnapshot): MatchSnapshot = MatchSimulator.next(current)
}

object MatchDataSources {
    val current: MatchDataSource = MockMatchDataSource
}
