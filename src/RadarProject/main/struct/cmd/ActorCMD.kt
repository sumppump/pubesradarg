@file:Suppress("NAME_SHADOWING")
package main.struct.cmd

import com.badlogic.gdx.math.Vector2
import main.GameListener
import main.bugln
import main.deserializer.ROLE_MAX
import main.deserializer.channel.ActorChannel.Companion.actors
import main.deserializer.channel.ActorChannel.Companion.airDropLocation
import main.deserializer.channel.ActorChannel.Companion.visualActors
import main.deserializer.shortRotationScale
import main.register
import main.struct.Actor
import main.struct.Archetype.*
import main.struct.Bunch
import main.struct.NetGUIDCache.Companion.guidCache
import main.struct.NetGuidCacheObject
import main.struct.NetworkGUID
import main.struct.*
import main.struct.*
import main.struct.cmd.PlayerStateCMD.selfID
import java.util.concurrent.ConcurrentHashMap

var selfDirection = 0f
val selfCoords = Vector2()
var selfAttachTo: Actor? = null


object ActorCMD : GameListener {
    init {
        register(this)
    }

    override fun onGameOver() {
        actorWithPlayerState.clear()
        playerStateToActor.clear()
        actorHealth.clear()
    }

    val actorWithPlayerState = ConcurrentHashMap<NetworkGUID, NetworkGUID>()
    val playerStateToActor = ConcurrentHashMap<NetworkGUID, NetworkGUID>()
    val actorHealth = ConcurrentHashMap<NetworkGUID, Float>()

    fun process(actor: Actor, bunch: Bunch, repObj: NetGuidCacheObject?, waitingHandle: Int, data: HashMap<String, Any?>): Boolean {
        actor as Character
        with(bunch) {
            when (waitingHandle) {
                1 -> if (readBit()) {//bHidden
                    visualActors.remove(actor.netGUID)
                    bugln { ",bHidden id$actor" }
                }
                2 -> if (!readBit()) {// bReplicateMovement
                    if (!actor.isVehicle) {
                        visualActors.remove(actor.netGUID)
                    }
                    bugln { ",!bReplicateMovement id$actor " }
                }
                3 -> if (readBit()) {//bTearOff
                    visualActors.remove(actor.netGUID)
                    bugln { ",bTearOff id$actor" }
                }
                4 -> {
                    val role = readInt(ROLE_MAX)
                    val b = role
                }
                5 -> {
                    val (netGUID, _) = readObject()
                    actor.owner = if (netGUID.isValid()) netGUID else null
                }
                6 -> {
                    repMovement(actor)
                    with(actor) {
                        when (type) {
                            AirDrop -> airDropLocation[netGUID] = location
                            Other -> {
                            }
                            else -> visualActors[netGUID] = this
                        }
                    }
                }
                7 -> {
                    val (a,_)=readObject()
                    val attachTo=if (a.isValid()) {
                        actors[a]?.attachChildren?.add(actor.netGUID)
                        a
                    } else null
                    if (actor.attachParent != null)
                        actors[actor.attachParent!!]?.attachChildren?.remove(actor.netGUID)
                    actor.attachParent=attachTo
                }
                8 -> {
                    val locationOffset = propertyVector100()
                    if (actor.type == DroopedItemGroup) {
                        bugln { "${actor.location} locationOffset $locationOffset" }
                    }
                    bugln { ",attachLocation $actor ----------> $locationOffset" }
                }
                9 -> propertyVector100()
                10 -> readRotationShort()
                11 -> {
                    val attachSocket = propertyName()
                }
                12 -> {
                    val (attachComponnent, attachName) = bunch.readObject()
                }
                13 -> {
                    readInt(ROLE_MAX)
                }
                14 -> propertyBool()
                15 -> propertyObject()
                16 -> {
                    val (playerStateGUID, playerState) = propertyObject()
                    if (playerStateGUID.isValid()) {
                        actorWithPlayerState[actor.netGUID] = playerStateGUID
                        playerStateToActor[playerStateGUID] = actor.netGUID
                    }
                }
                17 -> {//RemoteViewPitch 2
                    val result = readUInt16() * shortRotationScale//pitch
                }
                18 -> {
                    val result = propertyObject()
                }
            //ACharacter
                19 -> {
                    val result = propertyObject()
                }
                20 -> {
                    val result = propertyName()
                }
                21 -> {
                    val result = propertyVector100()
                }
                22 -> {
                    val Rotation = readRotationShort()
                }//propertyRotator()
                23 -> {
                    val result = propertyBool()
                }
                24 -> {
                    val result = propertyBool()
                }
                25 -> {
                    val result = propertyBool()
                }
                26 -> {
                    val result = propertyFloat()
                }
                27 -> {
                    val ReplicatedServerLastTransformUpdateTimeStamp = propertyFloat()
                }
                28 -> {
                    val result = propertyByte()
                }
                29 -> {
                    val result = propertyBool()
                }
                30 -> {
                    val result = propertyFloat()
                }
                31 -> {
                    val result = propertyInt()
                }
            //struct FRepRootMotionMontage RepRootMotion;
                32 -> {
                    val result = propertyBool()
                }
                33 -> {
                    val result = propertyObject()
                }
                34 -> {
                    val result = propertyFloat()
                }
                35 -> {
                    val result = propertyVector100()
                }
                36 -> {
                    val result = readRotationShort()
                }//propertyRotator()
                37 -> {
                    val result = propertyObject()
                }
                38 -> {
                    val result = propertyName()
                }
                39 -> {
                    val result = propertyBool()
                }
                40 -> {
                    val result = propertyBool()
                }
                41 -> {//player
                    val bHasAdditiveSources = readBit()
                    val bHasOverrideSources = readBit()
                    val lastPreAdditiveVelocity = propertyVector10()
                    val bIsAdditiveVelocityApplied = readBit()
                    val flags = readUInt8()
                }
                42 -> {
                    val result = propertyVector10()
                }
                43 -> {
                    val result = propertyVector10()
                }
            //AMutableCharacter
                44 -> {
                    val arrayNum = readUInt16()
                    var index = readIntPacked()
                    while (index != 0) {
                        val value = readUInt8()
                        index = readIntPacked()
                    }
                }
            //ATslCharacter
                45 -> {
                    val remote_CastAnim = readInt(8)
                }
                46 -> {
                    val result = propertyInt()
                }
                47 -> {
                    val result = propertyFloat()
                }
                48 -> {
                    val result = propertyObject()
                }
                49 -> {
                    val result = propertyObject()
                }
                50 -> {
                    val CharacterState = propertyByte()
                }
                51 -> {
                    val result = propertyBool()
                }
                52 -> {
                    val bIsAimingRemote = propertyBool()
                }
                53 -> {
                    val result = propertyBool()
                }
                54 -> {
                    val result = propertyBool()
                }
                55 -> {
                    val (id, team) = propertyObject()
                }
                56 -> {
                    val ActualDamage = propertyFloat()
                }
                57 -> {
                    val damageType = propertyObject()
                }
                58 -> {
                    val PlayerInstigator = propertyObject()
                }
                59 -> {
                    val DamageOrigin = propertyVectorQ()
                }
                60 -> {
                    val RelHitLocation = propertyVectorQ()
                }
                61 -> {
                    val result = propertyName()
                    val b = result
                }
                62 -> {
                    val DamageMaxRadius = propertyFloat()
                }
                63 -> {
                    val ShotDirPitch = propertyByte()
                }
                64 -> {
                    val ShotDirYaw = propertyByte()
                }
                65 -> {
                    val result = propertyBool()
                }
                66 -> {
                    val result = propertyBool()
                }
                67 -> {
                    val bKilled = propertyBool()
                }
                68 -> {
                    val EnsureReplicationByte = propertyByte()
                }
                69 -> {
                    val AttackerWeaponName = propertyName()
                }
                70 -> {
                    val AttackerLocation = propertyVector()
                }
                71 -> {
                    val TargetingType = readInt(3)
                    val a = TargetingType
                }
                72 -> {
                    val reviveCastingTime = propertyFloat()
                    val a = reviveCastingTime
                }
                73 -> {
                    val result = propertyBool()
                    val b = result
                }
                74 -> {
                    val result = propertyBool()
                    val b = result
                }
                75 -> {
                    val result = propertyBool()
                    val b = result
                }
                76 -> {
                    val result = propertyBool()
                    val b = result
                }
                77 -> {
                    val result = propertyBool()
                    val b = result
                }
                78 -> {
                    val result = propertyBool()
                    val b = result
                }
                79 -> {
                    val result = propertyBool()
                    val b = result
                }
                80 -> {
                    val result = propertyBool()
                    val b = result
                }
                81 -> {
                    val bIsGroggying=propertyBool()
                    val b=bIsGroggying
                }
                82 -> {
                    val bIsGroggying=propertyBool()
                    actor.isGroggying = bIsGroggying
                }
                83 -> {
                    val bIsReviving=propertyBool()
                    val b=bIsReviving
                }
                84 -> {
                    val result = propertyBool()
                    val b = result
                }
                85 -> {
                    val result = propertyBool()
                    val b = result
                }
                86 -> {
                    val result = propertyBool()
                    val b = result
                }
                87 -> {
                    val result = propertyBool()
                    val b = result
                }
                88 -> {
                    val result = propertyBool()
                    val b = result
                }
                89 -> {
                    val result = readRotationShort()//propertyRotator()
                    val b = result
                }
                90 -> {
                    val AimOffsets = propertyVectorNormal()
                    val b = AimOffsets
                }
                91 -> {
                    val result = readObject()
                    val b = result
                }
                92 -> {
                    val result = propertyBool()
                    val b = result
                }
                93 -> {
                    val result = propertyBool()
                    val b = result
                }
                94 -> {
                    val health = propertyFloat()
                    actorHealth[actor.netGUID] = health
                }
                95 -> {
                    val healthMax = propertyFloat()
                }
                96 -> {
                    val GroggyHealth=propertyFloat()
                    actor.groggyHealth=GroggyHealth
                }
                97 -> {
                    val GroggyHealthMax = propertyFloat()
                }
                98 -> {
                    val BoostGauge=propertyFloat()
                    actor.boostGauge=BoostGauge
                }
                99 -> {
                    val BoostGaugeMax = propertyFloat()
                }
                100 -> {
                    val ShoesSoundType = readInt(8)
                    val b = ShoesSoundType
                }
                101 -> {
                    val result = readObject()
                    val b = result
                }
                102 -> {
                    val result = propertyBool()
                    val b = result
                }
                103 -> {
                    val result = readInt(4)
                    val b = result
                }
                104 -> {
                    val result = propertyBool()
                    val b = result
                }
                105 -> {
                    val result = propertyBool()
                    val b = result
                }
                106 -> {
                    val result = propertyBool()
                    val b = result
                }
                else -> return ActorReplicator.process(actor, bunch, repObj, waitingHandle, data)
            }
            return true
        }
    }
}