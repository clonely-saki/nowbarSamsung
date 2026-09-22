package com.nowbarsports.poc

import android.content.Context

interface MatchDataSource {
    fun events(): List<MockEvent>

    fun findEvent(eventId: String): MockEvent?

    fun initial(eventId: String): MatchSnapshot

    fun restore(eventId: String, step: Int): MatchSnapshot?

    fun next(current: MatchSnapshot): MatchSnapshot

    /**
     * Refreshes the current event without making the UI or receiver know whether it is mock or real.
     * Mock data remains synchronous internally; a real source performs network work off the caller thread.
     */
    fun refresh(context: Context, current: MatchSnapshot?, callback: (Result<MatchSnapshot>) -> Unit) {
        if (current == null) {
            callback(Result.failure(IllegalStateException("没有可更新的赛事")))
        } else {
            callback(runCatching { next(current) })
        }
    }
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
    val footballReal: FootballRealDataSource = FootballRealDataSource(MockMatchDataSource)
    val current: MatchDataSource = footballReal
}
