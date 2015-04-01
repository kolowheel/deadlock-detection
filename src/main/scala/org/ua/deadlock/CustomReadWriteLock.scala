package main.scala.org.ua.deadlock

import java.util.concurrent.locks.{Condition, ReentrantReadWriteLock, Lock, ReadWriteLock}
import java.util.concurrent.TimeUnit
import deadlock.DeadlockDetectionManager

/**
 * @author yaroslav.gryniuk
 */
class CustomReadWriteLock(manager: DeadlockDetectionManager[Thread, ReadWriteLock]) extends ReadWriteLock {
  val delegate = new ReentrantReadWriteLock()
  val read = new CustomReadLock(delegate.readLock())
  val write = new CustomWriteLock(delegate.writeLock())

  def readLock(): Lock = read

  def writeLock(): Lock = write


  class CustomReadLock(l: Lock) extends Lock {
    def unlock() = {
      l.unlock()
      manager.releaseRead(Thread.currentThread(), delegate)
    }

    def lockInterruptibly() = l.lockInterruptibly()


    def lock() = {
      manager.requestRead(Thread.currentThread(), delegate)
      l.lock()
      manager.useRead(Thread.currentThread(), delegate)
    }

    def tryLock(): Boolean = l.tryLock()


    def tryLock(time: Long, unit: TimeUnit): Boolean = l.tryLock(time, unit)

    def newCondition(): Condition = l.newCondition()

  }

  class CustomWriteLock(l: Lock) extends Lock {
    def unlock() = {
      l.unlock()
      manager.releaseWrite(Thread.currentThread(), delegate)
    }

    def lockInterruptibly() = l.lockInterruptibly()


    def lock() = {
      manager.requestWrite(Thread.currentThread(), delegate)
      l.lock()
      manager.useWrite(Thread.currentThread(), delegate)
    }

    def tryLock(): Boolean = l.tryLock()


    def tryLock(time: Long, unit: TimeUnit): Boolean = l.tryLock(time, unit)

    def newCondition(): Condition = l.newCondition()

  }

}
