FROM mcr.microsoft.com/devcontainers/java:17-bookworm

# 1. Ensure we are root and set non-interactive mode to prevent apt errors
USER root
ENV DEBIAN_FRONTEND=noninteractive

# 2. Fix for Exit Code 100: Clean stale lists before updating
RUN rm -rf /var/lib/apt/lists/* \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
    unzip \
    zip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Set up environment variables
ENV ANDROID_HOME=/usr/local/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# 3. Download Android Command Line Tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip \
    && unzip commandlinetools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm commandlinetools.zip

# 4. Accept licenses and install API 36
RUN yes | sdkmanager --licenses \
    && sdkmanager --update \
    && sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

# 5. Fix permissions for the vscode user (Crucial for Dev Containers)
RUN chown -R vscode:vscode ${ANDROID_HOME}

# Switch back to vscode user so the container starts as the correct user
USER vscode