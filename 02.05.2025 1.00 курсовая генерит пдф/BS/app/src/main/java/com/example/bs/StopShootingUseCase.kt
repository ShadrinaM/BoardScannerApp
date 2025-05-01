package com.example.bs

class StopShootingUseCase(
    private val startShootingUseCase: StartShootingUseCase
) {

    fun execute() {
        startShootingUseCase.stop() // Вызываем метод stop() из StartShootingUseCase
    }
}
