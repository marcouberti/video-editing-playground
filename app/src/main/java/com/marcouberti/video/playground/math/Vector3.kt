package com.marcouberti.video.playground.math

import kotlin.math.sqrt

data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    val lengthSquared
        get() = x * x + y * y + z * z
    val length
        get() = sqrt(lengthSquared)

    operator fun times(value: Float) = Vector3(
        x = x * value,
        y = y * value,
        z = z * value,
    )

    operator fun plus(value: Vector3) = Vector3(
        x = x + value.x,
        y = y + value.y,
        z = z + value.z,
    )

    operator fun minus(value: Vector3) = Vector3(
        x = x - value.x,
        y = y - value.y,
        z = z - value.z,
    )

    operator fun unaryMinus() = Vector3(-x, -y, -z)

    fun normalized(): Vector3 {
        val length = length
        return if (length == 0f)
            this
        else
            Vector3(
                x = x / length,
                y = y / length,
                z = z / length,
            )
    }

    fun rotatedBy(value: Quaternion, normalize: Boolean = true): Vector3 {
        val value = if (normalize) value.normalized() else value
        val result = value * Quaternion(
            w = 0f,
            x = x,
            y = y,
            z = z,
        ) * value.conjugate()

        return Vector3(
            x = result.x,
            y = result.y,
            z = result.z,
        )
    }

    fun rotatedByRadians(angle: Float, axis: Vector3): Vector3 {
        return rotatedBy(Quaternion(angle, axis.normalized()), normalize = false)
    }

    fun rotatedByDegrees(angle: Float, axis: Vector3): Vector3 {
        return rotatedByRadians(angle = angle.degreesToRadians(), axis = axis)
    }

    companion object {
        val zero = Vector3(0f, 0f, 0f)
        val versorX = Vector3(1f, 0f, 0f)
        val versorY = Vector3(0f, 1f, 0f)
        val versorZ = Vector3(0f, 0f, 1f)
    }
}

operator fun Float.times(value: Vector3) = value * this
