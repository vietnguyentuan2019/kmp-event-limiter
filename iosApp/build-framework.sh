#!/bin/bash

# Build script for iOS framework
# This script builds the Kotlin framework for iOS simulator

set -e  # Exit on error

cd "$(dirname "$0")/.."

echo "🔨 Building iOS framework for simulator..."
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

FRAMEWORK_PATH="composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework"

if [ -d "$FRAMEWORK_PATH" ]; then
    echo "✅ Framework built successfully at:"
    echo "   $FRAMEWORK_PATH"
    echo ""
    echo "📱 Next steps:"
    echo "   1. Open iosApp/iosApp.xcodeproj in Xcode"
    echo "   2. Select a simulator device (e.g., iPhone 15 Pro)"
    echo "   3. Press Cmd+R to build and run"
else
    echo "❌ Framework build failed!"
    exit 1
fi
