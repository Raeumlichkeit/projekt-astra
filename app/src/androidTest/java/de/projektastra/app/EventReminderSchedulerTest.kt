package de.projektastra.app

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Exercises Astra's real scheduling API without sending a notification or touching saved events. */
@RunWith(AndroidJUnit4::class)
class EventReminderSchedulerTest {
    @Test fun schedulesReplacesAndCancelsOnlyItsOwnFutureReminder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val key = "instrumentation-${UUID.randomUUID()}"
        val name = "astra-event-$key"
        val event = SavedSkyEvent(key, "Testereignis", "Test", Instant.now().plusSeconds(172800).epochSecond)
        fun awaitWork(condition: (List<WorkInfo>) -> Boolean): List<WorkInfo> {
            val deadline = SystemClock.uptimeMillis() + 10000
            while (true) {
                val work = manager.getWorkInfosForUniqueWork(name).get(5, TimeUnit.SECONDS)
                if (condition(work)) return work
                check(SystemClock.uptimeMillis() < deadline) { "Reminder state timed out: $work" }
                SystemClock.sleep(50)
            }
        }
        try {
            EventReminderScheduler.schedule(context, event, leadHours = 1)
            val first = awaitWork { it.count { work -> work.state == WorkInfo.State.ENQUEUED } == 1 }
                .single { it.state == WorkInfo.State.ENQUEUED }
            assertTrue("Reminder must remain in the future", first.nextScheduleTimeMillis > System.currentTimeMillis() + 3600000)

            EventReminderScheduler.schedule(context, event, leadHours = 2)
            val replaced = awaitWork { work -> work.any { it.id != first.id && it.state == WorkInfo.State.ENQUEUED } }
            assertEquals(1, replaced.count { !it.state.isFinished })
            assertFalse("Old request must no longer be active", replaced.any { it.id == first.id && !it.state.isFinished })

            EventReminderScheduler.cancel(context, key)
            val cancelled = awaitWork { it.isNotEmpty() && it.all { work -> work.state.isFinished } }
            assertTrue(cancelled.all { it.state == WorkInfo.State.CANCELLED })
        } finally {
            // No cancelAllWork/pruneWork: preserve all unrelated user and test reminders.
            manager.cancelUniqueWork(name).result.get(10, TimeUnit.SECONDS)
        }
    }
}
