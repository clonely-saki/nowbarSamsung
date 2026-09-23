package com.nowbarsports.poc

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FootballRealDataSource(
    private val mockDataSource: MatchDataSource,
    private val apiClient: FootballApiClient = FootballApiClient(BuildConfig.API_FOOTBALL_KEY),
    val configuredFixtureId: String = BuildConfig.API_FOOTBALL_FIXTURE_ID
) : MatchDataSource {
    companion object {
        const val EVENT_ID_PREFIX = "football-real:"

        fun eventIdFor(fixtureId: String): String = EVENT_ID_PREFIX + fixtureId

        fun isRealEvent(eventId: String): Boolean = eventId.startsWith(EVENT_ID_PREFIX)

        fun fixtureIdFromEventId(eventId: String): String? =
            eventId.takeIf(::isRealEvent)?.removePrefix(EVENT_ID_PREFIX)?.takeIf { it.isNotBlank() }
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "football-real-data").apply { isDaemon = true }
    }

    @Volatile
    private var lastKnownGood: MatchSnapshot? = null

    private val fixturePresentations = ConcurrentHashMap<String, FootballFixturePresentation>()

    fun presentationFor(context: Context, eventId: String): FootballFixturePresentation? =
        fixturePresentations[eventId] ?: FootballFixturePresentationStore.load(context, eventId)

    val isConfigured: Boolean
        get() = BuildConfig.API_FOOTBALL_KEY.isNotBlank() && configuredFixtureId.isNotBlank()

    override fun events(): List<MockEvent> = mockDataSource.events()

    override fun findEvent(eventId: String): MockEvent? = mockDataSource.findEvent(eventId)

    override fun initial(eventId: String): MatchSnapshot = mockDataSource.initial(eventId)

    override fun restore(eventId: String, step: Int): MatchSnapshot? = mockDataSource.restore(eventId, step)

    override fun next(current: MatchSnapshot): MatchSnapshot = mockDataSource.next(current)

    fun refreshRealFootball(context: Context, callback: (Result<MatchSnapshot>) -> Unit) {
        refreshReal(context, previous = lastKnownGood, callback = callback)
    }

    override fun refresh(
        context: Context,
        current: MatchSnapshot?,
        callback: (Result<MatchSnapshot>) -> Unit
    ) {
        if (current != null && !isRealEvent(current.eventId)) {
            mockDataSource.refresh(context, current, callback)
            return
        }

        refreshReal(
            context = context,
            previous = current?.takeIf { isRealEvent(it.eventId) } ?: lastKnownGood,
            callback = callback
        )
    }

    private fun refreshReal(
        context: Context,
        previous: MatchSnapshot?,
        callback: (Result<MatchSnapshot>) -> Unit
    ) {
        val fixtureId = configuredFixtureId.trim()
        if (BuildConfig.API_FOOTBALL_KEY.isBlank()) {
            callback(Result.failure(FootballApiException("未配置 API_FOOTBALL_KEY")))
            return
        }
        if (fixtureId.isBlank()) {
            callback(Result.failure(FootballApiException("未配置 API_FOOTBALL_FIXTURE_ID")))
            return
        }

        executor.execute {
            val result = runCatching {
                val dto = apiClient.fetchFixture(fixtureId)
                val snapshot = FootballFixtureMapper.toSnapshot(dto, (previous?.step ?: -1) + 1)
                val presentation = FootballFixturePresentationMapper.from(dto)
                fixturePresentations[snapshot.eventId] = presentation
                FootballFixturePresentationStore.save(context, snapshot.eventId, presentation)
                snapshot
            }.onSuccess { snapshot ->
                lastKnownGood = snapshot
            }
            callback(result)
        }
    }
}
