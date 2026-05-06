#include <jni.h>
#include <unistd.h>
#include <string.h>

JNIEXPORT void JNICALL
Java_com_a4455jkjh_ndk_InstallNdk_symlink(JNIEnv *env, jclass clazz,
                                           jstring src, jstring dst) {
    const char *source = (*env)->GetStringUTFChars(env, src, 0);
    const char *target = (*env)->GetStringUTFChars(env, dst, 0);

    if (source && target) {
        symlink(source, target);
    }

    (*env)->ReleaseStringUTFChars(env, src, source);
    (*env)->ReleaseStringUTFChars(env, dst, target);
}

JNIEXPORT void JNICALL
Java_com_a4455jkjh_ndk_InstallNdk_link(JNIEnv *env, jclass clazz,
                                        jstring src, jstring dst) {
    const char *source = (*env)->GetStringUTFChars(env, src, 0);
    const char *target = (*env)->GetStringUTFChars(env, dst, 0);

    if (source && target) {
        link(source, target);
    }

    (*env)->ReleaseStringUTFChars(env, src, source);
    (*env)->ReleaseStringUTFChars(env, dst, target);
}
