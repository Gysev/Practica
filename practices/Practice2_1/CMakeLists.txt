cmake_minimum_required(VERSION 3.20)
project(practice_tray_app LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# GUI-приложение Windows (точка входа WinMain)
add_executable(${PROJECT_NAME} WIN32
    src/main.cpp
)

target_compile_definitions(${PROJECT_NAME} PRIVATE UNICODE _UNICODE)

if(MSVC)
  # Статический CRT — один .exe без vcruntime140.dll для типичной установки MSVC
  set_property(TARGET ${PROJECT_NAME} PROPERTY
    MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
endif()

target_link_libraries(${PROJECT_NAME} PRIVATE
    Shell32 comctl32 comdlg32 ole32 uuid
)

if(MSVC AND WIN32 AND NOT CMAKE_CONFIGURATION_TYPES AND NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
endif()
