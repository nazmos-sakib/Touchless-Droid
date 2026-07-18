#ifndef POSE_DECODER_H
#define POSE_DECODER_H

#include <vector>

#include "net.h"
#include "pose_types.h"

std::vector<PoseObject> decodePoses(
        const ncnn::Mat& output,
        float confidenceThreshold
);

#endif