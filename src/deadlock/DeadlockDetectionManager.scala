package deadlock


import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * @author yaroslav.gryniuk
 */

object DeadlockDetectionManager {
  def apply[Thread, ReadWriteLock]() = new DeadlockDetectionManager[Thread, ReadWriteLock]
}

class DeadlockDetectionManager[Thread, ReadWriteLock] {

  object State extends Enumeration {
    type State = Value
    val ReqRead, ReqWrite, UseRead, UseWrite = Value
  }

  import State._

  val stateMatrix = scala.collection.mutable.HashMap.empty[(Thread, ReadWriteLock), State]
  val lock = new ReentrantReadWriteLock

  def concurrentWrapper[T](func: => T, read: Boolean = false) = {
    val currentLock = if (read) lock.readLock() else lock.writeLock()
    currentLock.lock()
    val res = func
    currentLock.unlock()
    res
  }


  def requestRead(thread: Thread, lock: ReadWriteLock) = concurrentWrapper {
    stateMatrix += ((thread, lock) -> State.ReqRead)

  }

  def requestWrite(thread: Thread, lock: ReadWriteLock) = concurrentWrapper {
    stateMatrix += ((thread, lock) -> State.ReqWrite)

  }


  def useRead(thread: Thread, lock: ReadWriteLock) = concurrentWrapper {
    stateMatrix += ((thread, lock) -> State.UseRead)

  }


  def useWrite(thread: Thread, lock: ReadWriteLock) = concurrentWrapper {
    stateMatrix += ((thread, lock) -> State.UseWrite)

  }

  def releaseRead(thread: Thread, lock: ReadWriteLock) = concurrentWrapper {
    stateMatrix -= ((thread, lock))

  }

  def releaseWrite(thread: Thread, lock: ReadWriteLock) = concurrentWrapper {
    stateMatrix -= ((thread, lock))

  }

  def detectDeadlock() = concurrentWrapper({
    val immStateMatrix = stateMatrix.toMap
    future {
      val waitForMatrix = buildWaitForMatrix(immStateMatrix)
      var anyCycle = false
      val threadIterator = waitForMatrix.keys.iterator
      while (threadIterator.hasNext && !anyCycle) {
        anyCycle = dfs(waitForMatrix, threadIterator.next(), Set())
      }
      anyCycle
    }
  }, true)


  private def dfs[T](graph: Map[T, List[T]], node: T, visitedNodes: Set[T]): Boolean = {
    if (visitedNodes contains node) {
      true
    } else {
      val newVisitedNodes = visitedNodes + node
      var anyCycle = false
      val childIterator = graph.getOrElse(node, List.empty[T]).iterator
      while (childIterator.hasNext && !anyCycle) {
        val child = childIterator.next()
        anyCycle = dfs(graph, child, newVisitedNodes)
      }
      anyCycle
    }
  }


  private def buildWaitForMatrix(matrix: Map[(Thread, ReadWriteLock), State]) = {
    val waitForMatrix = scala.collection.mutable.HashMap.empty[Thread, List[Thread]]
    getAllThreads(matrix) foreach {
      thread =>
        val threads = scala.collection.mutable.ArrayBuffer.empty[Thread]
        matrix filter {
          case ((`thread`, _), _) => true
          case _ => false
        } foreach {
          case ((_, l), ReqRead) =>
            threads += findThreadWhichHoldWLock(l, matrix)
          case ((_, l), ReqWrite) =>
            threads ++= findThreadWhichHoldRLock(l, matrix)
          case _ =>
        }
        waitForMatrix += (thread -> threads.toList)
    }
    waitForMatrix.toMap
  }

  private def findThreadWhichHoldWLock(l: ReadWriteLock, matrix: Map[(Thread, ReadWriteLock), State]) = {
    (matrix filter {
      case ((thread, `l`), UseWrite) => true
      case _ => false
    }).toList.head._1._1
  }

  private def findThreadWhichHoldRLock(l: ReadWriteLock, matrix: Map[(Thread, ReadWriteLock), State]): List[Thread] = {
    (matrix filter {
      case ((thread, `l`), UseRead) => true
      case _ => false

    }).map {
      case ((t, _), _) => t
    }.toList
  }

  private def getAllThreads(matrix: Map[(Thread, ReadWriteLock), State]): Set[Thread] = {
    val set = scala.collection.mutable.HashSet.empty[Thread]
    matrix foreach {
      case ((thread, _), _) =>
        set += thread
    }
    set.toSet
  }

}
