package xyz.cssxsh.mirai.plugin.data

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import xyz.cssxsh.pixiv.client.PixivConfig

object PixivConfigData : AutoSavePluginConfig("PixivConfig") {

    /**
     * 默认助手配置
     */
    var default: PixivConfig by value()

    /**
     * 特定助手配置
     */
    val configs: MutableMap<Long, PixivConfig> by value()

    /**
     * 作品信息是否为简单构造
     */
    val simpleInfo: MutableMap<Long, Boolean> by value()
}