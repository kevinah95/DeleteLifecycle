package io.github.kevinah95.deletelifecycle

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform