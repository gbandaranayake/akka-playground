package playground.akka

import akka.actor.*
import akka.event.Logging
import akka.routing.FromConfig
import scala.Option
import scala.PartialFunction
import java.time.Duration

fun main(args: Array<String>) {
    runRouterExample()
}

private fun runRemoteRouterActorSystem() {
    val actorSystem = ActorSystem.create("routerActorSystem")
    val log = Logging.getLogger(actorSystem, actorSystem)
    Thread.sleep(2000)
    log.info("Enter any key to exit")
    System.`in`.read()
    actorSystem.terminate()
}

fun runChildActorSpawning() {
    val actorSystem = ActorSystem.create("testSystem")
    val log = Logging.getLogger(actorSystem, actorSystem)
    val printerProps = Props.create(Printer::class.java, ::Printer)
    val printer = actorSystem.actorOf(printerProps, "printer-actor")
    val morningGreeterActor = actorSystem.actorOf(Greeter.props("Good morning ", printer), "morning-greeter-actor")
    val goodDayGreeterActor =
        actorSystem.actorOf(WelcomeDrinkGreeter.props("Good day to you ", printer), "day-greeter-actor")
    morningGreeterActor.tell(Greeter.WhoToGreet("John"), ActorRef.noSender())
    goodDayGreeterActor.tell(WelcomeDrinkGreeter.WhoToServe("John"), ActorRef.noSender())
    goodDayGreeterActor.tell(WelcomeDrinkGreeter.WhoToServe("John"), ActorRef.noSender())
    goodDayGreeterActor.tell(Greeter.WhoToGreet("John"), ActorRef.noSender())
    morningGreeterActor.tell(Greeter.Greet, ActorRef.noSender())
    goodDayGreeterActor.tell(Greeter.Greet, ActorRef.noSender())
    Thread.sleep(2000)
    log.info("Enter any key to exit")
    System.`in`.read()
    actorSystem.terminate()
}

fun runRouterExample() {
    val actorSystem = ActorSystem.create("actorSystem")
    val log = Logging.getLogger(actorSystem, actorSystem)
    val master = actorSystem.actorOf(FromConfig.getInstance().props(), "router-master")
    master.tell(Work("Workload 1"), master)
    master.tell(Work("Workload 2"), master)
    master.tell(Work("Workload 3"), master)
    master.tell(Work("Workload 4"), master)
    master.tell(Work("Workload 5"), master)
    Thread.sleep(2000)
    log.info("Enter any key to exit")
    System.`in`.read()
    actorSystem.terminate()
}

open class Greeter(private val message: String, private val printer: ActorRef) : AbstractActor() {
    companion object {
        fun props(message: String, printer: ActorRef): Props =
            Props.create(Greeter::class.java) { Greeter(message, printer) }
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

class WelcomeDrinkGreeter(private val message: String, private val printer: ActorRef) : Greeter(message, printer) {
    companion object {
        fun props(message: String, printer: ActorRef): Props =
            Props.create(WelcomeDrinkGreeter::class.java) { WelcomeDrinkGreeter(message, printer) }
    }

    override fun createReceive(): Receive {
        return receiveBuilder().match(WhoToServe::class.java) {
            context.actorSelection("/user/day-greeter-actor/waiter").resolveOne(Duration.ofMillis(500))
                .thenAccept { ref -> ref.tell(Waiter.Serve(it.guest), self) }
                .handle { t, u ->
                    handleChildException(u, it, t)
                }
        }.match(Terminated::class.java) {
            val props = Props.create(Waiter::class.java) { Waiter() }
            context.actorOf(props, "waiter")
        }.build().orElse(super.createReceive())
    }

    private fun handleChildException(
        u: Throwable,
        it: WhoToServe,
        t: Void?
    ) {
        val cause = u.cause
        if (cause is ActorNotFound) {
            val props = Props.create(Waiter::class.java) { Waiter() }
            val waiterRef = context.actorOf(props, "waiter")
            waiterRef.tell(Waiter.Serve(it.guest), self)
        } else {
            println("Error occurred while finding the waiter actor ref $u")
        }
    }

    object DefaultDecider : PartialFunction<Throwable, SupervisorStrategy.Directive> {
        override fun isDefinedAt(x: Throwable?): Boolean {
            return true
        }

        override fun apply(v1: Throwable?): SupervisorStrategy.Directive {
            return when (v1) {
                is Waiter.FellDownException -> SupervisorStrategy.restart()
                else -> SupervisorStrategy.escalate()
            }
        }
    }

    override fun supervisorStrategy(): SupervisorStrategy {
        return OneForOneStrategy(DefaultDecider)
            .withMaxNrOfRetries(10)
    }

    data class WhoToServe(val guest: String)
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

class Waiter : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    private val servedGuests = HashSet<String>(20)

    override fun createReceive(): Receive {
        return receiveBuilder().match(Serve::class.java) {
            log.info("Here, take this glass of ice cold drink ${it.receiver} \\_/")
            servedGuests.add(it.receiver)
            self.tell(CollectGlass(it.receiver), ActorRef.noSender())
            throw FellDownException()
        }.match(CollectGlass::class.java) {
            Thread.sleep(10000)
            log.info("Hope you enjoyed your drink ${it.glassHolder} <= \\_/")
            servedGuests.remove(it.glassHolder)
        }.build()
    }

    override fun preRestart(reason: Throwable?, message: Option<Any>?) {
        log.info("Aww, seems gotta start fresh. Change of clothes in order")
    }

    data class Serve(val receiver: String)
    data class CollectGlass(val glassHolder: String)
    class FellDownException : Exception()
}