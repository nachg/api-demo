package com.apidemo.util


import org.xpathqs.framework.log.NoStyleBodyMessage
import org.xpathqs.framework.util.DateTimeUtil.NOW
import org.xpathqs.framework.util.DateTimeUtil.toStringValue
import org.xpathqs.log.BaseLogger
import org.xpathqs.log.Logger
import org.xpathqs.log.abstracts.IArgsProcessor
import org.xpathqs.log.abstracts.IBodyProcessor
import org.xpathqs.log.abstracts.IStreamLog
import org.xpathqs.log.message.IMessage
import org.xpathqs.log.printers.StreamLogPrinter
import org.xpathqs.log.printers.args.*
import org.xpathqs.log.printers.body.BodyProcessorImpl
import org.xpathqs.log.printers.body.HierarchyBodyProcessor
import org.xpathqs.log.printers.body.StyledBodyProcessor
import org.xpathqs.log.restrictions.RestrictionRuleHard
import org.xpathqs.log.restrictions.RestrictionRuleSoft
import org.xpathqs.log.restrictions.source.ExcludeByRootMethodClsSimple
import org.xpathqs.log.restrictions.value.ExcludeTags
import org.xpathqs.log.restrictions.value.IncludeTags
import org.xpathqs.log.style.Style
import org.xpathqs.log.style.StyledString.Companion.defaultStyles
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString

class ThreadLogProcessor(
    origin: IArgsProcessor,
) : ArgsProcessorDecorator(origin) {
    override fun process(msg: IMessage): String {
        return "[Thread-${Thread.currentThread().id}]"
    }
}

object ApiLogger: ApiLoggerCls()
open class ApiLoggerCls(
    consoleLog: Logger = Logger(
        streamPrinter = StreamLogPrinter(
            argsProcessor =
                StyleArgsProcessor(
                    TimeArgsProcessor(
                        ThreadLogProcessor(
                            NoArgsProcessor()
                        )
                    ),
                    Style(textColor = 60)
                ),
            bodyProcessor =
                StyledBodyProcessor(
                    HierarchyBodyProcessor(
                        BodyProcessorImpl()
                    )
                ),
            writer = System.out
        ),
        restrictions = listOf(
            RestrictionRuleSoft(
                rule = IncludeTags(ApiHelper.REQUEST_JSON),
                source = ExcludeByRootMethodClsSimple(
                    cls = "Auth",
                    method = "login"
                )
            )
        )
    ),
    allureLog: Logger = Logger(
        notifiers =
            arrayListOf(
                com.apidemo.util.AllureApiLogCallback()
            ),
        restrictions = listOf(
            RestrictionRuleHard(
                rule = ExcludeTags("trace")
            ),
            RestrictionRuleHard(
                source = ExcludeByRootMethodClsSimple(
                    cls = "Auth",
                    method = "login"
                )
            )
        )
    ),
    fileLog: Logger = Logger(
        streamPrinter = ThreadFilePrinter(
            argsProcessor = TimeArgsProcessor(
                TagArgsProcessor()
            )
        )
    )
): BaseLogger(
    loggers = arrayListOf(
        consoleLog,
        allureLog,
        fileLog
    ),
    stylesheet = defaultStyles
)

class ThreadFilePrinter(
    protected val argsProcessor: IArgsProcessor
    = TimeArgsProcessor(
        NoArgsProcessor()
    ),
    protected val bodyProcessor: IBodyProcessor
    = HierarchyBodyProcessor(
        NoStyleBodyMessage()
    )
) : IStreamLog {
    private val now = NOW.toStringValue()
    private val ts = System.currentTimeMillis()
    private val filesMap = HashMap<String, Writer>()

    fun getFileByThread(): Writer {
        val thread = Thread.currentThread()
        val key = thread.id.toString() + ".out"
        return filesMap.getOrPut(
            key
        ) {
            val path = Paths.get(
                "build/logs/$now/$ts"
            )
            Files.createDirectories(path)
            val f = Paths.get("${path.absolutePathString()}/$key").toFile()
            f.createNewFile()
            OutputStreamWriter(
                f.outputStream()
            )
        }
    }

    override fun onLog(msg: IMessage) {
        val out = getFileByThread()
        out.write(
            argsProcessor.processArgs(msg) + " " + bodyProcessor.processBody(msg) + "\n"
        )
        out.flush()
    }
}