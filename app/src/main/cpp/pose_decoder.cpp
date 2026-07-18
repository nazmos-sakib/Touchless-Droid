#include "pose_decoder.h"

std::vector<PoseObject> decodePoses(
        const ncnn::Mat& output,
        float confidenceThreshold)
{
    std::vector<PoseObject> poses;

    const int numPredictions = output.w; // 8400
    float maxScore = 0.f;
    for (int i = 0; i < numPredictions; i++)
    {


        float score = output.row(4)[i];
        if (score > maxScore)
        {
            maxScore = score;
        }


        if (score < confidenceThreshold)
        {
            continue;
        }


        PoseObject pose;

        float x = output.row(0)[i];
        float y = output.row(1)[i];
        float w = output.row(2)[i];
        float h = output.row(3)[i];

        pose.x1 = x - w * 0.5f;
        pose.y1 = y - h * 0.5f;

        pose.x2 = x + w * 0.5f;
        pose.y2 = y + h * 0.5f;

        pose.score = score;

        //----------------------------------
        // 17 keypoints
        //----------------------------------

        for (int kp = 0; kp < 17; kp++)
        {
            int offset = 5 + kp * 3;

            KeyPoint keypoint;

            keypoint.x = output.row(offset)[i];
            keypoint.y = output.row(offset + 1)[i];
            keypoint.score = output.row(offset + 2)[i];

            pose.keypoints.push_back(keypoint);
        }

        poses.push_back(pose);
    }

    __android_log_print(
            ANDROID_LOG_DEBUG,
            "POSE_DEBUG",
            "max score = %f",
            maxScore
    );

    return poses;
}