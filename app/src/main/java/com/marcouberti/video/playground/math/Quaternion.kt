package com.marcouberti.video.playground.math

import kotlin.math.*

data class Quaternion(
    val w: Float,
    val x: Float,
    val y: Float,
    val z: Float,
) {
    constructor(angleRadians: Float, axis: Vector3) : this(
        w = cos(angleRadians / 2),
        x = axis.x * sin(angleRadians / 2),
        y = axis.y * sin(angleRadians / 2),
        z = axis.z * sin(angleRadians / 2),
    )

    val lengthSquared
        get() = w * w + x * x + y * y + z * z
    val length
        get() = sqrt(lengthSquared)

    val angle: Float
        get() = 2f * acos(w)
    val axis: Vector3
        get() {
            return if (angle != 0f)
                (1f / sin(angle / 2f)) * Vector3(x, y, z)
            else
                Vector3(1f, 0f, 0f)
        }

    operator fun times(v: Quaternion) = Quaternion(
        w = w * v.w - x * v.x - y * v.y - z * v.z,
        x = w * v.x + x * v.w + y * v.z - z * v.y,
        y = w * v.y - x * v.z + y * v.w + z * v.x,
        z = w * v.z + x * v.y - y * v.x + z * v.w,
    )

    fun normalized(): Quaternion {
        val length = length
        return if (length == 0f)
            this
        else
            Quaternion(
                w = w / length,
                x = x / length,
                y = y / length,
                z = z / length,
            )
    }

    fun conjugate() = Quaternion(
        angleRadians = angle,
        axis = -axis,
    )
}

fun Float.degreesToRadians(): Float {
    return (this * PI / 180.0).toFloat()
}

fun Float.radiansToDegrees(): Float {
    return (this * 180.0 / PI).toFloat()
}
