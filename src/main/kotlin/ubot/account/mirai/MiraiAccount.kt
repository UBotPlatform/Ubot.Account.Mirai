package ubot.account.mirai

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.long
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory.INSTANCE.newBot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsVoice
import net.mamoe.mirai.utils.LoggerAdapters.asMiraiLogger
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import ubot.common.*
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MiraiAccount(private val event: UBotAccountEventEmitter,
                   private val bot: Bot)
    : BaseUBotAccount() {
    private val client = HttpClient()
    init {
        bot.coroutineContext[Job]?.invokeOnCompletion {
            client.close()
        }
        bot.eventChannel.run {
            subscribeAlways<MemberJoinEvent> {
                event.onMemberJoined(this.group.id.toString(), this.member.id.toString(), "")
            }
            subscribeAlways<MemberLeaveEvent> {
                event.onMemberLeft(this.group.id.toString(), this.member.id.toString())
            }
            subscribeAlways<FriendMessageEvent> {
                event.onReceiveChatMessage(
                    ChatMessageType.Private,
                    "",
                    this.sender.id.toString(),
                    toUBotMessage(this.message),
                    ChatMessageInfo()
                )
            }
            subscribeAlways<GroupMessageEvent> {
                event.onReceiveChatMessage(
                    ChatMessageType.Group,
                    this.group.id.toString(),
                    this.sender.id.toString(),
                    toUBotMessage(this.message),
                    ChatMessageInfo()
                )
            }
            subscribeAlways<BotInvitedJoinGroupRequestEvent> {
                val r = event.processGroupInvitation(
                    this.invitorId.toString(),
                    this.groupId.toString(),
                    ""
                )
                when (r.type) {
                    UBotEventResult.Type.Accept -> this.accept()
                    UBotEventResult.Type.Reject -> this.ignore()
                }
            }
            subscribeAlways<MemberJoinRequestEvent> {
                val r = event.processMembershipRequest(
                    this.groupId.toString(),
                    this.fromId.toString(),
                    "",
                    this.message
                )
                when (r.type) {
                    UBotEventResult.Type.Accept -> this.accept()
                    UBotEventResult.Type.Reject -> this.reject(message = r.reason ?: "")
                }
            }
            subscribeAlways<NewFriendRequestEvent> {
                val r = event.processFriendRequest(
                    this.fromId.toString(),
                    ""
                )
                when (r.type) {
                    UBotEventResult.Type.Accept -> this.accept()
                    UBotEventResult.Type.Reject -> this.reject()
                }
            }
        }
    }

    override suspend fun getGroupName(id: String): String {
        return bot.getGroupOrFail(id.toLong()).name
    }

    override suspend fun getMemberName(source: String, target: String): String {
        if (source.isEmpty()) {
            return bot.getFriendOrFail(target.toLong()).nick
        }
        return bot.getGroupOrFail(source.toLong())[target.toLong()]!!.nameCardOrNick
    }

    override suspend fun getSelfID(): String {
        return bot.id.toString()
    }

    override suspend fun getUserAvatar(id: String): String {
        return "http://q1.qlogo.cn/g?b=qq&nk=${id}&s=640"
    }

    override suspend fun getUserName(id: String): String {
        // stupid but useful
        val idLong = id.toLong()
        bot.getFriend(idLong)?.apply {
            return nick
        }
        bot.groups.forEach {
            it[idLong]?.apply {
                return nick
            }
        }
        return "unknown"
    }

    override suspend fun removeMember(source: String, target: String) {
        bot.getGroupOrFail(source.toLong()).getOrFail(target.toLong()).kick("")
    }

    override suspend fun sendChatMessage(type: Int, source: String, target: String, message: String) {
        val contact = when (type) {
            ChatMessageType.Group ->
                bot.getGroupOrFail(source.toLong())
            ChatMessageType.Private ->
                bot.getFriendOrFail(target.toLong())
            else ->
                throw IllegalArgumentException("invalid type")
        }
        val parsed = ChatMessageParser.parse(message)
        val chain = MessageChainBuilder()
        var hasConstrainSingle = false
        for (entity in parsed) {
            when (entity.type) {
                "text" -> entity.args.firstOrNull()?.let(::PlainText)
                "at" -> {
                    entity.args.firstOrNull()?.let {
                        when (it) {
                            "all" -> AtAll
                            else -> it.toLongOrNull()?.let { toAt ->
                                (contact as? Group)?.get(toAt)?.let { member ->
                                    At(member)
                                }
                            }
                        }
                    }
                }
                "face" -> entity.args.firstOrNull()?.toInt()?.let(::Face)
                "image" -> entity.fetchExternalResource {
                    it.uploadAsImage(contact)
                }
                "voice" -> entity.fetchExternalResource {
                    it.uploadAsVoice(contact)
                }
                "qq_app" -> entity.args.firstOrNull()?.let(::LightApp)
                "qq_service" -> entity.args.firstOrNull()?.let {
                    @OptIn(MiraiExperimentalApi::class)
                    SimpleServiceMessage(entity.namedArgs["service_id"]?.toIntOrNull() ?: 0, it)
                }
                else -> PlainText("不支持的消息")
            }?.also { msg: SingleMessage ->
                if (msg is ConstrainSingle) {
                    if (hasConstrainSingle) {
                        contact.sendMessage(chain.build())
                        chain.clear()
                    } else {
                        hasConstrainSingle = true
                    }
                }
                chain.add(msg)
            }
        }
        if (chain.isNotEmpty()) {
            contact.sendMessage(chain.build())
        }
    }

    @OptIn(ExperimentalContracts::class)
    private suspend inline fun <R> ChatMessageEntity.fetchExternalResource(block: (ExternalResource) -> R): R? {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        namedArgs["base64"]?.let { base64 ->
            return Base64.getDecoder().decode(base64).toExternalResource().use(block)
        }
        args.firstOrNull()?.let { url ->
            return client.get<InputStream>(url).use {
                runInterruptible(Dispatchers.IO) {
                    it.toExternalResource()
                }
            }.use(block)
        }
        return null
    }

    override suspend fun shutupAllMember(source: String, switch: Boolean) {
        bot.getGroupOrFail(source.toLong()).settings.isMuteAll = switch
    }

    override suspend fun shutupMember(source: String, target: String, duration: Int) {
        if (duration == 0) {
            bot.getGroupOrFail(source.toLong()).getOrFail(target.toLong()).unmute()
        } else {
            bot.getGroupOrFail(source.toLong()).getOrFail(target.toLong()).mute(duration * 60)
        }
    }

    override suspend fun getGroupList(): List<String> {
        return bot.groups.map {
            it.id.toString()
        }.toList()
    }

    override suspend fun getMemberList(id: String): List<String> {
        return bot.getGroupOrFail(id.toLong()).members.map {
            it.id.toString()
        }.toList()
    }

    override suspend fun getPlatformID(): String {
        return "QQ"
    }

    private suspend fun toUBotMessage(msg: Message): String {
        val builder = ChatMessageBuilder()
        for (it in msg.toMessageChain()) {
            when (it) {
                is PlainText -> builder.add(it.content)
                is At -> builder.add("at", it.target.toString())
                is AtAll -> builder.add("at", "all")
                is Face -> builder.add("face", it.id.toString())
                is Image -> builder.add("image", it.queryUrl())
                is Voice -> builder.add("voice", it.url ?: "")
                is LightApp -> builder.add("qq_app", it.content)
                is ServiceMessage -> builder.add(
                    ChatMessageEntity(
                        "qq_service",
                        listOf(it.content),
                        mapOf("service_id" to it.serviceId.toString())
                    )
                )
                is MessageMetadata -> {
                }
                else -> builder.add(it.contentToString())
            }
        }
        return builder.build()
    }
}

class MiraiCommand : CliktCommand() {
    private val ubotOp: String by argument("UBotOp")
    private val ubotAddr: String by argument("UBotAddr")
    private val qqId: Long by argument("QQID").long()
    private val qqPassword: String by argument("QQPassword")
    private val cache: Boolean by option(
        "--cache",
        help = "Enable or disable contact cache"
    ).flag("--no-cache", default = true)
    private val protocol: BotConfiguration.MiraiProtocol by option(
        "-p",
        "--protocol"
    ).enum<BotConfiguration.MiraiProtocol>(ignoreCase = true)
        .default(BotConfiguration.MiraiProtocol.ANDROID_PHONE)
    private val heartbeatStrategy: BotConfiguration.HeartbeatStrategy by option(
        "-hb",
        "--heartbeat"
    ).enum<BotConfiguration.HeartbeatStrategy>(ignoreCase = true)
        .default(BotConfiguration.HeartbeatStrategy.STAT_HB)
    private val level: Level by option(
        "--level"
    ).choice(
        "OFF" to Level.OFF,
        "ERROR" to Level.ERROR,
        "WARN" to Level.WARN,
        "INFO" to Level.INFO,
        "DEBUG" to Level.DEBUG,
        "TRACE" to Level.TRACE,
        "ALL" to Level.ALL,
        ignoreCase = true
    ).default(Level.ALL)
    private val workingDir: File by lazy {
        var appFolder = File(MiraiAccount::class.java.protectionDomain.codeSource.location.toURI())
        if (appFolder.isFile) {
            appFolder = appFolder.parentFile
        }
        val workingDir = File(appFolder, "Mirai${qqId}")
        workingDir.mkdir()
        check(workingDir.isDirectory) {
            "Failed to create instance folder for ${qqId}"
        }
        workingDir
    }

    @OptIn(MiraiInternalApi::class)
    override fun run() {
        ConfigurationBuilderFactory.newConfigurationBuilder().apply {
            newAppender("stdout", "Console")
                .add(newLayout("PatternLayout").apply {
                    addAttribute("pattern", "%d %level{length=1}/%logger: %notEmpty{[%marker]} %msg%n%throwable")
                })
                .let(::add)
            newRootLogger(level)
                .add(newAppenderRef("stdout"))
                .let(::add)
        }.let {
            Configurator.initialize(it.build())
        }
        MiraiLogger.setDefaultLoggerCreator { identity ->
            LogManager.getLogger(identity).asMiraiLogger()
        }
        runBlocking {
            val bot = newBot(qqId, qqPassword) {
                parentCoroutineContext = coroutineContext
                workingDir = this@MiraiCommand.workingDir
                protocol = this@MiraiCommand.protocol
                heartbeatStrategy = this@MiraiCommand.heartbeatStrategy
                fileBasedDeviceInfo()
                if (cache) {
                    enableContactCache()
                } else {
                    disableContactCache()
                }
            }
            bot.login()
            UBotClientHost.hostAccount(ubotOp, ubotAddr, "QQ${bot.id}") { event ->
                MiraiAccount(event, bot)
            }
            bot.closeAndJoin()
        }
    }
}

fun main(args: Array<String>) = MiraiCommand().main(args)