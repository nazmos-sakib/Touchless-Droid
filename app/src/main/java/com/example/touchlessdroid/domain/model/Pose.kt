package com.example.touchlessdroid.domain.model

data class Pose(
    val nose: Keypoint,
    val leftEye: Keypoint,
    val rightEye: Keypoint,
    val leftEar: Keypoint,
    val rightEar: Keypoint,
    val leftShoulder: Keypoint,
    val rightShoulder: Keypoint,
    val leftElbow: Keypoint,
    val rightElbow: Keypoint,
    val leftWrist: Keypoint,
    val rightWrist: Keypoint,
    val leftHip: Keypoint,
    val rightHip: Keypoint,
    val leftKnee: Keypoint,
    val rightKnee: Keypoint,
    val leftAnkle: Keypoint,
    val rightAnkle: Keypoint,
){
    override fun toString(): String {
        return """
            Pose(
              nose=$nose,
              leftEye=$leftEye, rightEye=$rightEye,
              leftEar=$leftEar, rightEar=$rightEar,
              leftShoulder=$leftShoulder, rightShoulder=$rightShoulder,
              leftElbow=$leftElbow, rightElbow=$rightElbow,
              leftWrist=$leftWrist, rightWrist=$rightWrist,
              leftHip=$leftHip, rightHip=$rightHip,
              leftKnee=$leftKnee, rightKnee=$rightKnee,
              leftAnkle=$leftAnkle, rightAnkle=$rightAnkle
            )
        """.trimIndent()
    }
}

