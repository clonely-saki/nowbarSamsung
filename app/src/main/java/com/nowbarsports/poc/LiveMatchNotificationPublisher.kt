package com.nowbarsports.poc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Posts the stable notification immediately, then optionally tests two small Samsung Icon extras. */
object LiveMatchNotificationPublisher {
    private const val LOGO_SIZE_PX = 48
    private const val LOGO_TIMEOUT_SECONDS = 4L

    private val logoExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "football-notification-icons").apply { isDaemon = true }
    }

    fun post(context: Context, snapshot: MatchSnapshot) {
        val appContext = context.applicationContext
        LiveMatchNotifier.post(appContext, snapshot)

        if (!BuildConfig.SAMSUNG_DIAGNOSTIC || snapshot.kind != MatchKind.FOOTBALL ||
            !FootballRealDataSource.isRealEvent(snapshot.eventId)
        ) return

        logoExecutor.execute {
            val presentation = runCatching {
                FootballFixturePresentationStore.load(appContext, snapshot.eventId)
            }.getOrNull() ?: return@execute
            val homeUrl = presentation.homeLogoUrl?.takeIf(String::isNotBlank) ?: return@execute
            val awayUrl = presentation.awayLogoUrl?.takeIf(String::isNotBlank) ?: return@execute

            val homeLogo = AtomicReference<Bitmap?>()
            val awayLogo = AtomicReference<Bitmap?>()
            val loaded = CountDownLatch(2)
            val imageLoader = appContext.imageLoader

            fun request(url: String, result: AtomicReference<Bitmap?>): ImageRequest =
                ImageRequest.Builder(appContext)
                    .data(url)
                    .size(LOGO_SIZE_PX, LOGO_SIZE_PX)
                    .allowHardware(false)
                    .target(
                        onSuccess = { drawable ->
                            result.set(runCatching {
                                drawable.toBitmap(
                                    width = LOGO_SIZE_PX,
                                    height = LOGO_SIZE_PX,
                                    config = Bitmap.Config.ARGB_8888
                                )
                            }.getOrNull())
                            loaded.countDown()
                        },
                        onError = { loaded.countDown() }
                    )
                    .build()

            val homeLoad = runCatching {
                imageLoader.enqueue(request(homeUrl, homeLogo))
            }.getOrNull() ?: return@execute
            val awayLoad = runCatching {
                imageLoader.enqueue(request(awayUrl, awayLogo))
            }.getOrNull() ?: run {
                homeLoad.dispose()
                return@execute
            }

            if (!runCatching { loaded.await(LOGO_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
                    .getOrDefault(false)) {
                homeLoad.dispose()
                awayLoad.dispose()
                return@execute
            }

            val homeBitmap = homeLogo.get() ?: return@execute
            val awayBitmap = awayLogo.get() ?: return@execute
            val isStillCurrent = runCatching {
                FollowStore.load(appContext).isTracked(snapshot.eventId) &&
                    MatchStore.load(appContext, MatchDataSources.current)?.let {
                        it.eventId == snapshot.eventId && it.step == snapshot.step
                    } == true
            }.getOrDefault(false)
            if (!isStillCurrent) return@execute

            runCatching {
                LiveMatchNotifier.post(
                    appContext,
                    snapshot,
                    Icon.createWithBitmap(homeBitmap),
                    Icon.createWithBitmap(awayBitmap)
                )
            }
        }
    }
}
