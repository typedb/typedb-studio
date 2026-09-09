#!/usr/bin/env bash
# Force the Debian package name to a canonical value.
#
# Tauri derives the deb Package field from `productName` by kebab-casing it, which turns
# "TypeDB Studio" into "type-db-studio", and it exposes no config override. So we rewrite the
# field after bundling. Only the control member is touched - data.tar is left byte-for-byte
# intact, which keeps file ownership correct without needing fakeroot.
set -euo pipefail

deb="${1:?usage: fix-deb-package-name.sh <path-to-deb> [package-name]}"
pkg_name="${2:-typedb-studio}"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

cp "$deb" "$work/pkg.deb"
(
    cd "$work"
    ar x pkg.deb

    control_member=""
    for f in control.tar.*; do
        if [ -e "$f" ]; then control_member="$f"; break; fi
    done
    if [ -z "$control_member" ]; then
        echo "fix-deb-package-name: no control.tar.* member found in $deb" >&2
        exit 1
    fi

    mkdir control
    tar -xf "$control_member" -C control

    current="$(sed -n 's/^Package: *//p' control/control | head -1)"
    if [ "$current" = "$pkg_name" ]; then
        echo "fix-deb-package-name: Package is already '$pkg_name', leaving $deb unchanged"
        exit 0
    fi
    echo "fix-deb-package-name: rewriting Package '$current' -> '$pkg_name'"

    sed "s/^Package: .*/Package: $pkg_name/" control/control > control.new
    mv control.new control/control

    case "$control_member" in
        *.xz)  (cd control && tar -cJf "../$control_member" .) ;;
        *.gz)  (cd control && tar -czf "../$control_member" .) ;;
        *.zst) (cd control && tar --zstd -cf "../$control_member" .) ;;
        *)
            echo "fix-deb-package-name: unsupported control compression: $control_member" >&2
            exit 1
            ;;
    esac

    # `ar r` replaces the member in place, preserving the debian-binary/control/data ordering
    # that dpkg requires.
    ar r pkg.deb "$control_member"
    cp pkg.deb "$deb"
)

echo "--- deb control metadata ---"
dpkg-deb -I "$deb" control 2>/dev/null || true
echo "--- end control metadata ---"
