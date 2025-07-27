#!/bin/bash

# Code Path Finder - Debug Trace Helper Script
# ByteBuddyメソッドトレースのデバッグを補助するスクリプト

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🔍 Code Path Finder - Debug Trace Helper"
echo "======================================="

# テスト実行とログフィルタリング
echo ""
echo "📋 Running test with trace output..."
echo ""

# テスト実行してトレース関連のログのみを抽出
./gradlew sample-robolectric:testDebugUnitTest \
  --tests="RobolectricMethodTraceTest.testBusinessLogicWithTrace" \
  --rerun-tasks \
  --console=plain 2>&1 | \
  grep -E "(MethodTrace|ENTERING|EXITING|→|←)" | \
  grep -v "Transform called for:" | \
  grep -v "shouldTransformClass:" | \
  grep -v "checking pattern" | \
  grep -v "FINAL DECISION" | \
  grep -v "Transform REJECTED" | \
  grep -v "Transform approved" | \
  grep -v "Successfully transformed" | \
  grep -v "Transformer invoked"

echo ""
echo "✅ Debug trace completed!"
echo ""
echo "💡 Expected output should include:"
echo "   → ENTERING: SampleCalculator.add(arg0=10, arg1=5)"
echo "   ← EXITING: SampleCalculator.add -> 15"
echo "   → ENTERING: SampleCalculator.multiply(arg0=15, arg1=2)"
echo "   ← EXITING: SampleCalculator.multiply -> 30"
echo "   → ENTERING: SampleCalculator.complexCalculation(arg0=5, arg1=3)"
echo "     → ENTERING: SampleCalculator.add(arg0=5, arg1=3)"
echo "     ← EXITING: SampleCalculator.add -> 8"
echo "     → ENTERING: SampleCalculator.multiply(arg0=8, arg1=2)"
echo "     ← EXITING: SampleCalculator.multiply -> 16"
echo "     → ENTERING: SampleCalculator.add(arg0=16, arg1=12)"
echo "     ← EXITING: SampleCalculator.add -> 28"
echo "   ← EXITING: SampleCalculator.complexCalculation -> 28"