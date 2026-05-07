#!/usr/bin/env python3
# dcc_bridge.py — Dex2c Mega on-device bridge
# Writes result JSON to --result file (never relies on stdout capture).
# Called by Dex2cPythonBridge.java via ProcessBuilder.
#
# Usage:
#   python3 dcc_bridge.py --dex <dex_or_apk> --outdir <dir> --result <result.json> [--keys <keys_file>] [--obfuscate]

import sys
import os
import json
import traceback

# Make bundled dex2c + androguard importable
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)

def write_result(result_path, methods, errors):
    payload = {'success': len(methods), 'errors': errors, 'methods': methods}
    with open(result_path, 'w', encoding='utf-8') as f:
        json.dump(payload, f)

def main():
    import argparse
    import logging
    logging.basicConfig(level=logging.WARNING, stream=sys.stderr)

    parser = argparse.ArgumentParser()
    parser.add_argument('--dex',      required=True)
    parser.add_argument('--outdir',   required=True)
    parser.add_argument('--result',   required=True)
    parser.add_argument('--keys',     default=None)
    parser.add_argument('--obfuscate', action='store_true', default=False)
    args = parser.parse_args()

    os.makedirs(args.outdir, exist_ok=True)

    methods = []
    errors  = []

    try:
        from androguard.core import androconf
        from androguard.core.analysis import analysis
        from androguard.core.bytecodes import apk, dvm
        from androguard.util import read as ag_read
        from dex2c.compiler import Dex2C
        from dex2c.util import JniLongName, get_method_triple, is_native_method
    except Exception as e:
        write_result(args.result, [], ['Import error: ' + str(e) + '\n' + traceback.format_exc()])
        sys.exit(1)

    SKIP_PREFIXES = (
        'Landroid/', 'Ljava/', 'Ljavax/', 'Lkotlin/', 'Lkotlinx/',
        'Landroidx/', 'Lcom/google/', 'Ldalvik/', 'Lsun/', 'Lorg/apache/',
    )

    def get_target_classes(allowed_set):
        """Extract bare class descriptors from the allowed key set.
        Key format: 'Lcom/example/Foo;someMethod(args)ret'
        Returns a set of bytes objects like b'Lcom/example/Foo;'
        """
        if not allowed_set:
            return None
        classes = set()
        for key in allowed_set:
            try:
                semi = key.index(';')
                classes.add(key[:semi + 1].encode('utf-8'))
            except ValueError:
                pass
        return classes

    def load_dex_files(path, target_classes=None):
        """Load DEX files from APK or a single DEX file.
        When target_classes is provided (a set of UTF-8 class descriptor bytes),
        only DEX files whose raw bytes contain at least one target class are
        fully parsed — skipping irrelevant DEX files speeds up big multi-DEX APKs
        dramatically (e.g. 10 DEX files → 1-2 parsed).
        """
        kind = androconf.is_android(path)
        raw_list = []  # list of (name, bytes)
        if kind == 'APK':
            import zipfile
            with zipfile.ZipFile(path, 'r') as zf:
                for name in sorted(n for n in zf.namelist()
                                   if n.endswith('.dex') and '/' not in n):
                    raw_list.append((name, zf.read(name)))
        elif kind in ('DEX', 'DEY'):
            raw_list.append((path, ag_read(path)))
        else:
            raise ValueError('Not a DEX or APK: %s' % path)

        result = []
        for name, data in raw_list:
            if target_classes:
                # Fast bytes scan — DEX string pool stores descriptors as
                # raw UTF-8 so a simple memoryview search works perfectly.
                if not any(cls in data for cls in target_classes):
                    print('[dex2c] Skipping %s (no target classes)' % name, flush=True)
                    continue
            result.append(dvm.DalvikVMFormat(data))

        # Safety fallback: if no DEX matched (shouldn't happen), load all
        if not result:
            result = [dvm.DalvikVMFormat(data) for _, data in raw_list]
        return result

    def build_allowed_set(keys_file):
        if not keys_file or not os.path.exists(keys_file):
            return None
        allowed = set()
        with open(keys_file, 'r', encoding='utf-8') as f:
            for line in f:
                key = line.strip()
                if key:
                    # Java format:  Lcom/Foo;->bar(I)V
                    # Codehasan fmt: Lcom/Foo;bar(I)V  (no ->)
                    allowed.add(key.replace(';->', ';'))
        return allowed

    def should_compile(method, allowed_set):
        if is_native_method(method):
            return False
        cls_name, name, _ = get_method_triple(method)
        if name in ('<clinit>', '<init>'):
            return False
        if any(cls_name.startswith(p) for p in SKIP_PREFIXES):
            return False
        if allowed_set is None:
            return True
        full = ''.join(get_method_triple(method))
        return full in allowed_set

    try:
        allowed_set   = build_allowed_set(args.keys)
        target_classes = get_target_classes(allowed_set)
        dex_files     = load_dex_files(args.dex, target_classes)
        dex_analysis  = analysis.Analysis()
        for dex in dex_files:
            dex_analysis.add(dex)
    except Exception as e:
        write_result(args.result, [], ['Load error: ' + str(e) + '\n' + traceback.format_exc()])
        sys.exit(1)

    # Count eligible methods first so we can show [i/n] progress
    eligible = []
    for dex in dex_files:
        for m in dex.get_methods():
            if should_compile(m, allowed_set):
                eligible.append((dex, m))
    total = len(eligible)
    print('[dex2c] Found %d method(s) to transpile' % total, flush=True)

    compiled = {}
    dex_to_compiler = {}
    done = 0
    for dex, m in eligible:
        if dex not in dex_to_compiler:
            try:
                dex_to_compiler[dex] = Dex2C(dex, dex_analysis, args.obfuscate, False)
            except Exception as e:
                errors.append('Compiler init error: ' + str(e))
                continue
        compiler = dex_to_compiler[dex]
        triple   = get_method_triple(m)
        jni_name = JniLongName(*triple)
        if len(jni_name) > 220:
            done += 1
            continue
        done += 1
        cls_name, meth_name, proto = triple
        short = cls_name.split('/')[-1].rstrip(';') + '->' + meth_name + proto
        print('[dex2c] [%d/%d] %s' % (done, total, short), flush=True)
        try:
            code, _ = compiler.get_source_method(m)
            if code:
                compiled[triple] = code
        except Exception as e:
            errors.append('%s: %s' % (''.join(triple), str(e)))

    print('[dex2c] Writing %d C++ file(s)...' % len(compiled), flush=True)
    for triple, code in compiled.items():
        cls_name, meth_name, proto = triple
        fname   = JniLongName(*triple) + '.cpp'
        dex_key = cls_name + '->' + meth_name + proto
        try:
            with open(os.path.join(args.outdir, fname), 'w', encoding='utf-8') as fp:
                fp.write('#include "Dex2C.h"\n' + code)
            methods.append({'dex_key': dex_key, 'file': fname})
        except Exception as e:
            errors.append('Write error %s: %s' % (fname, str(e)))

    write_result(args.result, methods, errors)
    sys.exit(0 if methods else 1)

if __name__ == '__main__':
    try:
        main()
    except Exception as e:
        # Last-resort: write error to a side file so Java can find it
        import tempfile
        try:
            err_path = os.path.join(tempfile.gettempdir(), 'dcc_crash.txt')
            with open(err_path, 'w') as f:
                f.write(traceback.format_exc())
        except Exception:
            pass
        sys.stderr.write('FATAL: ' + str(e) + '\n')
        sys.stderr.write(traceback.format_exc())
        sys.exit(2)
