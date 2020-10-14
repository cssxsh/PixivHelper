package xyz.cssxsh.mirai.plugin.command

import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.message.MessageEvent
import xyz.cssxsh.mirai.plugin.*
import xyz.cssxsh.mirai.plugin.data.BaseInfo
import xyz.cssxsh.mirai.plugin.data.PixivCacheData
import xyz.cssxsh.mirai.plugin.data.PixivStatisticalData

@Suppress("unused")
object PixivEroCommand : SimpleCommand(
    PixivHelperPlugin,
    "ero", "色图", "涩图",
    description = "色图指令",
    prefixOptional = true
), PixivHelperLogger {

    private fun PixivHelper.randomIllust(): BaseInfo = PixivCacheData.eros().random().takeIf { illust ->
        illust.pid !in historyQueue
    }?.also {
        if (historyQueue.remainingCapacity() == 0) historyQueue.take()
        historyQueue.put(it.pid)
    } ?: randomIllust()

    @Handler
    suspend fun CommandSenderOnMessage<MessageEvent>.handle() = getHelper().runCatching {
        PixivStatisticalData.eroAdd(id = fromEvent.sender.id).let {
            logger.verbose("${fromEvent.sender}第${it}次使用色图")
        }
        buildMessage(randomIllust())
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply("读取色图失败， ${it.message}")
    }.isSuccess
}

















