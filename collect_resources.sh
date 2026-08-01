#!/usr/bin/env bash

set -u

# ============================================================
# TouchlessDroid CPU and memory measurement
#
# Usage:
#   ./collect_resources.sh <runtime> <delegate> <precision>
#
# Examples:
#   ./collect_resources.sh TFLite cpu f32
#   ./collect_resources.sh TFLite gpu f32
#   ./collect_resources.sh ONNX nnapi int8
#   ./collect_resources.sh NCNN vulkan f32
#
# Output:
#   Nothing-TFLite-cpu-f32-20260730_090500.csv
# ============================================================

PACKAGE="com.example.touchlessdroid"

DURATION_SECONDS=60
SAMPLE_INTERVAL_SECONDS=1

OUTPUT_DIR="/Users/naz/Documents/Master-Arbeit/benchmark/adb_cpu_memory"

# Number of decimal places used for memory values.
MEMORY_DECIMAL_PLACES=3


# ============================================================
# 1. Validate arguments
# ============================================================

if [[ $# -ne 3 ]]; then
    echo "Error: exactly three arguments are required."
    echo
    echo "Usage:"
    echo "  $0 <runtime> <delegate> <precision>"
    echo
    echo "Example:"
    echo "  $0 TFLite cpu f32"
    exit 1
fi

RUNTIME="$1"
DELEGATE="$2"
PRECISION="$3"


# ============================================================
# 2. Helper functions
# ============================================================

sanitize_filename_value() {
    printf "%s" "$1" |
        tr '[:space:]' '_' |
        tr -cd '[:alnum:]_.-'
}


trim_value() {
    printf "%s" "$1" |
        tr -d '\r' |
        sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}


is_number() {
    [[ "$1" =~ ^[0-9]+([.][0-9]+)?$ ]]
}


kb_to_mb() {
    local value_kb="$1"

    if is_number "$value_kb"; then
        awk \
            -v kb="$value_kb" \
            -v decimals="$MEMORY_DECIMAL_PLACES" \
            'BEGIN {
                format = "%." decimals "f"
                printf format, kb / 1024
            }'
    else
        printf "NA"
    fi
}


# Quote a CSV field and escape internal quotation marks.
csv_quote() {
    local value="$1"

    value=${value//\"/\"\"}

    printf '"%s"' "$value"
}


extract_total_pss_kb() {
    local meminfo="$1"

    printf "%s\n" "$meminfo" |
        awk '
            /TOTAL PSS:/ {
                for (i = 1; i <= NF; i++) {
                    if ($i == "PSS:") {
                        print $(i + 1)
                        exit
                    }
                }
            }

            /^TOTAL[[:space:]]+[0-9]+/ {
                print $2
                exit
            }
        '
}


extract_total_rss_kb() {
    local meminfo="$1"

    printf "%s\n" "$meminfo" |
        awk '
            /TOTAL RSS:/ {
                for (i = 1; i <= NF; i++) {
                    if ($i == "RSS:") {
                        print $(i + 1)
                        exit
                    }
                }
            }
        '
}


# First CPU method:
# Ask top to print only PID, CPU percentage, and process arguments.
extract_cpu_from_ordered_top() {
    local top_output="$1"
    local target_pid="$2"

    printf "%s\n" "$top_output" |
        awk -v pid="$target_pid" '
            $1 == pid {
                value = $2
                gsub("%", "", value)

                if (value ~ /^[0-9]+([.][0-9]+)?$/) {
                    print value
                    exit
                }
            }
        '
}


# Second CPU method:
# Detect the %CPU column from the top header.
extract_cpu_from_top_header() {
    local top_output="$1"
    local target_pid="$2"

    printf "%s\n" "$top_output" |
        awk -v pid="$target_pid" '
            {
                clean = $0
                gsub(/\r/, "", clean)
            }

            clean ~ /PID/ && clean ~ /%CPU|CPU%/ {
                cpu_column = 0
                pid_column = 0

                for (i = 1; i <= NF; i++) {
                    if ($i == "PID") {
                        pid_column = i
                    }

                    if ($i == "%CPU" || $i == "CPU%") {
                        cpu_column = i
                    }
                }

                next
            }

            cpu_column > 0 && pid_column > 0 {
                if ($pid_column == pid) {
                    value = $cpu_column
                    gsub("%", "", value)

                    if (value ~ /^[0-9]+([.][0-9]+)?$/) {
                        print value
                        exit
                    }
                }
            }
        '
}


# Third CPU method:
# Find the PID row and then find any numeric field ending in %.
extract_cpu_from_generic_top() {
    local top_output="$1"
    local target_pid="$2"

    printf "%s\n" "$top_output" |
        awk -v pid="$target_pid" '
            {
                pid_found = 0

                for (i = 1; i <= NF; i++) {
                    if ($i == pid) {
                        pid_found = 1
                        break
                    }
                }

                if (pid_found == 1) {
                    for (i = 1; i <= NF; i++) {
                        if ($i ~ /^[0-9]+([.][0-9]+)?%$/) {
                            value = $i
                            gsub("%", "", value)
                            print value
                            exit
                        }
                    }
                }
            }
        '
}


collect_cpu_percent() {
    local target_pid="$1"

    local ordered_output
    local standard_output
    local cpu_value

    cpu_value=""

    # --------------------------------------------------------
    # Method 1: explicit top output columns
    # --------------------------------------------------------

    ordered_output=$(
        adb shell top \
            -b \
            -n 1 \
            -p "$target_pid" \
            -o PID,%CPU,ARGS \
            2>/dev/null |
            tr -d '\r'
    )

    cpu_value=$(
        extract_cpu_from_ordered_top \
            "$ordered_output" \
            "$target_pid"
    )

    if is_number "$cpu_value"; then
        printf "%s" "$cpu_value"
        return 0
    fi

    # --------------------------------------------------------
    # Method 2: ordinary top output with header detection
    # --------------------------------------------------------

    standard_output=$(
        adb shell top \
            -b \
            -n 1 \
            -p "$target_pid" \
            2>/dev/null |
            tr -d '\r'
    )

    cpu_value=$(
        extract_cpu_from_top_header \
            "$standard_output" \
            "$target_pid"
    )

    if is_number "$cpu_value"; then
        printf "%s" "$cpu_value"
        return 0
    fi

    # --------------------------------------------------------
    # Method 3: search the complete top output
    # --------------------------------------------------------

    standard_output=$(
        adb shell top \
            -b \
            -n 1 \
            2>/dev/null |
            tr -d '\r'
    )

    cpu_value=$(
        extract_cpu_from_top_header \
            "$standard_output" \
            "$target_pid"
    )

    if is_number "$cpu_value"; then
        printf "%s" "$cpu_value"
        return 0
    fi

    cpu_value=$(
        extract_cpu_from_generic_top \
            "$standard_output" \
            "$target_pid"
    )

    if is_number "$cpu_value"; then
        printf "%s" "$cpu_value"
        return 0
    fi

    printf "NA"
    return 1
}


# ============================================================
# 3. Check ADB
# ============================================================

if ! command -v adb >/dev/null 2>&1; then
    echo "Error: adb was not found."
    echo "Install Android Platform Tools or add adb to PATH."
    exit 1
fi


# ============================================================
# 4. Check connected devices
# ============================================================

DEVICE_COUNT=$(
    adb devices |
        awk '
            NR > 1 && $2 == "device" {
                count++
            }

            END {
                print count + 0
            }
        '
)

if [[ "$DEVICE_COUNT" -eq 0 ]]; then
    echo "Error: no authorized Android device is connected."
    echo
    echo "Run:"
    echo "  adb devices"
    exit 1
fi

if [[ "$DEVICE_COUNT" -gt 1 ]]; then
    echo "Error: more than one Android device is connected."
    echo "Disconnect unused devices before running this script."
    exit 1
fi


# ============================================================
# 5. Get device information
#
# This must happen before the filename is created.
# ============================================================

DEVICE_MANUFACTURER=$(
    adb shell getprop ro.product.manufacturer 2>/dev/null |
        tr -d '\r' |
        sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
)

DEVICE_MODEL=$(
    adb shell getprop ro.product.model 2>/dev/null |
        tr -d '\r' |
        sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
)

if [[ -z "$DEVICE_MANUFACTURER" ]]; then
    DEVICE_MANUFACTURER="UnknownManufacturer"
fi

if [[ -z "$DEVICE_MODEL" ]]; then
    DEVICE_MODEL="UnknownModel"
fi


# ============================================================
# 6. Sanitize filename values
# ============================================================

DEVICE_FILENAME=$(
    sanitize_filename_value "$DEVICE_MANUFACTURER"
)

RUNTIME_FILENAME=$(
    sanitize_filename_value "$RUNTIME"
)

DELEGATE_FILENAME=$(
    sanitize_filename_value "$DELEGATE"
)

PRECISION_FILENAME=$(
    sanitize_filename_value "$PRECISION"
)

if [[ -z "$DEVICE_FILENAME" ]]; then
    DEVICE_FILENAME="UnknownManufacturer"
fi

if [[ -z "$RUNTIME_FILENAME" ]]; then
    echo "Error: invalid runtime value."
    exit 1
fi

if [[ -z "$DELEGATE_FILENAME" ]]; then
    echo "Error: invalid delegate value."
    exit 1
fi

if [[ -z "$PRECISION_FILENAME" ]]; then
    echo "Error: invalid precision value."
    exit 1
fi


# ============================================================
# 7. Create output paths
# ============================================================

if ! mkdir -p "$OUTPUT_DIR"; then
    echo "Error: could not create output directory:"
    echo "  $OUTPUT_DIR"
    exit 1
fi

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

BASE_FILENAME="${DEVICE_FILENAME}-${RUNTIME_FILENAME}-${DELEGATE_FILENAME}-${PRECISION_FILENAME}-${TIMESTAMP}"

OUTPUT_FILE="${OUTPUT_DIR}/${BASE_FILENAME}.csv"
RAW_FILE="${OUTPUT_DIR}/${BASE_FILENAME}-raw.txt"


# ============================================================
# 8. Find application PID
# ============================================================

PID=$(
    adb shell pidof "$PACKAGE" 2>/dev/null |
        tr -d '\r' |
        awk '{print $1}'
)

if [[ -z "$PID" ]]; then
    echo "Error: $PACKAGE is not running."
    echo
    echo "Start TouchlessDroid and open the camera screen first."
    exit 1
fi


# ============================================================
# 9. Test CPU extraction before creating the full run
# ============================================================

echo "Testing CPU data extraction..."

TEST_CPU=$(
    collect_cpu_percent "$PID"
)

if ! is_number "$TEST_CPU"; then
    echo
    echo "Error: CPU usage could not be extracted from this device."
    echo
    echo "Run these commands and inspect their output:"
    echo
    echo "  adb shell top -b -n 1 -p $PID"
    echo
    echo "  adb shell top -b -n 1 -p $PID -o PID,%CPU,ARGS"
    echo
    echo "  adb shell top -b -n 1 | grep $PID"
    echo
    echo "No measurement file was created."
    exit 1
fi

echo "CPU extraction works: ${TEST_CPU}%"


# ============================================================
# 10. Create CSV file
# ============================================================

CSV_HEADER="sample,timestamp,device_manufacturer,device_model,runtime,delegate,precision,pid,cpu_percent,total_pss_mb,total_rss_mb"

if ! printf "%s\n" "$CSV_HEADER" > "$OUTPUT_FILE"; then
    echo "Error: could not create CSV file:"
    echo "  $OUTPUT_FILE"
    exit 1
fi

if [[ ! -f "$OUTPUT_FILE" ]]; then
    echo "Error: CSV file was not created."
    exit 1
fi


# ============================================================
# 11. Create raw diagnostic file
# ============================================================

{
    echo "TouchlessDroid resource measurement"
    echo "==================================="
    echo
    echo "Device manufacturer: $DEVICE_MANUFACTURER"
    echo "Device model:        $DEVICE_MODEL"
    echo "Package:             $PACKAGE"
    echo "PID:                 $PID"
    echo "Runtime:             $RUNTIME"
    echo "Delegate:            $DELEGATE"
    echo "Precision:           $PRECISION"
    echo "Start time:          $(date +"%Y-%m-%dT%H:%M:%S%z")"
    echo "Requested samples:   $DURATION_SECONDS"
    echo "Sampling interval:   approximately ${SAMPLE_INTERVAL_SECONDS}s"
    echo
} > "$RAW_FILE"


# ============================================================
# 12. Display configuration
# ============================================================

echo
echo "TouchlessDroid resource measurement"
echo "-----------------------------------"
echo "Device:       $DEVICE_MANUFACTURER $DEVICE_MODEL"
echo "Package:      $PACKAGE"
echo "PID:          $PID"
echo "Runtime:      $RUNTIME"
echo "Delegate:     $DELEGATE"
echo "Precision:    $PRECISION"
echo "Samples:      $DURATION_SECONDS"
echo "Interval:     approximately ${SAMPLE_INTERVAL_SECONDS}s"
echo
echo "CSV file:"
echo "  $OUTPUT_FILE"
echo
echo "Raw diagnostic file:"
echo "  $RAW_FILE"
echo


# ============================================================
# 13. Collect samples
# ============================================================

for ((sample = 1; sample <= DURATION_SECONDS; sample++)); do

    SAMPLE_START_TIME=$(date +%s)

    CURRENT_PID=$(
        adb shell pidof "$PACKAGE" 2>/dev/null |
            tr -d '\r' |
            awk '{print $1}'
    )

    if [[ -z "$CURRENT_PID" ]]; then
        echo
        echo
        echo "Error: TouchlessDroid stopped at sample $sample."
        echo "The incomplete CSV was retained:"
        echo "  $OUTPUT_FILE"
        exit 1
    fi

    if [[ "$CURRENT_PID" != "$PID" ]]; then
        echo
        echo
        echo "Error: TouchlessDroid restarted during measurement."
        echo "Original PID: $PID"
        echo "Current PID:  $CURRENT_PID"
        echo
        echo "Do not use this run as experimental data."
        exit 1
    fi

    TIMESTAMP_NOW=$(date +"%Y-%m-%dT%H:%M:%S")

    # --------------------------------------------------------
    # CPU measurement
    # --------------------------------------------------------

    CPU_PERCENT=$(
        collect_cpu_percent "$PID"
    )

    if ! is_number "$CPU_PERCENT"; then
        echo
        echo
        echo "Error: CPU extraction failed at sample $sample."
        echo "The incomplete measurement was retained for diagnosis."
        exit 1
    fi

    # --------------------------------------------------------
    # Memory measurement
    # --------------------------------------------------------

    MEMINFO=$(
        adb shell dumpsys meminfo "$PACKAGE" 2>/dev/null |
            tr -d '\r'
    )

    TOTAL_PSS_KB=$(
        extract_total_pss_kb "$MEMINFO"
    )

    TOTAL_RSS_KB=$(
        extract_total_rss_kb "$MEMINFO"
    )

    TOTAL_PSS_MB=$(
        kb_to_mb "$TOTAL_PSS_KB"
    )

    TOTAL_RSS_MB=$(
        kb_to_mb "$TOTAL_RSS_KB"
    )

    # --------------------------------------------------------
    # Create CSV row
    # --------------------------------------------------------

    {
        printf "%s," "$sample"
        csv_quote "$TIMESTAMP_NOW"
        printf ","
        csv_quote "$DEVICE_MANUFACTURER"
        printf ","
        csv_quote "$DEVICE_MODEL"
        printf ","
        csv_quote "$RUNTIME"
        printf ","
        csv_quote "$DELEGATE"
        printf ","
        csv_quote "$PRECISION"
        printf ",%s,%s,%s,%s\n" \
            "$PID" \
            "$CPU_PERCENT" \
            "$TOTAL_PSS_MB" \
            "$TOTAL_RSS_MB"
    } >> "$OUTPUT_FILE"

    # --------------------------------------------------------
    # Save raw diagnostic values
    # --------------------------------------------------------

    {
        echo "===== SAMPLE $sample ====="
        echo "Timestamp:    $TIMESTAMP_NOW"
        echo "PID:          $PID"
        echo "CPU:          $CPU_PERCENT%"
        echo "PSS KB:       ${TOTAL_PSS_KB:-NA}"
        echo "PSS MB:       $TOTAL_PSS_MB"
        echo "RSS KB:       ${TOTAL_RSS_KB:-NA}"
        echo "RSS MB:       $TOTAL_RSS_MB"
        echo
        echo "Raw memory output"
        echo "-----------------"
        echo "$MEMINFO"
        echo
    } >> "$RAW_FILE"

    printf \
        "\rSample %d/%d | CPU: %s%% | PSS: %s MB | RSS: %s MB" \
        "$sample" \
        "$DURATION_SECONDS" \
        "$CPU_PERCENT" \
        "$TOTAL_PSS_MB" \
        "$TOTAL_RSS_MB"

    # --------------------------------------------------------
    # Maintain approximately one-second start-to-start interval
    # --------------------------------------------------------

    SAMPLE_END_TIME=$(date +%s)
    SAMPLE_PROCESSING_TIME=$((SAMPLE_END_TIME - SAMPLE_START_TIME))
    REMAINING_SLEEP=$((SAMPLE_INTERVAL_SECONDS - SAMPLE_PROCESSING_TIME))

    if [[ "$REMAINING_SLEEP" -gt 0 ]]; then
        sleep "$REMAINING_SLEEP"
    fi

done


# ============================================================
# 14. Complete run
# ============================================================

{
    echo "End time: $(date +"%Y-%m-%dT%H:%M:%S%z")"
    echo "Status: completed"
} >> "$RAW_FILE"

echo
echo
echo "Measurement completed."
echo
echo "CSV file:"
echo "  $(pwd)/$OUTPUT_FILE"
echo
echo "Raw diagnostic file:"
echo "  $(pwd)/$RAW_FILE"
echo
echo "CSV rows:"
wc -l < "$OUTPUT_FILE"