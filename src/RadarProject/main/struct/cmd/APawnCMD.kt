@file:Suppress("NAME_SHADOWING")
package main.struct.cmd

import main.bugln
import main.deserializer.ROLE_MAX
import main.deserializer.channel.ActorChannel
import main.deserializer.channel.ActorChannel.Companion.actors
import main.struct.Actor
import main.struct.Archetype.*
import main.struct.Bunch
import main.struct.NetGUIDCache
import main.struct.NetGuidCacheObject
import main.struct.*

object APawnCMD {
    fun process(actor: Actor, bunch: Bunch, repObj: NetGuidCacheObject?, waitingHandle: Int, data: HashMap<String, Any?>): Boolean {
        with(bunch) {
            when (waitingHandle) {
                1 -> if (readBit()) {//bHidden
                    ActorChannel.visualActors.remove(actor.netGUID)
                    bugln { ",bHidden id$actor" }
                }
                2 -> if (!readBit()) {// bReplicateMovement
                    ActorChannel.visualActors.remove(actor.netGUID)
                    bugln { ",!bReplicateMovement id$actor " }
                }
                3 -> if (readBit()) {//bTearOff
                    ActorChannel.visualActors.remove(actor.netGUID)
                    bugln { ",bTearOff id$actor" }
                }
                4 -> {
                    val role = readInt(ROLE_MAX)
                    val b = role
                }
                5 -> {
                    val (netGUID, obj) = readObject()
                    actor.owner = if (netGUID.isValid()) netGUID else null
                    bugln { " owner: [$netGUID] $obj ---------> beOwned:$actor" }
                }
                6 -> {
                    repMovement(actor)
                    with(actor) {
                        when (type) {
                            AirDrop -> ActorChannel.airDropLocation[netGUID] = location
                            Other -> {
                            }
                            else -> ActorChannel.visualActors[netGUID] = this
                        }
                    }
                }
                7 -> {
                    val bReplicatesAttachment=readBit()
                    val a=bReplicatesAttachment
                }
                8 -> {
                    val locationOffset = propertyVector100()
                    if (actor.type == DroopedItemGroup) {
                        bugln { "${actor.location} locationOffset $locationOffset" }
                    }
                    bugln { ",attachLocation $actor ----------> $locationOffset" }
                }
                else -> return false
            }
            return true
        }
    }
}