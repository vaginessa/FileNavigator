package com.w2sv.navigator.notifications.managers.abstrct

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.w2sv.androidutils.notifying.UniqueIds
import com.w2sv.navigator.notifications.NotificationResources
import slimber.log.i

abstract class MultiInstanceAppNotificationManager<A : MultiInstanceAppNotificationManager.BuilderArgs>(
    notificationChannel: NotificationChannel,
    notificationManager: NotificationManager,
    context: Context,
    resourcesBaseSeed: Int
) : AppNotificationManager<A>(notificationChannel, notificationManager, context) {

    private val notificationIds = UniqueIds(resourcesBaseSeed)
    private val pendingIntentRequestCodes = UniqueIds(resourcesBaseSeed)

    protected fun getNotificationResources(nPendingRequestCodes: Int): NotificationResources =
        NotificationResources(
            notificationIds.addNewId(),
            pendingIntentRequestCodes.addMultipleNewIds(nPendingRequestCodes)
        )

    fun cancelNotificationAndFreeResources(resources: NotificationResources) {
        notificationManager.cancel(resources.id)
        freeNotificationResources(resources)
    }

    private fun freeNotificationResources(resources: NotificationResources) {
        notificationIds.remove(resources.id)
        pendingIntentRequestCodes.removeAll(resources.actionRequestCodes.toSet())

        i { "Post-freeNotificationResources: NotificationIds: $notificationIds | pendingIntentRequestCodes: $pendingIntentRequestCodes" }
    }

    abstract class BuilderArgs(val resources: NotificationResources) :
        AppNotificationManager.BuilderArgs

    fun buildAndEmit(args: A) {
        super.buildAndEmit(args.resources.id, args)
    }
}