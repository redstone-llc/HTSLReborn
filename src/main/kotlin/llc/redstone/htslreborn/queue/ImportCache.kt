package llc.redstone.htslreborn.queue

import com.google.gson.*
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.data.*
import llc.redstone.htslreborn.data.enums.Sound
import llc.redstone.htslreborn.ui.working.ContainerQueueEntry
import llc.redstone.htslreborn.utils.HousingUtils
import llc.redstone.htslreborn.utils.NbtHelper
import net.minecraft.nbt.NbtIo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

object ImportCache {
    private const val VERSION = 2
    private val gson = Gson()
    private var pending: Pending? = null
    private var committed = false

    private val types: Map<String, KClass<*>> by lazy { indexTypes() }
    private val names: Map<KClass<*>, String> by lazy { types.entries.associate { (name, cls) -> cls to name } }
    private val defaults = HashMap<KClass<*>, Any?>()

    fun key(container: ScriptContainer): String = listOf(
        container.context.name,
        container.target.name.orEmpty(),
        container.target.trigger.orEmpty(),
    ).joinToString("\u0000")

    fun stage(entry: ContainerQueueEntry) {
        val container = entry.container
        committed = false
        if (container == null || container.context == ImportContext.DEFAULT) {
            pending = null
            return
        }
        val house = HousingUtils.houseName()
        if (house == null) {
            pending = null
            return
        }
        pending = runCatching {
            Pending(house, key(container), encodeActions(container.actions))
        }.onFailure {
            HTSLReborn.LOGGER.warn("Failed to snapshot import for {}", key(container).replace('\u0000', '/'), it)
        }.getOrNull()
    }

    fun commit() {
        val pending = pending ?: return
        this.pending = null
        runCatching {
            val file = cacheFile(pending.house, pending.key)
            Files.createDirectories(file.parent)
            val root = JsonObject()
            root.addProperty("v", VERSION)
            root.add("a", pending.actions)
            Files.writeString(file, gson.toJson(root))
            committed = true
            HTSLReborn.LOGGER.info("Cached import for {}", pending.key.replace('\u0000', '/'))
        }.onFailure {
            HTSLReborn.LOGGER.warn("Failed to write import cache for {}", pending.key, it)
        }
    }

    fun discard() {
        pending = null
    }

    fun invalidateIfUncommitted(container: ScriptContainer?) {
        if (committed) {
            committed = false
            return
        }
        if (container == null) return
        val house = HousingUtils.houseName() ?: return
        val file = cacheFile(house, key(container))
        runCatching { Files.deleteIfExists(file) }
    }

    fun load(container: ScriptContainer): List<Action>? {
        if (container.context == ImportContext.DEFAULT) return null
        val house = HousingUtils.houseName()
        val key = key(container)
        if (house == null) {
            HTSLReborn.LOGGER.info("Import cache skipped for {}: house name is unknown", key.replace('\u0000', '/'))
            return null
        }
        val file = cacheFile(house, key)
        if (!Files.isRegularFile(file)) {
            HTSLReborn.LOGGER.info("Import cache missing for {} in {}", key.replace('\u0000', '/'), house)
            return null
        }
        val root = runCatching { gson.fromJson(readCache(file), JsonObject::class.java) }.getOrElse {
            HTSLReborn.LOGGER.warn("Failed to read import cache {}", file, it)
            return null
        } ?: return null
        val version = root.get("v")?.asInt ?: root.get("version")?.asInt ?: return null
        if (version != 1 && version != VERSION) return null
        val actions = (root.get("a") ?: root.get("actions"))?.asJsonArray ?: return null
        return runCatching { decodeActions(actions) }.getOrElse {
            HTSLReborn.LOGGER.warn("Failed to read cached actions {}", file, it)
            null
        }
    }

    private fun readCache(file: Path): String {
        val bytes = Files.readAllBytes(file)
        val input = if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            GZIPInputStream(ByteArrayInputStream(bytes))
        } else {
            ByteArrayInputStream(bytes)
        }
        return input.bufferedReader().use { it.readText() }
    }

    private fun encodeActions(actions: List<Action>): JsonArray {
        val array = JsonArray()
        actions.forEach { array.add(encodeTyped(it)) }
        return array
    }

    private fun decodeActions(array: JsonArray): List<Action> =
        array.map { decodeValue(it, Action::class.createType()) as Action }

    private fun encodeTyped(value: Any): JsonElement {
        if (value is Action.CustomAction) error("Custom actions can't be cached")
        val cls = value::class
        var typeName = names[cls] ?: cls.java.name
        if (value is Condition && value.inverted) typeName = "!$typeName"
        cls.objectInstance?.let { return JsonPrimitive(typeName) }

        val args = JsonObject()
        val ctor = cls.primaryConstructor
        if (ctor != null) {
            val props = cls.memberProperties.associateBy { it.name }
            val fallback = defaultInstance(cls)
            for (param in ctor.parameters) {
                val name = param.name ?: continue
                val prop = props[name] ?: continue
                val current = prop.getter.call(value)
                if (fallback != null && prop.getter.call(fallback) == current) continue
                if (current == null && param.isOptional) continue
                args.add(name, encodeValue(current))
            }
        }
        if (args.isEmpty) return JsonPrimitive(typeName)
        return JsonArray().apply {
            add(typeName)
            add(args)
        }
    }

    private fun encodeValue(value: Any?): JsonElement = when (value) {
        null -> JsonNull.INSTANCE
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value)
        is Enum<*> -> JsonPrimitive(value.name)
        is InventorySlot -> JsonArray().apply {
            add(names[value::class] ?: value::class.java.simpleName)
            add(value.slot)
        }
        is ItemStack -> encodeItem(value)
        is List<*> -> JsonArray().apply { value.forEach { add(encodeValue(it)) } }
        else -> encodeTyped(value)
    }

    private fun decodeValue(el: JsonElement, type: KType): Any? {
        if (el.isJsonNull) return null
        val classifier = type.classifier as? KClass<*> ?: error("Unsupported type $type")
        return when {
            classifier == String::class -> el.asString
            classifier == Boolean::class -> el.asBoolean
            classifier == Int::class -> el.asInt
            classifier == Long::class -> el.asLong
            classifier == Double::class -> el.asDouble
            classifier == Float::class -> el.asFloat
            classifier == List::class -> {
                val elementType = type.arguments.first().type ?: error("Untyped list")
                el.asJsonArray.map { decodeValue(it, elementType) }
            }
            classifier == ItemStack::class -> decodeItem(el.asJsonObject)
            classifier == InventorySlot::class -> decodeSlot(el)
            classifier.java.isEnum -> decodeEnum(el, classifier.java)
            el.isJsonPrimitive -> decodeNamed(el.asString, JsonObject())
            el.isJsonArray -> {
                val array = el.asJsonArray
                val args = if (array.size() > 1 && array[1].isJsonObject) array[1].asJsonObject else JsonObject()
                decodeNamed(array[0].asString, args)
            }
            else -> decodeLegacy(el.asJsonObject)
        }
    }

    private fun decodeNamed(typeName: String, args: JsonObject): Any {
        val inverted = typeName.startsWith("!")
        val name = if (inverted) typeName.substring(1) else typeName
        val cls = types[name] ?: Class.forName(name).kotlin
        cls.objectInstance?.let { return it }
        val ctor = cls.primaryConstructor ?: error("No constructor for ${cls.qualifiedName}")
        val values = ctor.parameters.mapNotNull { param ->
            val paramName = param.name ?: return@mapNotNull null
            if (!args.has(paramName)) return@mapNotNull null
            param to decodeValue(args.get(paramName), param.type)
        }.toMap()
        val instance = ctor.callBy(values)
        if (instance is Condition) {
            instance.inverted = inverted || (args.has("inverted") && args.get("inverted").asBoolean)
        }
        return instance
    }

    private fun decodeLegacy(obj: JsonObject): Any {
        val args = obj.getAsJsonObject("args") ?: JsonObject()
        return decodeNamed(obj.get("type").asString, args)
    }

    private fun decodeEnum(el: JsonElement, enumClass: Class<*>): Any {
        val name = if (el.isJsonPrimitive) el.asString else el.asJsonObject.get("name").asString
        val cls = if (el.isJsonObject && el.asJsonObject.has("type")) {
            Class.forName(el.asJsonObject.get("type").asString)
        } else {
            enumClass
        }
        return cls.enumConstants.first { (it as Enum<*>).name == name }
    }

    private fun decodeSlot(el: JsonElement): InventorySlot {
        val (typeName, slot) = if (el.isJsonArray) {
            el.asJsonArray[0].asString to el.asJsonArray[1].asInt
        } else {
            val obj = el.asJsonObject
            val type = obj.get("type").asString.substringAfterLast('.').substringAfterLast('$')
            type to obj.get("slot").asInt
        }
        return when (typeName.substringAfterLast('.')) {
            "HandSlot" -> InventorySlot.HandSlot()
            "FirstAvailableSlot" -> InventorySlot.FirstAvailableSlot()
            "HelmetSlot" -> InventorySlot.HelmetSlot()
            "ChestplateSlot" -> InventorySlot.ChestplateSlot()
            "LeggingsSlot" -> InventorySlot.LeggingsSlot()
            "BootsSlot" -> InventorySlot.BootsSlot()
            "HotbarSlot" -> InventorySlot.HotbarSlot(slot + 1)
            "PlayerInventorySlot" -> InventorySlot.PlayerInventorySlot(slot - 8)
            "ManualInput" -> InventorySlot.ManualInput(slot)
            else -> error("Unknown inventory slot $typeName")
        }
    }

    private fun encodeItem(item: ItemStack): JsonObject {
        val obj = JsonObject()
        item.slot?.let { obj.addProperty("s", it) }
        val stack = item.stack ?: return obj
        val tag = NbtHelper.serializeItemStack(stack).getOrNull() ?: error("Could not serialize item")
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { NbtIo.write(tag, it) }
        obj.addProperty("n", Base64.getEncoder().encodeToString(bytes.toByteArray()))
        return obj
    }

    private fun decodeItem(obj: JsonObject): ItemStack {
        val slot = obj.get("s")?.asInt ?: obj.get("slot")?.asInt
        val nbt = obj.get("n") ?: obj.get("nbt")
        if (nbt == null) return ItemStack(slot = slot)
        val bytes = Base64.getDecoder().decode(nbt.asString)
        val tag = DataInputStream(ByteArrayInputStream(bytes)).use { NbtIo.read(it) }
        val stack = NbtHelper.deserializeItemStack(tag).getOrNull() ?: error("Could not read cached item")
        return ItemStack(stack, slot)
    }

    private fun defaultInstance(cls: KClass<*>): Any? = defaults.getOrPut(cls) {
        val ctor = cls.primaryConstructor ?: return@getOrPut null
        runCatching { ctor.callBy(emptyMap()) }.getOrNull()
    }

    private fun indexTypes(): Map<String, KClass<*>> {
        val leaves = mutableListOf<KClass<*>>()
        fun walk(cls: KClass<*>) {
            val nested = cls.sealedSubclasses
            if (nested.isEmpty()) {
                leaves.add(cls)
            } else {
                nested.forEach { walk(it) }
                if (cls.objectInstance != null) leaves.add(cls)
            }
        }
        listOf(Action::class, Condition::class, Location::class, Time::class, InventorySlot::class, Sound::class)
            .forEach { walk(it) }
        val duplicated = leaves.groupingBy { it.simpleName }.eachCount().filterValues { it > 1 }.keys
        return leaves.associate { cls ->
            val simple = cls.simpleName ?: cls.java.name
            val name = if (simple in duplicated) {
                "${cls.java.enclosingClass?.simpleName ?: simple}.$simple"
            } else {
                simple
            }
            name to cls
        }
    }

    private fun cacheFile(house: String, key: String): Path {
        val safe = house.replace(Regex("""[^\w .\-]"""), "_").trim().trimEnd('.').take(48).ifBlank { "house" }
        val name = "$safe-${sha256("$house\u0000$key".toByteArray()).take(12)}.json"
        return HTSLReborn.MC.gameDirectory.toPath().resolve("htsl/.cache").resolve(name)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private data class Pending(val house: String, val key: String, val actions: JsonArray)
}
