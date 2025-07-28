#!/bin/bash

# Code Path Finder - Debug Trace Helper (Minimal Version)
set -e

echo "🔍 Code Path Finder - Method Trace Verification"
echo "=============================================="

# Run test and check HTML report
echo "Running Activity test..."
./gradlew sample-robolectric:testDebugUnitTest \
  --tests="RobolectricMethodTraceTest.testActivityCreationWithTrace" \
  --rerun-tasks --console=plain > /dev/null 2>&1

html_report="sample-robolectric/build/reports/tests/testDebugUnitTest/classes/io.github.takahirom.codepathtracer.sample.RobolectricMethodTraceTest.html"

if [ ! -f "$html_report" ]; then
    echo "❌ ERROR: HTML report not found" >&2
    exit 1
fi

echo ""
echo "🎯 Method Trace Verification Results:"

# Check for specific target methods
main_activity=$(grep -o "MainActivity\.onCreate[^<]*" "$html_report" 2>/dev/null | head -1)
snapshot_thread=$(grep -o "SnapshotThreadLocal\.get[^<]*" "$html_report" 2>/dev/null | head -1)
phone_window=$(grep -o "PhoneWindow\.getPanelState[^<]*" "$html_report" 2>/dev/null | head -1)

exit_code=0

if [ -n "$main_activity" ]; then
    echo "✅ Project:          MainActivity.onCreate"
else
    echo "❌ Project:          MainActivity.onCreate - NOT FOUND" >&2
    exit_code=1
fi

if [ -n "$snapshot_thread" ]; then
    echo "✅ Library:          SnapshotThreadLocal.get"
else
    echo "❌ Library:          SnapshotThreadLocal.get - NOT FOUND" >&2
    exit_code=1
fi

if [ -n "$phone_window" ]; then
    echo "✅ Android Framework: PhoneWindow.getPanelState"
else
    echo "❌ Android Framework: PhoneWindow.getPanelState - NOT FOUND" >&2
    exit_code=1
fi

echo ""
if [ $exit_code -eq 0 ]; then
    echo "🎉 All method traces verified successfully!"
else
    echo "💡 Missing methods may be filtered or not called." >&2
fi

exit $exit_code