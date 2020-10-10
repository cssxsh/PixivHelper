package xyz.cssxsh.mirai.plugin.command

import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.message.MessageEvent
import xyz.cssxsh.mirai.plugin.PixivHelperLogger
import xyz.cssxsh.mirai.plugin.PixivHelperPlugin
import xyz.cssxsh.mirai.plugin.data.PixivCacheData
import xyz.cssxsh.mirai.plugin.getHelper
import xyz.cssxsh.mirai.plugin.getIllustInfo
import xyz.cssxsh.pixiv.api.app.userFollowAdd
import xyz.cssxsh.pixiv.data.app.UserInfo

@Suppress("unused")
object PixivFollowCommand : CompositeCommand(
    PixivHelperPlugin,
    "follow",
    description = "缓存指令",
    prefixOptional = true
), PixivHelperLogger {

    /**
     * 关注色图作者
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.good() = getHelper().runCatching {
        PixivCacheData.values.mapNotNull { pid ->
            getIllustInfo(pid).takeIf { info ->
                info.totalBookmarks ?: 0 >= 10_000 && info.sanityLevel > 4
            }
        }.fold(emptySet<UserInfo>()) { acc, info ->
            if (info.user.isFollowed == true) {
                acc
            } else {
                acc + info.user
            }
        }.count { user ->
            logger.verbose("添加关注(${user.id})[${user.name}]")
            runCatching {
                userFollowAdd(user.id).let {
                    logger.verbose(it.toString())
                }
            }.isSuccess
        }
    }.onSuccess {
        quoteReply("关注添加成功, 共${it}个新关注")
    }.onFailure {
        quoteReply("关注添加失败， ${it.message}")
    }.isSuccess
}