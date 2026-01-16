#include <jni.h>
#include <string>
#include <vector>

#include <android/log.h>

#include "llama.h"
#include "ggml-backend.h"

#define LOG_TAG "RunnableAI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_ai_runnable_local_backends_llama_LlamaNative_init(
        JNIEnv * env,
        jobject /*thiz*/,
        jstring nativeLibDir) {
    const char * path = env->GetStringUTFChars(nativeLibDir, nullptr);
    if (path && path[0] != '\0') {
        ggml_backend_load_all_from_path(path);
    } else {
        ggml_backend_load_all();
    }
    if (path) {
        env->ReleaseStringUTFChars(nativeLibDir, path);
    }
    llama_backend_init();
    LOGI("llama.cpp backend initialized");
}

extern "C" JNIEXPORT jlong JNICALL
Java_ai_runnable_local_backends_llama_LlamaNative_loadModel(
        JNIEnv * env,
        jobject /*thiz*/,
        jstring modelPath,
        jint nGpuLayers) {
    const char * path = env->GetStringUTFChars(modelPath, nullptr);
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = nGpuLayers;

    llama_model * model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);
    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }
    return reinterpret_cast<jlong>(model);
}

extern "C" JNIEXPORT void JNICALL
Java_ai_runnable_local_backends_llama_LlamaNative_freeModel(
        JNIEnv * /*env*/,
        jobject /*thiz*/,
        jlong handle) {
    auto * model = reinterpret_cast<llama_model *>(handle);
    if (model) {
        llama_model_free(model);
    }
}

static std::string token_to_piece(const llama_vocab * vocab, llama_token token) {
    char buf[256];
    int n = llama_token_to_piece(vocab, token, buf, sizeof(buf), 0, true);
    if (n < 0) {
        return std::string();
    }
    return std::string(buf, n);
}

extern "C" JNIEXPORT jstring JNICALL
Java_ai_runnable_local_backends_llama_LlamaNative_generate(
        JNIEnv * env,
        jobject /*thiz*/,
        jlong modelHandle,
        jstring prompt,
        jint nCtx,
        jint nPredict,
        jint nThreads,
        jfloat temperature) {
    auto * model = reinterpret_cast<llama_model *>(modelHandle);
    if (!model) {
        return env->NewStringUTF("Model not loaded");
    }

    const char * prompt_chars = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_str = prompt_chars ? prompt_chars : "";
    if (prompt_chars) {
        env->ReleaseStringUTFChars(prompt, prompt_chars);
    }

    const llama_vocab * vocab = llama_model_get_vocab(model);
    int n_prompt = -llama_tokenize(vocab, prompt_str.c_str(), prompt_str.size(), nullptr, 0, true, true);
    if (n_prompt <= 0) {
        return env->NewStringUTF("Tokenization failed");
    }
    std::vector<llama_token> prompt_tokens(n_prompt);
    if (llama_tokenize(vocab, prompt_str.c_str(), prompt_str.size(), prompt_tokens.data(), prompt_tokens.size(), true, true) < 0) {
        return env->NewStringUTF("Tokenization failed");
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = nCtx;
    ctx_params.n_batch = prompt_tokens.size();
    ctx_params.n_threads = nThreads;
    ctx_params.n_threads_batch = nThreads;

    llama_context * ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        return env->NewStringUTF("Context init failed");
    }

    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(0));

    std::string output = prompt_str;

    llama_batch batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());
    if (llama_decode(ctx, batch) != 0) {
        llama_sampler_free(sampler);
        llama_free(ctx);
        return env->NewStringUTF("Decode failed");
    }

    llama_token token;
    for (int i = 0; i < nPredict; ++i) {
        token = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) {
            break;
        }
        output += token_to_piece(vocab, token);
        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx, batch) != 0) {
            break;
        }
    }

    llama_sampler_free(sampler);
    llama_free(ctx);

    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_ai_runnable_local_backends_llama_LlamaNative_systemInfo(
        JNIEnv * env,
        jobject /*thiz*/) {
    const char * info = llama_print_system_info();
    return env->NewStringUTF(info);
}
