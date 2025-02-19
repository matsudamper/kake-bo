package net.matsudamper.money.backend.graphql.exception
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.PrintWriter

class GraphQlMultiException(
    private val exceptions: List<Throwable>,
) : Exception(exceptions[0].message, exceptions[0].cause) {
    override val message: String
        get() = buildString {
            printStackTrace(
                textWriter = {
                    appendLine(it)
                },
                exceptionWriter = { e ->
                    appendLine(
                        ByteArrayOutputStream().use { outputStream ->
                            PrintStream(outputStream).use { printStream ->
                                e.printStackTrace(stream = printStream)
                            }
                            outputStream.toString(Charsets.UTF_8)
                        },
                    )
                },
            )
        }

    override fun printStackTrace() {
        printStackTrace(
            textWriter = {
                System.err.println(it)
            },
            exceptionWriter = {
                it.printStackTrace(System.err)
            },
        )
    }

    override fun printStackTrace(s: PrintStream?) {
        s ?: return
        printStackTrace(
            textWriter = {
                s.println(it)
            },
            exceptionWriter = {
                it.printStackTrace(s)
            },
        )
    }

    override fun printStackTrace(s: PrintWriter?) {
        s ?: return
        printStackTrace(
            textWriter = {
                s.println(it)
            },
            exceptionWriter = {
                it.printStackTrace(s)
            },
        )
    }

    private fun printStackTrace(
        textWriter: (String) -> Unit,
        exceptionWriter: (Throwable) -> Unit,
    ) {
        exceptions.forEachIndexed { index, item ->
            textWriter("MultiException stack ${index + 1} of ${exceptions.size}")
            exceptionWriter(item)
        }
    }
}
