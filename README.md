# BatteryMonitor

BatteryMonitor is a lightweight Android application designed to provide real-time insights into your device's battery performance. It displays various metrics such as current flow, voltage, temperature, and more, helping you understand your battery's health and charging behavior.

## Features

- **Real-time Monitoring**: Updates battery statistics every second.
- **Comprehensive Metrics**: Displays Status, Level, Power Source, Technology, Temperature, Voltage, Current, and Capacity.
- **Display Modes**: Toggle between viewing Charging Current (mA) and Charging Power (W).
- **Data Source Selection**: Choose between Instantaneous or Average current readings.
- **Theme Support**: Light, Dark, and System Default themes.
- **Debug Mode**: View raw sensor data and estimation methods.

## Installation

You can build the APK from source using Android Studio or Gradle.

### Prerequisites

- JDK 17
- Android SDK (API 34)

### Building from Source

1. Clone the repository:
   ```bash
   git clone <repository-url>
   ```
2. Navigate to the project directory:
   ```bash
   cd BatteryMonitor
   ```
3. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be located in `app/build/outputs/apk/debug/`.

## License

This project is licensed under the Unlicense - see the [LICENSE](LICENSE) file for details.
