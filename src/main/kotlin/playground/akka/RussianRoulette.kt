package playground.akka

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import java.lang.IllegalStateException
import kotlin.random.Random

class Player(val name: String) : AbstractActor() {
    override fun createReceive(): Receive {
        return receiveBuilder().match(SitAtTableMessage::class.java) {
            it.tableRef.tell(Table.AddSeatMessage(self()), self())
        }.match(PlayMessage::class.java) {
            //todo implement play
        }.match(GetHitMessage::class.java) {
            println("Oh no, $name is hit in the head and dead")
            throw IllegalStateException("Player $name died")
        }.match(TableIsFullMessage::class.java) {
            println("Player $name met a full table, leaving the site")
        }.build()
    }

    data class SitAtTableMessage(val tableRef: ActorRef)
    data class PlayMessage(val gunRef: ActorRef)
    data class GetHitMessage(val henchManRef: ActorRef)
    data class TableIsFullMessage(val tableRef: ActorRef)
}


class Table(noOfSeats: Int) : AbstractActor() {
    private val seats = ArrayList<Seat>(noOfSeats)

    override fun createReceive(): Receive {
        return receiveBuilder().match(AddSeatMessage::class.java) {
            when {
                seats.contains<Seat?>(null) -> it.occupier.tell(Player.TableIsFullMessage(self()), self())
                else -> seats[seats.indexOfFirst { false }] = Seat(it.occupier)
            }
        }.match(RemoveSeatMessage::class.java) {
            seats.remove(Seat(it.occupier))
        }.match(ClearTable::class.java) {
            seats.clear()
        }.build()
    }

    data class AddSeatMessage(val occupier: ActorRef)
    data class RemoveSeatMessage(val occupier: ActorRef)
    class ClearTable
    data class Seat(val occupier: ActorRef)
}

class HenchMan : AbstractActor() {
    override fun createReceive(): Receive {
        return receiveBuilder().match(SetUpTable::class.java) {
            it.tableRef.tell(Table.ClearTable(), self())
        }.match(RemoveDeadPlayer::class.java) {
            it.tableRef.tell(Table.RemoveSeatMessage(it.playerRef), self())
        }.match(RefillBullet::class.java) {
            //todo
        }.build()
    }

    data class SetUpTable(val tableRef: ActorRef)
    data class RemoveDeadPlayer(val playerRef: ActorRef, val tableRef: ActorRef)
    data class RefillBullet(val gunRef: ActorRef)
}

class Gun : AbstractActor() {
    val cartridges = ArrayList<Cartridge>(6)
    var currentCartridge = 0

    override fun createReceive(): Receive {
        return receiveBuilder().match(Fire::class.java) {
            cartridges.get(currentCartridge).bullet
        }.match(SpinChamber::class.java) {
            currentCartridge = Random.nextInt(6)
        }.match(LoadBullet::class.java) {
            cartridges.get(currentCartridge).bullet = it.bullet
        }.build()
    }

    class Fire
    class SpinChamber
    class LoadBullet(val bullet: Bullet)
    data class Cartridge(var bullet: Bullet)
    class Bullet
}