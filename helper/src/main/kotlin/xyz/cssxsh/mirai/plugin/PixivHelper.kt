@file:Suppress("unused")

package xyz.cssxsh.mirai.plugin

import com.soywiz.klock.KlockLocale
import com.soywiz.klock.PatternDateFormat
import com.soywiz.klock.locale.chinese
import com.soywiz.klock.wrapped.WDateTimeTz
import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import xyz.cssxsh.mirai.plugin.data.*
import xyz.cssxsh.pixiv.client.*
import xyz.cssxsh.pixiv.data.AuthResult
import xyz.cssxsh.pixiv.data.app.IllustInfo
import java.util.concurrent.ArrayBlockingQueue

/**
 * 助手实例
 */
class PixivHelper(val contact: Contact) : SimplePixivClient(
    parentCoroutineContext = PixivHelperPlugin.coroutineContext,
    coroutineName = "PixivHelper:${contact}",
    config = PixivConfig()
), PixivHelperLogger {

    companion object {
        val DATE_FORMAT_CHINESE = PatternDateFormat("YYYY-MM-dd HH:mm:ss", KlockLocale.chinese)
    }

    override var config: PixivConfig by ConfigDelegate(contact)

    override var authInfo: AuthResult.AuthInfo? by AuthInfoDelegate(contact)

    override var expiresTime: WDateTimeTz by ExpiresTimeDelegate(contact)

    fun getExpiresTimeText() = expiresTime.format(DATE_FORMAT_CHINESE)

    val historyQueue by lazy {
        ArrayBlockingQueue<Long>(PixivHelperSettings.minInterval)
    }

    var minSanityLevel = 1
        set(value) {
            field = minOf(value, 6)
        }

    var minBookmarks: Long = 0

    var simpleInfo: Boolean
        get() = PixivConfigData.simpleInfo.getOrPut(contact.toString()) { contact !is Friend }
        set(value) {
            PixivConfigData.simpleInfo[contact.toString()] = value
        }

    private var cacheJob: Job? = null

    private val cacheList: MutableList<Pair<String, List<IllustInfo>>> = mutableListOf()

    fun addCacheJob(name: String, list: List<IllustInfo>): Boolean = cacheList.add(name to list).also {
        if (cacheJob?.takeIf { it.isActive } == null) {
            cacheJob = launch(Dispatchers.IO) {
                while (isActive && cacheList.isNotEmpty()) {
                    cacheList.removeFirst().let { (name, list) ->
                        list.sortedBy {
                            it.pid
                        }.apply {
                            runCatching {
                                reply("任务<$name>有{${first().pid}...${last().pid}}共${size}个新作品等待缓存")
                            }
                        }.runCatching {
                            size to count { illust ->
                                illust.pid in PixivCacheData || isActive && runCatching {
                                    getImages(illust)
                                }.onSuccess {
                                    delay(PixivHelperSettings.delayTime)
                                }.onFailure {
                                    logger.warning({ "任务<$name>获取作品(${illust.pid})[${illust.title}]错误" }, it)
                                    reply("任务<$name>获取作品(${illust.pid})[${illust.title}]错误, ${it.message}")
                                }.isSuccess
                            }
                        }.onSuccess { (total, success) ->
                            runCatching {
                                reply("任务<$name>缓存完毕, 共${total}个新作品, 缓存成功${success}个")
                            }
                        }.onFailure {
                            runCatching {
                                reply("任务<$name>缓存失败, ${it.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun cacheStop() = cacheJob?.apply {
        cacheList.clear()
        cancelAndJoin()
    }

    var tagJob: Job? = null

    var followJob: Job? = null

    override fun config(block: PixivConfig.() -> Unit) =
        config.apply(block).also { config = it }

    override suspend fun refresh(token: String) = super.refresh(token).also {
        logger.info { "$it by RefreshToken: $token, ExpiresTime: ${expiresTime.format(DATE_FORMAT_CHINESE)}" }
    }

    override suspend fun login(mailOrPixivID: String, password: String) = super.login(mailOrPixivID, password).also {
        logger.info { "$it by Account: $mailOrPixivID, ExpiresTime: ${expiresTime.format(DATE_FORMAT_CHINESE)}" }
    }

    /**
     * 给这个助手的联系人发送消息
     */
    @JvmSynthetic
    suspend fun reply(message: Message): MessageReceipt<Contact> =
        contact.sendMessage(message)

    /**
     * 给这个助手的联系人发送文本消息
     */
    @JvmSynthetic
    suspend fun reply(plain: String): MessageReceipt<Contact> =
        contact.sendMessage(PlainText(plain))
}