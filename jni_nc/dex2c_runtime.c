/*
 * dex2c_runtime.c — C implementation of Dex2C runtime helpers.
 * Compiled by TCC alongside generated method stubs into libdex2c_t1.so
 */
#include "Dex2C_runtime.h"
#include <stdlib.h>
#include <pthread.h>

/* Simple hash map for class/method/field caching using linear probing */
#define CACHE_SIZE 4096

typedef struct {
    const char *key1;
    const char *key2;
    const char *key3;
    void *value;
} CacheEntry;

static CacheEntry class_cache[CACHE_SIZE];
static CacheEntry method_cache[CACHE_SIZE];
static CacheEntry field_cache[CACHE_SIZE];
static pthread_mutex_t cache_mutex = PTHREAD_MUTEX_INITIALIZER;

static unsigned int hash3(const char *a, const char *b, const char *c) {
    unsigned int h = 5381;
    if (a) while (*a) h = ((h << 5) + h) ^ (unsigned char)*a++;
    if (b) while (*b) h = ((h << 5) + h) ^ (unsigned char)*b++;
    if (c) while (*c) h = ((h << 5) + h) ^ (unsigned char)*c++;
    return h % CACHE_SIZE;
}

bool d2c_resolve_class(JNIEnv *env, jclass *cached_class, const char *class_name) {
    if (*cached_class) return false;
    pthread_mutex_lock(&cache_mutex);
    unsigned int idx = hash3(class_name, NULL, NULL);
    if (class_cache[idx].key1 && strcmp(class_cache[idx].key1, class_name) == 0) {
        *cached_class = (jclass)class_cache[idx].value;
        pthread_mutex_unlock(&cache_mutex);
        return false;
    }
    pthread_mutex_unlock(&cache_mutex);

    jclass clz = (*env)->FindClass(env, class_name);
    if (!clz) return true;
    jclass global = (jclass)(*env)->NewGlobalRef(env, clz);
    (*env)->DeleteLocalRef(env, clz);
    *cached_class = global;

    pthread_mutex_lock(&cache_mutex);
    class_cache[idx].key1 = class_name;
    class_cache[idx].value = global;
    pthread_mutex_unlock(&cache_mutex);
    return false;
}

bool d2c_resolve_method(JNIEnv *env, jclass *cached_class, jmethodID *cached_method,
                        bool is_static, const char *class_name,
                        const char *method_name, const char *signature) {
    if (*cached_method) return false;
    if (d2c_resolve_class(env, cached_class, class_name)) return true;

    pthread_mutex_lock(&cache_mutex);
    unsigned int idx = hash3(class_name, method_name, signature);
    if (method_cache[idx].key1 && strcmp(method_cache[idx].key1, class_name) == 0
        && strcmp(method_cache[idx].key2, method_name) == 0
        && strcmp(method_cache[idx].key3, signature) == 0) {
        *cached_method = (jmethodID)method_cache[idx].value;
        pthread_mutex_unlock(&cache_mutex);
        return false;
    }
    pthread_mutex_unlock(&cache_mutex);

    jmethodID mid = is_static
        ? (*env)->GetStaticMethodID(env, *cached_class, method_name, signature)
        : (*env)->GetMethodID(env, *cached_class, method_name, signature);
    if (!mid) return true;
    *cached_method = mid;

    pthread_mutex_lock(&cache_mutex);
    method_cache[idx].key1 = class_name;
    method_cache[idx].key2 = method_name;
    method_cache[idx].key3 = signature;
    method_cache[idx].value = mid;
    pthread_mutex_unlock(&cache_mutex);
    return false;
}

bool d2c_resolve_field(JNIEnv *env, jclass *cached_class, jfieldID *cached_field,
                       bool is_static, const char *class_name,
                       const char *field_name, const char *signature) {
    if (*cached_field) return false;
    if (d2c_resolve_class(env, cached_class, class_name)) return true;

    jfieldID fid = is_static
        ? (*env)->GetStaticFieldID(env, *cached_class, field_name, signature)
        : (*env)->GetFieldID(env, *cached_class, field_name, signature);
    if (!fid) return true;
    *cached_field = fid;
    return false;
}

bool d2c_is_instance_of(JNIEnv *env, jobject instance, const char *class_name) {
    if (!instance) return false;
    jclass clz = (*env)->FindClass(env, class_name);
    if (!clz) return false;
    bool result = (*env)->IsInstanceOf(env, instance, clz);
    (*env)->DeleteLocalRef(env, clz);
    return result;
}

bool d2c_check_cast(JNIEnv *env, jobject instance, jclass clz, const char *class_name) {
    if ((*env)->IsInstanceOf(env, instance, clz)) return false;
    d2c_throw_exception(env, "java/lang/ClassCastException", class_name);
    return true;
}

void d2c_throw_exception(JNIEnv *env, const char *class_name, const char *message) {
    jclass clz = (*env)->FindClass(env, class_name);
    if (clz) {
        (*env)->ThrowNew(env, clz, message);
        (*env)->DeleteLocalRef(env, clz);
    }
}

void d2c_filled_new_array(JNIEnv *env, jarray array, const char *type, jint count, ...) {
    va_list args;
    va_start(args, count);
    bool ref = (type[0] == '[' || type[0] == 'L');
    for (int i = 0; i < count; i++) {
        if (ref) {
            jobject val = (jobject)(intptr_t)va_arg(args, long);
            (*env)->SetObjectArrayElement(env, (jobjectArray)array, i, val);
        } else {
            jint val = va_arg(args, jint);
            (*env)->SetIntArrayRegion(env, (jintArray)array, i, 1, &val);
        }
    }
    va_end(args);
}

#define SAFE_CAST_FLOAT_LONG(val) \
    (((val) != (val)) ? 0LL : \
     ((val) >= (double)INT64_MAX) ? INT64_MAX : \
     ((val) <= (double)INT64_MIN) ? INT64_MIN : (int64_t)(val))

int64_t d2c_double_to_long(double val) { return SAFE_CAST_FLOAT_LONG(val); }
int64_t d2c_float_to_long(float val)   { return SAFE_CAST_FLOAT_LONG((double)val); }
int32_t d2c_double_to_int(double val) {
    if (val != val) return 0;
    if (val >= INT32_MAX) return INT32_MAX;
    if (val <= INT32_MIN) return INT32_MIN;
    return (int32_t)val;
}
int32_t d2c_float_to_int(float val) {
    if (val != val) return 0;
    if (val >= INT32_MAX) return INT32_MAX;
    if (val <= INT32_MIN) return INT32_MIN;
    return (int32_t)val;
}
