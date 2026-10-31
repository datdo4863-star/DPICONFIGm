package com.dpiconfig

class UserService : IUserService.Stub() {
    override fun destroy() { System.exit(0) }
    override fun exit() { System.exit(0) }
    override fun applyPointerSpeed(speed: Int): Int {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", "settings put system pointer_speed $speed"))
            proc.waitFor()
            proc.exitValue()
        } catch (e: Exception) { -1 }
    }
}
