package s4y.yopt.usecase

actual fun currentTimeMillis(): Long = js("Date.now()") as Long
