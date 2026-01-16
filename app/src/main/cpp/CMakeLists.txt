cmake_minimum_required(VERSION 3.22.1)
project(runnableai LANGUAGES C CXX)

include(FetchContent)

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(LLAMA_HTTPLIB OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    llama
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG cff777f22614e3129203ddc93e78b5576c936b0c
)

FetchContent_MakeAvailable(llama)

add_library(runnableai SHARED
    native-lib.cpp
)

target_include_directories(runnableai PRIVATE ${llama_SOURCE_DIR}/include)

target_link_libraries(runnableai
    llama
    android
    log
)
