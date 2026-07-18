
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("ncnn_yolo26_pose_analyzer")
add_library(
        ${CMAKE_PROJECT_NAME}
        SHARED
        ncnn_yolo26_pose_analyzer.cpp
        pose_decoder.cpp
        nms.cpp
)

find_library(log-lib log)
find_library(
        jnigraphics-lib
        jnigraphics
)
find_library(
        android-lib
        android
)

add_library(
        ncnn
        SHARED
        IMPORTED
)

set_target_properties(
        ncnn
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libncnn.so
)

target_include_directories(
        ${CMAKE_PROJECT_NAME}
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/ncnn
)

target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        ncnn
        ${log-lib}
        ${android-lib}
        ${jnigraphics-lib}
)