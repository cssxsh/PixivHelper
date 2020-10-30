package xyz.cssxsh.mirai.plugin.command

import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.message.MessageEvent
import xyz.cssxsh.mirai.plugin.*
import xyz.cssxsh.mirai.plugin.data.*

@Suppress("unused")
object PixivEroCommand : SimpleCommand(
    owner = PixivHelperPlugin,
    "ero", "色图", "涩图",
    description = "色图指令"
), PixivHelperLogger {

    override val prefixOptional: Boolean = true

    @Handler
    suspend fun CommandSenderOnMessage<MessageEvent>.handle() = getHelper().runCatching {
        if ("更色" in message.contentToString()) {
            minSanityLevel++
        } else {
            minSanityLevel = 1
        }
        if ("更好" !in message.contentToString()) {
            minBookmarks = 0
        }
        PixivCacheData.eros { info ->
            info.pid !in historyQueue && info.sanityLevel >= minSanityLevel && info.totalBookmarks > minBookmarks
        }.apply {
            PixivStatisticalData.eroAdd(user = fromEvent.sender).let {
                logger.verbose("${fromEvent.sender}第${it}次使用色图, 最小健全等级${minSanityLevel}, 最小收藏数${minBookmarks} 共找到${size} 张色图")
            }
        }.random().also { info ->
            minSanityLevel = info.sanityLevel
            minBookmarks = info.totalBookmarks
            historyQueue.apply {
                if (remainingCapacity() == 0) take()
                put(info.pid)
            }
        }.let {
            buildMessage(it)
        }
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply("读取色图失败， ${it.message}")
    }.isSuccess
}

















