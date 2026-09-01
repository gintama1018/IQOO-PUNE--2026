# Model Assets

This directory should contain the ML model files required by SkillLens.

## Required Files

### 1. `electrical_components_detector.task`
- **Purpose**: MediaPipe Object Detector task bundle for detecting electrical components
  (terminals, wires, screwdriver, board, etc.)
- **How to create**: Train a custom object detection model using MediaPipe Model Maker,
  then export as a `.task` bundle.
- **If unavailable**: The app falls back to a heuristic color-based detection mode
  (low accuracy, suitable only for basic demo continuity).

### 2. `hand_landmarker.task`
- **Purpose**: MediaPipe Hand Landmarker for detecting hand positions during task execution.
- **How to obtain**: Download from the official MediaPipe solutions:
  https://developers.google.com/mediapipe/solutions/vision/hand_landmarker
- **Recommended model**: `hand_landmarker.task` (full precision)

## Notes

- Models are loaded lazily when entering the practice screen.
- Models are released when leaving the practice screen.
- All inference runs on-device — no network required during practice.
- Total model size should stay under 50MB for reasonable APK size.
