#!/bin/bash
# Database backup script for AquaRush

set -e

BACKUP_DIR="/var/backups/aquarush"
DB_NAME="aqua_rush"
DB_USER="postgres"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/aquarush_${TIMESTAMP}.sql.gz"

# Create backup directory if not exists
mkdir -p ${BACKUP_DIR}

# Perform backup
pg_dump -U ${DB_USER} -h localhost ${DB_NAME} | gzip > ${BACKUP_FILE}

echo "Backup completed: ${BACKUP_FILE}"

# Keep only last 7 days of backups
find ${BACKUP_DIR} -name "aquarush_*.sql.gz" -mtime +7 -delete

echo "Old backups cleaned up"
