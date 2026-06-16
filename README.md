# Touchless-Droid

CameraX
↓
CameraViewModel (detects gesture)
↓
StateFlow<Gesture>
↓
UseCase (filters + deduplicates)
↓
BluetoothManager.send()
↓
ESP32