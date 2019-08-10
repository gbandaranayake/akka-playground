package playground.akka

import akka.actor.AbstractActor
import akka.event.Logging
import java.io.Serializable
import akka.actor.Props
import java.util.ArrayList
import akka.actor.*
import java.time.Clock.system
import akka.actor.ActorRef
import akka.routing.*


class Worker : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    override fun createReceive(): Receive {
        return receiveBuilder().match(Work::class.java) {
            log.info("Doing the ${it.workLoad}")
        }.build()
    }
}

data class Work(val workLoad: String) : Serializable

class Master : AbstractActor() {
    private lateinit var router: ActorRef

    init {
        /*val routees = ArrayList<Routee>()
        for (i in 0..4) {
            val r = context.actorOf(Props.create(Worker::class.java, { Worker() }), "worker-actor-$i")
            context.watch(r)
            routees.add(ActorRefRoutee(r))
        }*/
//        router = context.actorOf(FromConfig.getInstance().props(Props.create(Worker::class.java, { Worker() })), "router1")
        router = context.actorOf(FromConfig.getInstance().props(Props.create(Worker::class.java, { Worker() })), "poolWithDispatcher")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(
                Work::class.java
            ) { message -> router.tell(message, sender) }
            .match(
                Terminated::class.java
            ) { message ->
                println("Worker actor terminated $message")
            }
            .build()
    }
}