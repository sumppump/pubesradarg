@file:Suppress("NAME_SHADOWING")
package main.deserializer.channel

import com.badlogic.gdx.math.*
import main.*
import main.deserializer.CHTYPE_ACTOR
//import main.deserializer.actor.repl_layout_bunch
import main.struct.CharacterMoveComp
import main.struct.VehicleMoveComp
import main.struct.*
import main.struct.Archetype.*
import main.struct.NetGUIDCache.Companion.guidCache
import main.struct.cmd.PlayerStateCMD.selfID
import main.util.*
import java.util.concurrent.ConcurrentHashMap
import main.struct.cmd.receiveProperties

class ActorChannel(ChIndex: Int, client: Boolean = true): Channel(ChIndex, CHTYPE_ACTOR, client) {
    companion object: GameListener {
        init {
            register(this)
        }

        val actors = ConcurrentHashMap<NetworkGUID, Actor>()
        val visualActors = ConcurrentHashMap<NetworkGUID, Actor>()
        val airDropLocation = ConcurrentHashMap<NetworkGUID, Vector3>()
        val droppedItemLocation = ConcurrentHashMap<NetworkGUID, tuple2<Vector2, String>>()
        val droppedItemToItem = ConcurrentHashMap<NetworkGUID, NetworkGUID>()
        val droppedItemGroup = ConcurrentHashMap<NetworkGUID, DynamicArray<NetworkGUID?>>()
        val droppedItemCompToItem = ConcurrentHashMap<NetworkGUID, NetworkGUID>()
        val crateitems=ConcurrentHashMap<NetworkGUID,DynamicArray<NetworkGUID?>>()
        val corpseLocation = ConcurrentHashMap<NetworkGUID, Vector3>()
        val actorHasWeapons = ConcurrentHashMap<NetworkGUID, DynamicArray<NetworkGUID?>>()
        val weapons = ConcurrentHashMap<NetworkGUID, Actor>()
        val itemBag=ConcurrentHashMap<NetworkGUID,DynamicArray<NetworkGUID?>>()



        override fun onGameOver() {
            actors.clear()
            visualActors.clear()
            airDropLocation.clear()
            droppedItemLocation.clear()
            droppedItemGroup.clear()
            droppedItemCompToItem.clear()
            droppedItemToItem.clear()
            crateitems.clear()
            corpseLocation.clear()
            weapons.clear()
            actorHasWeapons.clear()
        }
    }

    var actor: Actor? = null

    override fun ReceivedBunch(bunch: Bunch) {
        if (client && bunch.bHasMustBeMappedGUIDs) {
            val NumMustBeMappedGUIDs = bunch.readUInt16()
            for (i in 0 until NumMustBeMappedGUIDs) {
                val guid = bunch.readNetworkGUID()
            }
        }
        ProcessBunch(bunch)
    }

    fun ProcessBunch(bunch: Bunch) {
        if (client && actor == null) {
            if (!bunch.bOpen) {
                return
            }
            SerializeActor(bunch)
            if (actor == null)
                return
        }
        if (!client && actor == null) {
            val clientChannel = inChannels[chIndex] ?: return
            actor = (clientChannel as ActorChannel).actor
            if (actor == null) return
        }
        val actor = actor!!
        if (actor.type == DroppedItem && bunch.bitsLeft() == 0)
            droppedItemLocation.remove(droppedItemToItem[actor.netGUID] ?: return)
        while (bunch.notEnd()) {
            //header
            val bHasRepLayout = bunch.readBit()
            val bIsActor = bunch.readBit()
            var repObj: NetGuidCacheObject?
            if (bIsActor) {
                repObj = NetGuidCacheObject(actor.type.name, actor.netGUID)
            } else {
                val (netguid, _subobj) = bunch.readObject()//SubObject, SubObjectNetGUID
                if (!client) {
                    if (_subobj == null)// The server should never need to create sub objects
                        continue
                    repObj = _subobj
                    bugln { "$actor hasSubObj $repObj" }
                } else {
                    val bStablyNamed = bunch.readBit()
                    if (bStablyNamed) {// If this is a stably named sub-object, we shouldn't need to create it
                        if (_subobj == null)
                            continue
                        repObj = _subobj
                    } else {
                        val (classGUID, classObj) = bunch.readObject()//SubOjbectClass,SubObjectClassNetGUID

                    // adding some stuff

                        if (classObj != null && (actor.type == DroopedItemGroup || actor.type == DroppedItem || actor.type == AirDrop)) {
                            val sn = Item.isGood(classObj.pathName)
                            if (sn != null)
                                droppedItemLocation[netguid] = tuple2(Vector2(actor.location.x, actor.location.y), sn)
                        }

                   //  ---------------------

                        if (!classGUID.isValid() || classObj == null)
                            continue
                        val subobj = NetGuidCacheObject(classObj.pathName, classGUID)
                        guidCache.registerNetGUID_Client(netguid, subobj)
                        repObj = guidCache.getObjectFromNetGUID(netguid)
                    }

                }
            }
            val NumPayloadBits = bunch.readIntPacked()
            if (NumPayloadBits < 0 || NumPayloadBits > bunch.bitsLeft()) {
                bugln { "NumPayloadBits=$NumPayloadBits > bunch.bitsLeft()=${bunch.bitsLeft()}" }
                return
            }
            if (NumPayloadBits == 0)
                continue
            try {
                val outPayload = bunch.deepCopy(NumPayloadBits)

                var parseComplete=!bHasRepLayout
                if (bHasRepLayout) {
                    if (!client)// Server shouldn't receive properties.
                        return
                    if (actor.type == DroopedItemGroup && repObj?.pathName == "RootComponent")
                        repObj = NetGuidCacheObject("DroppedItemGroupRootComponent", repObj.outerGUID)
                    //repl_layout_bunch(outPayload, repObj, actor)
                    parseComplete=receiveProperties(outPayload,repObj,actor)
                }
                if (!client) {
                    when {
                        actor.isVehicle ->
                            VehicleMoveComp(actor, outPayload)
                        actor.isACharacter && bIsActor -> {
                            selfID = actor.netGUID
                            CharacterMoveComp(outPayload)
                        }
                    }
                }

            } catch (e: Exception) {
            }
            bunch.skipBits(NumPayloadBits)
        }
        infoln { "" }
    }

    fun SerializeActor(bunch: Bunch) {
        val (netGUID, newActor) = bunch.readObject()//NetGUID
        if (netGUID.isDynamic()) {
            val (archetypeNetGUID, archetype) = bunch.readObject()
            if (archetypeNetGUID.isValid() && archetype == null) {
                val existingCacheObjectPtr = guidCache.objectLoop[archetypeNetGUID]
                if (existingCacheObjectPtr != null)
                    bugln { "Unresolved Archetype GUID. Path: ${existingCacheObjectPtr.pathName}, NetGUID: $archetypeNetGUID" }
                else
                    bugln { "Unresolved Archetype GUID. Guid not registered!, NetGUID: $archetypeNetGUID" }
            }
            val bSerializeLocation = bunch.readBit()

            val Location = if (bSerializeLocation)
                bunch.readVector()
            else
                Vector3.Zero
            val bSerializeRotation = bunch.readBit()
            val Rotation = if (bSerializeRotation) bunch.readRotationShort() else Vector3.Zero

            val bSerializeScale = bunch.readBit()
            val Scale = if (bSerializeScale) bunch.readVector() else Vector3.Zero

            val bSerializeVelocity = bunch.readBit()
            val Velocity = if (bSerializeVelocity) bunch.readVector() else Vector3.Zero

            if (actor == null && archetype != null) {
                val _actor=makeActor(netGUID,archetype)
                with(_actor) {
                    location = Location
                    rotation = Rotation
                    velocity = Velocity
                    guidCache.registerNetGUID_Client(netGUID, this)
                    actor = this
                    if (client) {
                        actors[netGUID] = this
                        when (type) {
                            Weapon -> weapons[netGUID] = this
                            AirDrop -> airDropLocation[netGUID]=location
                            DeathDropItemPackage -> corpseLocation[netGUID] = location
                            else -> {
                            }
                        }
                    }
                }
                bugln { "spawn $actor" }
            }
            bugln { ",[$netGUID] spawn:$Location,$Rotation,$Velocity, actor:$actor" }
        } else {
            if (newActor == null) return
            actor=makeActor(netGUID,newActor)
            actor!!.isStatic = true
        }

    }

    override fun close() {
        if (actor != null) {
            if (client) {
                actors.remove(actor!!.netGUID)
                visualActors.remove(actor!!.netGUID)
            }
            actor = null
        }
    }

}
