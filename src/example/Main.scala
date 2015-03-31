package example

/**
 * @author yaroslav.gryniuk
 */


import example.CustomReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import deadlock.DeadlockDetectionManager

object Main extends App {


  val manager = DeadlockDetectionManager[Thread, ReadWriteLock]()


  val lock1 = new CustomReadWriteLock(manager)
  val lock2 = new CustomReadWriteLock(manager)
  val thread1 = new Thread(new Runnable {
    def run() = {
      lock1.writeLock().lock()
      Thread.sleep(1000)
      lock2.readLock().lock()
    }
  })
  val thread2 = new Thread(new Runnable {
    def run() = {
      lock2.writeLock().lock()
      Thread.sleep(1000)
      lock1.readLock().lock()
    }
  })
  thread1.start()
  thread2.start()
  Thread.sleep(2000)
  println(Await.result(manager.detectDeadlock(), Duration.Inf))

}
