#!/usr/bin/env python3
"""Legacy Spotygram desktop packager, intentionally disabled for the independent MLib fork."""

import argparse


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    parser.error(
        "Disabled: this legacy packager creates Spotygram-named bundles and manifests. "
        "MLib desktop release packaging must be migrated as a complete, separately signed format."
    )


if __name__ == "__main__":
    main()
