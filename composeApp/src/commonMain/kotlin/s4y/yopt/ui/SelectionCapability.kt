package s4y.yopt.ui

expect val supportsTextSelection: Boolean

/**
 * Compose 1.11.0 macOS native: key-event classification (`isCopyKeyEvent`) is not implemented and
 * throws on any key reaching a selection-capable widget, so Cmd+C cannot be handled natively there.
 * When true, the response area intercepts the key and reuses the text toolbar's copy callback.
 * Every other target handles Cmd+C natively, so the interceptor must stay off to preserve real
 * selection copy.
 */
expect val needsCopyKeyInterceptor: Boolean
