#ifndef NMS_H
#define NMS_H

#include <vector>
#include "pose_types.h"

float calculateIoU(
        const PoseObject& a,
        const PoseObject& b);

std::vector<PoseObject> performNms(
        std::vector<PoseObject>& poses,
        float iouThreshold);

#endif