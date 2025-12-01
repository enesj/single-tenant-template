# Monitoring Scripts

Scripts for monitoring application processes, output, and browser console during development.

## Scripts

### `monitor_terminal.sh`
Advanced process monitoring script that runs commands with output capture and cleanup capabilities.

**Usage:**
```bash
./monitor_terminal.sh 'command'
```

**Features:**
- Captures command output to temporary files
- Process ID tracking for cleanup
- Handles Ctrl+C interruption gracefully
- Automatic cleanup of processes and temporary files
- Java process detection and termination for port 8080
- Used by `run-app.sh` for application monitoring

### `read_output.sh`
Reads output from commands started with `monitor_terminal.sh` with support for following output in real-time.

**Usage:**
```bash
./read_output.sh [-f]
```

**Options:**
- `-f` : Follow mode for continuous output monitoring

**Features:**
- Tracks file position to show only new content
- Stateful reading to avoid duplicate output
- Real-time following with configurable intervals
- Handles output file rotation and cleanup

### `read-console.sh`
Reads browser console logs from the running development application with various filtering options.

**Usage:**
```bash
./read-console.sh [options]
```

**Options:**
- `--follow, -f` : Follow console output continuously
- `--clear, -c` : Clear console before reading
- `--errors-only` : Show only error messages
- `--debug, -d` : Show debug output
- `--help, -h` : Show help message

**Features:**
- Connects to development server at localhost:8080
- Filters console output by type (errors, debug, etc.)
- Continuous monitoring support
- Integration with fe-tools console reader
- Automatic server availability check
