/*
 * Dex2C_runtime.h - C-compatible runtime declarations for TCC-compiled method stubs.
 * This replaces the C++ Dex2C.h when compiling with TCC (C mode).
 */
#pragma once
#ifndef DEX2C_RUNTIME_H
#define DEX2C_RUNTIME_H

#include "jni.h"
#include <stdint.h>
#include <stdbool.h>
#include <stdarg.h>
#include <string.h>

/* ── Logging ─────────────────────────────────────────────────────────────── */
#ifdef ANDROID
# include <android/log.h>
# define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "Dex2C", __VA_ARGS__)
# define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,  "Dex2C", __VA_ARGS__)
#else
# include <stdio.h>
# define LOGD(...) fprintf(stderr, "[D] " __VA_ARGS__)
# define LOGE(...) fprintf(stderr, "[E] " __VA_ARGS__)
#endif

/* ── Forward declarations ─────────────────────────────────────────────────── */
extern bool d2c_resolve_class(JNIEnv *env, jclass *cached_class, const char *class_name);
extern bool d2c_resolve_method(JNIEnv *env, jclass *cached_class, jmethodID *cached_method,
                               bool is_static, const char *class_name,
                               const char *method_name, const char *signature);
extern bool d2c_resolve_field(JNIEnv *env, jclass *cached_class, jfieldID *cached_field,
                              bool is_static, const char *class_name,
                              const char *field_name, const char *signature);
extern bool d2c_is_instance_of(JNIEnv *env, jobject instance, const char *class_name);
extern bool d2c_check_cast(JNIEnv *env, jobject instance, jclass clz, const char *class_name);
extern void d2c_throw_exception(JNIEnv *env, const char *class_name, const char *message);
extern void d2c_filled_new_array(JNIEnv *env, jarray array, const char *type, jint count, ...);

extern int64_t d2c_double_to_long(double val);
extern int64_t d2c_float_to_long(float val);
extern int32_t d2c_double_to_int(double val);
extern int32_t d2c_float_to_int(float val);

/* ── Type helpers ─────────────────────────────────────────────────────────── */
#define d2c_shl_int(a,b)  ((int32_t)(a) << ((b) & 0x1f))
#define d2c_shr_int(a,b)  ((int32_t)(a) >> ((b) & 0x1f))
#define d2c_ushr_int(a,b) ((uint32_t)(a) >> ((b) & 0x1f))
#define d2c_shl_long(a,b) ((int64_t)(a) << ((b) & 0x3f))
#define d2c_shr_long(a,b) ((int64_t)(a) >> ((b) & 0x3f))
#define d2c_ushr_long(a,b) ((uint64_t)(a) >> ((b) & 0x3f))

#endif /* DEX2C_RUNTIME_H */
