package net.matsudamper.money.frontend.common.base.notification

public sealed interface NotificationAccessState {
    public data object Granted : NotificationAccessState

    public data object NotGranted : NotificationAccessState
}