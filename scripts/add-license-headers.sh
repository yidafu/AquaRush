#!/bin/bash

# Script to add AGPL license header to all Kotlin files
set -e


# Get current year
YEAR=$(date +"%Y")

# Function to check if file already has license header
has_license_header() {
    local file="$1"
    # Check if first 10 lines contain AGPL or copyright mentions
    head -n 10 "$file" | grep -qi "gnu\|copyright\|agpl" 2>/dev/null
}

# Function to add license header to file
add_license_header() {
    local file="$1"
    local temp_file=$(mktemp)

    # Create license header with correct year (ktlint compliant format)
    cat > "$temp_file" << 'EOF'
/*
 * Copyright YEAR_PLACEHOLDER AquaRush
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

EOF

    # Replace YEAR_PLACEHOLDER with actual year
    sed "s/YEAR_PLACEHOLDER/$YEAR/" "$temp_file" > "${temp_file}.fixed"
    mv "${temp_file}.fixed" "$temp_file"

    # Add original content
    cat "$file" >> "$temp_file"

    # Replace original file
    mv "$temp_file" "$file"

    echo "Added license header to: $file"
}

# Counter for processed files
processed=0
skipped=0

echo "Adding AGPL license headers to Kotlin files..."
echo "Current year: $YEAR"
echo ""

# Find all .kt files in the project
while IFS= read -r -d '' file; do
    if has_license_header "$file"; then
        echo "Skipped (already has header): $file"
        ((skipped++))
    else
        add_license_header "$file"
        ((processed++))
    fi
done < <(find . -type f -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" -not -path "*/node_modules/*" -print0)

echo ""
echo "================================="
echo "Summary:"
echo "Files processed: $processed"
echo "Files skipped:   $skipped"
echo "================================="