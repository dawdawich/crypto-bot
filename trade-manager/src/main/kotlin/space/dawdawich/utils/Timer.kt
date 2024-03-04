package space.dawdawich.utils

class Timer {
    private var activeFor = 0L

    fun setTimer(milliseconds: Long) {
        activeFor = System.currentTimeMillis() + milliseconds
    }

    fun isTimerActive(): Boolean {
        return System.currentTimeMillis() < activeFor
    }
}
