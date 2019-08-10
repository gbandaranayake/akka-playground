package playground.akka

import akka.actor.*
import akka.event.Logging

fun main(args: Array<String>) {
    val actorSystem = ActorSystem.create("testSystem")
    val printerProps = Props.create(Printer::class.java, ::Printer)
    val printer = actorSystem.actorOf(printerProps, "printer-actor")
    val morningGreeterActor = actorSystem.actorOf(Greeter.props("Good morning ", printer), "morning-greeter-actor")
    val goodDayGreeterActor = actorSystem.actorOf(Greeter.props("Good day to you ", printer), "day-greeter-actor")
    morningGreeterActor.tell(Greeter.WhoToGreet("John"), actorSystem.actorSelection("/user").anchor())
    goodDayGreeterActor.tell(Greeter.WhoToGreet("John"), actorSystem.actorSelection("/user").anchor())
    morningGreeterActor.tell(Greeter.Greet, actorSystem.actorSelection("/user").anchor())
    goodDayGreeterActor.tell(Greeter.Greet, actorSystem.actorSelection("/user").anchor())
    Thread.sleep(5000)
    actorSystem.terminate()
}

class Greeter(private val message: String, private val printer: ActorRef) : AbstractActor() {
    companion object {
        fun props(message: String, printer: ActorRef): Props = Props.create(Greeter::class.java) {
            Greeter(message, printer)
        }
    }

    var greeting = ""
    override fun createReceive(): Receive {
        return receiveBuilder().match(WhoToGreet::class.java) {
            greeting = "Hello! $message ${it.who}"
        }.match(Greet::class.java) {
            printer.tell(Printer.Greeting(greeting), self)
        }.matchAny {
            println("Unknown message received, ignoring")
        }.build()
    }

    data class WhoToGreet(val who: String)
    object Greet
}

class Printer : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    override fun createReceive(): Receive {
        return receiveBuilder().match(Greeting::class.java) {
            log.info("Greeting received (from " + sender() + "): " + it.greeting)
        }.build()
    }

    data class Greeting(val greeting: String)
}