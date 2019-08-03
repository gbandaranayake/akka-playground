package playground.akka

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

fun main(args: Array<String>) {
    val actorSystem = ActorSystem.create("testSystem")
    val greetingActor = actorSystem.actorOf(Greeter.props, "greeting-actor")
    greetingActor.tell(Greet("John Doe", greetingActor), greetingActor)
    Thread.sleep(5000)
    actorSystem.terminate()
}

data class Greet(val whom: String, val replyTo: ActorRef)

data class Greeted(val whom: String, val from: ActorRef)

class Greeter : AbstractActor() {
    companion object {
        val props: Props = Props.create(Greeter::class.java) {
            Greeter()
        }
    }

    override fun createReceive(): Receive {
        return receiveBuilder().match(Greet::class.java, fun(greet: Greet) {
            println("Hello ${greet.whom}")
        }).matchAny(fun(any: Any) {
            println("Unknown message received, ignoring")
        }).build()
    }
}