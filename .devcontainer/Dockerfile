FROM mcr.microsoft.com/devcontainers/java:17-bookworm

# Install Android SDK dependencies
RUN apt-get update && apt-get install -y \
    unzip \
    zip \
    && rm -rf /var/lib/apt/lists/*

# Set up environment variables
ENV ANDROID_HOME=/usr/local/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Download and install Android Command Line Tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip \
    && unzip commandlinetools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm commandlinetools.zip

# Accept licenses and install basic components
RUN yes | sdkmanager --licenses \
    && sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

# Fix permissions for the vscode user
RUN chown -R vscode:vscode ${ANDROID_HOME}