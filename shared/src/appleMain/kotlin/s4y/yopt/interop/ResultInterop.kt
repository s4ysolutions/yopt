package s4y.yopt.interop

/**
 * Apple-bridge helper. Kotlin `Result<T>` is a value class that K/N cannot export
 * to Swift, so suspend functions returning `Result<T>` cross the boundary as an
 * opaque `Any?` (the boxed Result). Swift has no way to read it.
 *
 * Swift calls the use case, gets that `Any?`, then passes it here. We unbox the
 * Result and rethrow its failure, which SKIE converts to a Swift error.
 * Success returns the unwrapped value (also `Any?`); Swift may ignore or cast it.
 */
@Throws(Throwable::class)
fun resultOrThrow(result: Any?): Any? {
    println("[ResultInterop] in: type=${result?.let { it::class.qualifiedName } ?: "null"}")
    @Suppress("UNCHECKED_CAST")
    val r = result as Result<Any?>
    println("[ResultInterop] isSuccess=${r.isSuccess} isFailure=${r.isFailure}")
    r.exceptionOrNull()?.let {
        println("[ResultInterop] failure: ${it::class.qualifiedName}: ${it.message}")
    }
    return r.getOrThrow()
}
