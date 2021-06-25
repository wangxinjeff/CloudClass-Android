package io.agora.edu.launch;

import org.jetbrains.annotations.NotNull;

public interface AgoraEduCoursewarePreloadListener {
    void onStartDownload(@NotNull AgoraEduCourseware ware);

    void onProgress(@NotNull AgoraEduCourseware ware, double progress);

    void onComplete(@NotNull AgoraEduCourseware ware);

    void onFailed(@NotNull AgoraEduCourseware ware);
}