//
// Created by Nazmos Sakib mini on 21.06.26.
//

#ifndef POSE_TYPES_H
#define POSE_TYPES_H

#include <vector>

struct KeyPoint
{
    float x;
    float y;
    float score;
};

struct PoseObject
{
    float x1;
    float y1;
    float x2;
    float y2;

    float score;

    std::vector<KeyPoint> keypoints;
};

#endif