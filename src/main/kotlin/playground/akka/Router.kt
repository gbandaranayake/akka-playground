package playground.akka

import akka.actor.AbstractActor
import akka.event.Logging
import java.io.Serializable
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router
import akka.routing.ActorRefRoutee
import akka.actor.Props
import akka.routing.Routee
import java.util.ArrayList
import akka.actor.*


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
    private lateinit var router: Router

    init {
        val routees = ArrayList<Routee>()
        for (i in 0..4) {
            val r = context.actorOf(Props.create(Worker::class.java, { Worker() }), "worker-actor-$i")
            context.watch(r)
            routees.add(ActorRefRoutee(r))
        }
        router = Router(RoundRobinRoutingLogic(), routees)
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(
                Work::class.java
            ) { message -> router.route(message, sender) }
            .match(
                Terminated::class.java
            ) { message ->
                router = router.removeRoutee(message.actor())
                val r = context.actorOf(Props.create(Worker::class.java, { Worker() }))
                context.watch(r)
                router = router.addRoutee(ActorRefRoutee(r))
            }
            .build()
    }
}