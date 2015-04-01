This project is a base for deadlock detection.
It is based on building a wait-for graph and finding cycles in it.

Currently, deadlock detection algorithm can work with read-write locks and it is located in org.ua.deadlock.DeadlockDetectionManager .

Possible usage with decorated ReadWriteLock can be found in org.ua.deadlock.DeadlockDetectionManagerTest.
