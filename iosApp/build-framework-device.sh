#!/bin/bash

# Build script for iOS framework for real device
# This script builds the Kotlin framework for iOS device (arm64)

set -e  # Exit on error

cd "$(dirname "$0")/.."

echo "🔨 Building iOS framework for device..."
./gradlew :composeApp:linkDebugFrameworkIosArm64

FRAMEWORK_PATH="composeApp/build/bin/iosArm64/debugFramework/ComposeApp.framework"

if [ -d "$FRAMEWORK_PATH" ]; then
    echo "✅ Framework built successfully at:"
    echo "   $FRAMEWORK_PATH"
    echo ""
    echo "⚠️  Note: To use this framework, you need to update the Xcode project:"
    echo "   1. Open iosApp/iosApp.xcodeproj in Xcode"
    echo "   2. Go to Build Settings → Framework Search Paths"
    echo "   3. Change the path to: \$(PROJECT_DIR)/../composeApp/build/bin/iosArm64/debugFramework"
    echo "   4. Select your device and press Cmd+R to build and run"
else
    echo "❌ Framework build failed!"
    exit 1
fi
