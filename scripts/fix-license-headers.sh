#!/bin/bash

# Script to fix license headers with formatting issues
set -e

YEAR=$(date +"%Y")

# Function to fix license header in a file
fix_license_header() {
    local file="$1"
    local temp_file=$(mktemp)

    # Skip if file doesn't have license issues
    if ! head -n 20 "$file" | grep -q "/${YEAR}"; then
        return
    fi

    echo "Fixing license header in: $file"

    # Remove the first 20 lines (the broken license header) and write to temp file
    tail -n +21 "$file" > "$temp_file"

    # Write correct license header to the original file
    cat > "$file" << EOF
/*
 * AquaRush
 *
 * Copyright (C) ${YEAR} AquaRush Team
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

    # Append the original content
    cat "$temp_file" >> "$file"
    rm "$temp_file"
}

# Counter for fixed files
fixed=0

echo "Fixing license headers with formatting issues..."
echo "Current year: $YEAR"
echo ""

# Find all .kt files and fix them
while IFS= read -r -d '' file; do
    if head -n 20 "$file" | grep -q "/${YEAR}"; then
        fix_license_header "$file"
        ((fixed++))
    fi
done < <(find . -type f -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" -not -path "*/node_modules/*" -print0)

echo ""
echo "================================="
echo "Summary:"
echo "Files fixed: $fixed"
echo "================================="