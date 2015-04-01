package org.ua.deadlock

import java.util.concurrent.locks.ReadWriteLock

import deadlock.DeadlockDetectionManager
import main.scala.org.ua.deadlock.CustomReadWriteLock
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * @author yaroslav.gryniuk
 */
class DeadlockDetectionManagerTest extends FlatSpec {

  "Manager" should "detects deadlock properly" in {

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
    thread1.setDaemon(true)
    thread2.setDaemon(true)
    thread1.start()
    thread2.start()
    Thread.sleep(2000)
    assert(Await.result(manager.detectDeadlock(), Duration.Inf))

  }


}
