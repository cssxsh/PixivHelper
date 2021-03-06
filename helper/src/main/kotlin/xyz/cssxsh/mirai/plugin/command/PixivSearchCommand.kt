package xyz.cssxsh.mirai.plugin.command

import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.descriptor.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*
import okio.ByteString.Companion.toByteString
import xyz.cssxsh.mirai.plugin.*
import xyz.cssxsh.mirai.plugin.model.*
import xyz.cssxsh.mirai.plugin.tools.*

object PixivSearchCommand : SimpleCommand(
    owner = PixivHelperPlugin,
    "search", "搜索", "搜图",
    description = "PIXIV搜索指令，通过https://saucenao.com/"
) {

    @ExperimentalCommandDescriptors
    @ConsoleExperimentalApi
    override val prefixOptional: Boolean = true

    private fun MessageChain.getQuoteImage(): Image {
        val quote = requireNotNull(findIsInstance<QuoteReply>()) { "没有回复消息" }
        return requireNotNull(PixivHelperListener.images[quote.source.metadata()]) { "图片历史未找到" }
    }

    private suspend fun other(image: Image) = ImageSearcher.other(url = image.queryUrl())

    private suspend fun pixiv(image: Image) = ImageSearcher.pixiv(url = image.queryUrl())

    private suspend fun json(image: Image) = ImageSearcher.json(url = image.queryUrl())

    private fun List<PixivSearchResult>.save(image: Image) = filter { it.similarity > MIN_SIMILARITY }.apply {
        (maxByOrNull { it.similarity } ?: return@apply).copy(md5 = image.md5.toByteString().hex()).save()
    }

    @Handler
    suspend fun CommandSenderOnMessage<*>.search(image: Image = fromEvent.message.getQuoteImage()) = withHelper {
        logger.info { "搜索 ${image.queryUrl()}" }
        val cache = image.findSearchResult()
        if (cache != null) return@withHelper cache.getContent()

        if (ImageSearcher.key.isNotBlank()) {
            json(image).run {
                filterIsInstance<PixivSearchResult>().save(image).ifEmpty { this }
            }
        } else {
            pixiv(image).save(image).ifEmpty { other(image) }
        }.maxByOrNull { it.similarity }?.getContent() ?: "没有搜索结果"
    }
}