#!/usr/bin/env bash

set -euo pipefail

WORK_DIR=$(realpath .)
PROJ_ID="ingest-vcf-header"
BUILD_PATH="${WORK_DIR}/build/distributions"
DIST_DIR="${BUILD_PATH}/${PROJ_ID}-0.1"
DB_PATH="/Users/bm646-admin/Downloads/alt_tile_test/"

rm -rf ${DB_PATH}

# Build the project
"${WORK_DIR}/gradlew" clean build

# Extract the distribution
tar -xf "${BUILD_PATH}/${PROJ_ID}-0.1.tar" -C "${BUILD_PATH}"

# Run the help command
"${DIST_DIR}/bin/${PROJ_ID}" \
    --vcf-path "/Users/bm646-admin/Development/common_data/tiledb_toy_maize/hvcf_files/A188.h.vcf.gz" \
    --db-path "/Users/bm646-admin/Downloads/alt_tile_test/"


