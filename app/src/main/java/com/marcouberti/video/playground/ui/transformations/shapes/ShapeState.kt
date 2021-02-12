package com.marcouberti.video.playground.ui.transformations.shapes

import android.opengl.Matrix
import com.bendingspoons.opengltest1.ui.main.shapes.Shape
import com.marcouberti.video.playground.math.Quaternion
import com.marcouberti.video.playground.math.Vector3
import com.marcouberti.video.playground.math.degreesToRadians
import com.marcouberti.video.playground.math.radiansToDegrees

data class ShapeState(
    val shape: Shape,
    var position: Vector3 = Vector3.zero,
    var orientation: Quaternion = Quaternion(angleRadians = 0f, axis = Vector3.versorX),
) {
    fun rotate(angleAroundX: Float, angleAroundY: Float) {
        val rotation = Quaternion(
            angleRadians = angleAroundX.degreesToRadians(),
            axis = Vector3.versorX,
        ) * Quaternion(
            angleRadians = angleAroundY.degreesToRadians(),
            axis = Vector3.versorY,
        )
        orientation = rotation * orientation
    }

    fun translate(dx: Float, dy: Float) {
        position += Vector3(dx, dy, 0f)
    }

    fun draw(vpMatrix: FloatArray) {
        val modelMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        Matrix.translateM(modelMatrix, 0, position.x, position.y, position.z)
        val rotationMatrix = FloatArray(16)
        val angle = orientation.angle.radiansToDegrees()
        val axis = orientation.axis
        Matrix.setRotateM(rotationMatrix, 0, angle, axis.x, axis.y, axis.z)
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, rotationMatrix, 0)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vpMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        shape.draw(mvpMatrix)
    }
}
