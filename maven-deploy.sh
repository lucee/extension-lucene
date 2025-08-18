#!/bin/bash

# Set environment variables
export MAVEN_USERNAME="KVwNruCc"
export MAVEN_PASSWORD="5vSGq/+AD0CQJo3UXByCOYXPp2IupcWaqX1vJPMSK0NM"
export GPG_PASSPHRASE="Susi Sorglos"


# Set the directory containing the Maven project
PROJECT_DIR="$(dirname "$0")"

# Navigate to the project directory
cd "$PROJECT_DIR" || {
    echo "Failed to navigate to project directory: $PROJECT_DIR"
    exit 1
}

# Ensure the pom.xml file exists in the directory
if [[ ! -f "pom.xml" ]]; then
    echo "No pom.xml found in the project directory: $PROJECT_DIR"
    exit 1
fi

# Run Maven clean install with the predefined goal
echo "Running Maven clean deploy in: $PROJECT_DIR"
mvn clean deploy -Dgoal=deploy

# Capture the exit code of the Maven command
EXIT_CODE=$?

# Check if Maven ran successfully
if [ $EXIT_CODE -eq 0 ]; then
    echo "Maven build completed successfully."
else
    echo "Maven build failed with exit code $EXIT_CODE."
fi

exit $EXIT_CODE
