@file:Suppress("unused")

package xyz.cssxsh.mirai.plugin.command

import com.soywiz.klock.PatternDateFormat
import com.soywiz.klock.parseDate
import com.soywiz.klock.wrapped.WDate
import com.soywiz.klock.wrapped.wrapped
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.utils.MiraiLogger
import xyz.cssxsh.mirai.plugin.*
import xyz.cssxsh.mirai.plugin.data.PixivHelperSettings
import xyz.cssxsh.pixiv.RankMode
import xyz.cssxsh.pixiv.api.app.*
import xyz.cssxsh.pixiv.client.exception.NotLoginException
import xyz.cssxsh.pixiv.tool.addIllustFollowListener

object PixivMethod : CompositeCommand(
    PixivHelperPlugin,
    "pixiv",
    description = "pixiv 基本方法"
) {

    private val logger: MiraiLogger get() = PixivHelperPlugin.logger

    /**
     * 设置代理
     * @param proxy 代理URL
     */
    @SubCommand
    fun ConsoleCommandSender.proxy(proxy: String) {
        logger.info(PixivHelperSettings.config.proxy + " -> " +  proxy)
        PixivHelperSettings.config.proxy = proxy
    }

    /**
     * 获取助手信息
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.info() = getHelper().runCatching {
        (authInfo ?: throw NotLoginException()).run {
            "账户：${user.account} \nPixivID: ${user.uid} \nToken: $refreshToken"
        }
    }.onSuccess {
        quoteReply(it)
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 登录 通过 用户名，密码
     * @param username 用户名
     * @param password 密码
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.login(
        username: String,
        password: String
    ) = getHelper().runCatching {
        login(username, password)
    }.onSuccess {
        quoteReply("${it.user.name} 登陆成功，Token ${it.refreshToken}")
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 登录 通过 Token
     * @param token refreshToken
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.refresh(
        token: String
    ) = getHelper().runCatching {
        refresh(token)
    }.onSuccess {
        quoteReply("${it.user.name} 登陆成功, Token ${it.refreshToken}")
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 排行榜
     * @param type 模式名 [RankMode]
     * @param date 日期 yyyy-MM-dd
     * @param index 排名
     */
    @SubCommand
    @Description("type by in DAY, DAY_MALE, DAY_FEMALE, WEEK_ORIGINAL, WEEK_ROOKIE, WEEK, MONTH, DAY_MANGA")
    suspend fun CommandSenderOnMessage<MessageEvent>.rank(
        type: String,
        date: String,
        index: Long
    ) = getHelper().runCatching {
        val rankMode: RankMode = enumValueOf(type.also {
            require("18" !in it) { "R18禁止！" }
        })
        val wDate: WDate = PatternDateFormat("y-M-d").parseDate(date).wrapped

        buildMessage(illustRanking(date = wDate, mode = rankMode, offset = index.positiveLongCheck()).illusts.first())
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 当前排行榜
     * @param type 模式名 [RankMode]
     * @param index 排名
     */
    @SubCommand
    @Description("type by in DAY, DAY_MALE, DAY_FEMALE, WEEK_ORIGINAL, WEEK_ROOKIE, WEEK, MONTH, DAY_MANGA")
    suspend fun CommandSenderOnMessage<MessageEvent>.now(
        type: String,
        index: Long
    ) = getHelper().runCatching {
        val rankMode: RankMode = enumValueOf(type.also {
            require("18" !in it) { "R18禁止！" }
        })
        buildMessage(illustRanking(mode = rankMode, offset = index.positiveLongCheck()).illusts.first())
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 随机排行榜
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.random() = getHelper().runCatching {
        val rankMode: RankMode = RankMode.values().random()
        buildMessage(illustRanking(mode = rankMode).illusts.random())
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 作品详情
     * @param pid 作品ID
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.detail(
        pid: Long
    ) = getHelper().runCatching {
        buildMessage(illustDetail(pid.positiveLongCheck()).illust)
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 用户作品
     * @param uid 用户ID
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.user(
        uid: Long
    ) = getHelper().runCatching {
        buildMessage(userIllusts(uid.positiveLongCheck()).illusts.first())
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 搜索
     * @param word 关键词
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.search(
        word: String,
        index: Int
    ) = getHelper().runCatching {
        require(index in 1..30) {  "index 的范围在1~30" }
        buildMessage(searchIllust(word).illusts[index - 1])
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 关注
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.follow() = getHelper().runCatching {
        buildMessage(illustFollow().illusts.random())
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 监听
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.listen() = getHelper().runCatching {
        addIllustFollowListener {
            buildMessage(it).forEach { message -> reply(message) }
        }
    }.onSuccess {
        quoteReply("监听任务添加成功")
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess

    /**
     * 书签
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.bookmark() = getHelper().runCatching {
        buildMessage(userBookmarksIllust(uid = (authInfo ?: throw NotLoginException()).user.uid).illusts.random())
    }.onSuccess { list ->
        list.forEach { quoteReply(it) }
    }.onFailure {
        quoteReply(it.toString())
    }.isSuccess
}