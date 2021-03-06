package xyz.cssxsh.mirai.plugin.command

import kotlinx.coroutines.flow.toList
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import xyz.cssxsh.mirai.plugin.*
import xyz.cssxsh.pixiv.apps.*

object PixivMarkCommand : CompositeCommand(
    owner = PixivHelperPlugin,
    "mark", "bookmark",
    description = "PIXIV收藏指令"
) {

    @SubCommand
    @Description("添加指定作品收藏，并设定TAG")
    suspend fun CommandSenderOnMessage<*>.add(pid: Long, vararg words: String) = withHelper {
        illustBookmarkAdd(pid = pid, tags = words.toSet())
        "收藏${pid} -> [${words.joinToString()}] 成功"
    }

    @SubCommand
    @Description("删除指定作品收藏")
    suspend fun CommandSenderOnMessage<*>.delete(pid: Long) = withHelper {
        illustBookmarkDelete(pid = pid).let {
            "取消收藏${pid}成功, $it"
        }
    }

    @SubCommand
    @Description("随机发送一个收藏的作品")
    suspend fun CommandSenderOnMessage<*>.random(tag: String? = null) = sendIllust(flush = false) {
        getBookmarks(uid = info().user.uid, tag = tag).also {
            addCacheJob(name = "MARK_RANDOM", reply = false) { it }
        }.toList().flatten().write().random()
    }

    @SubCommand
    @Description("显示收藏列表")
    suspend fun CommandSenderOnMessage<*>.list() = withHelper {
        getBookmarkTagInfos().toList().flatten().joinToString("\n") { (name, count) ->
            "[${name}] -> $count"
        }
    }
}