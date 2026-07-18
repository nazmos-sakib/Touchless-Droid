#include "nms.h"

#include <algorithm>

float calculateIoU(
        const PoseObject& a,
        const PoseObject& b)
{
    float interLeft =
            std::max(a.x1, b.x1);

    float interTop =
            std::max(a.y1, b.y1);

    float interRight =
            std::min(a.x2, b.x2);

    float interBottom =
            std::min(a.y2, b.y2);

    float interWidth =
            std::max(0.f, interRight - interLeft);

    float interHeight =
            std::max(0.f, interBottom - interTop);

    float intersection =
            interWidth * interHeight;

    float areaA =
            (a.x2 - a.x1) *
            (a.y2 - a.y1);

    float areaB =
            (b.x2 - b.x1) *
            (b.y2 - b.y1);

    float unionArea =
            areaA + areaB - intersection;

    return intersection / unionArea;
}

std::vector<PoseObject> performNms(
        std::vector<PoseObject>& poses,
        float iouThreshold)
{
    std::sort(
            poses.begin(),
            poses.end(),
            [](const PoseObject& a,
               const PoseObject& b)
            {
                return a.score > b.score;
            });

    std::vector<PoseObject> result;

    while (!poses.empty())
    {
        PoseObject best = poses.front();

        result.push_back(best);

        poses.erase(poses.begin());

        poses.erase(
                std::remove_if(
                        poses.begin(),
                        poses.end(),
                        [&](const PoseObject& pose)
                        {
                            return calculateIoU(
                                    best,
                                    pose)
                                   > iouThreshold;
                        }),
                poses.end());
    }

    return result;
}