package io.github.takahirom.codepathtracer

/**
 * Thread-safe circular buffer for storing trace events
 */
class CircularBuffer<T>(private val capacity: Int) {
    private val buffer = arrayOfNulls<Any?>(capacity)
    private var head = 0
    private var size = 0
    
    @Synchronized
    fun add(item: T) {
        if (capacity == 0) return
        
        buffer[head] = item
        head = (head + 1) % capacity
        if (size < capacity) {
            size++
        }
    }
    
    @Synchronized
    fun getLast(count: Int): List<T> {
        if (capacity == 0 || size == 0 || count <= 0) return emptyList()
        
        val actualCount = minOf(count, size)
        val result = mutableListOf<T>()
        
        for (i in 0 until actualCount) {
            val index = (head - 1 - i + capacity) % capacity
            @Suppress("UNCHECKED_CAST")
            val item = buffer[index] as? T
            if (item != null) {
                result.add(0, item)
            }
        }
        
        return result
    }
    
    @Synchronized
    fun clear() {
        for (i in buffer.indices) {
            buffer[i] = null
        }
        head = 0
        size = 0
    }
}