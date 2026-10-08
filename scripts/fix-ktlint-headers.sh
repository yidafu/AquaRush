#!/bin/bash

# Script to fix license headers to be ktlint compliant
set -e

YEAR=$(date +"%Y")

# Function to fix license header in a file to be ktlint compliant
fix_ktlint_header() {
    local file="$1"
    local temp_file=$(mktemp)

    echo "Fixing ktlint header format in: $file"

    # Find where the actual package declaration starts (skip existing header)
    local package_line=$(grep -n "^package " "$file" | cut -d: -f1 | head -1)

    if [ -z "$package_line" ]; then
        # If no package declaration, find first import or class declaration
        package_line=$(grep -n -E "^(import |class |interface |enum |object )" "$file" | cut -d: -f1 | head -1)
        if [ -z "$package_line" ]; then
            package_line=1
        fi
    fi

    # Extract content after the existing header
    tail -n +$package_line "$file" > "$temp_file"

    # Write ktlint compliant license header to the original file
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

echo "Fixing license headers to be ktlint compliant..."
echo "Current year: $YEAR"
echo "Expected format: /* Copyright 2025 AquaRush */"
echo ""

# Find all .kt files and fix them
while IFS= read -r -d '' file; do
    # Check if file needs fixing (doesn't match ktlint expected format)
    if head -n 5 "$file" | grep -q "Copyright.*AquaRush Team"; then
        fix_ktlint_header "$file"
        ((fixed++))
    fi
done < <(find . -type f -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" -not -path "*/node_modules/*" -print0)

echo ""
echo "================================="
echo "Summary:"
echo "Files fixed: $fixed"
echo "================================="
echo ""
echo "Ktlint format now requires:"
echo "/* Copyright 2025 AquaRush */"
echo "This matches the regex in .editorconfig:"
echo "^/\\*\\n \\* Copyright \\\\d{4} AquaRush\\\\n( \\* .*\\\\n)* \\*/$"
