#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Key Map Viewer 跨版本自动验证器
================================
对每个版本模块自动执行“真实启动冒烟测试”：
  1) 启动该版本的 runClient（默认环境：Fabric Loader + Fabric API + MaLiLib）
  2) 模组内置 AUTOTEST 钩子（KVM_AUTOTEST=1）会在日志中输出标记：
        AUTOTEST settings-opened —— 设置界面已成功打开（含 K 键路径）
        AUTOTEST hud-ok         —— HUD 已实际渲染
  3) 引擎轮询日志并读取该模块 run 目录的 kvm_bindings.json（原版+模组键位是否读到）
  4) 汇总输出 启动/开菜单/HUD/键位读取 四项判定。

用法：
    cd <项目根目录>
    python scripts/autotest.py [--module mc2612] [--timeout 150]
  未给 --module 时按顺序验证全部版本模块。
"""

import os
import subprocess
import sys
import time
import glob
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRADLEW = os.path.join(ROOT, 'gradlew.bat')

# (模块名, 覆盖版本说明)
MODULES = [
    ('mc1201', '1.20.1'),
    ('mc121',  '1.21.0 ~ 1.21.8'),
    ('mc1217', '1.21.9 ~ 1.21.11'),
    ('mc2612', '26.1.x / 26.2'),
]

MARK_SETTINGS = 'AUTOTEST settings-opened'
MARK_HUD = 'AUTOTEST hud-ok'
MARK_REGISTERED = '[keymapviewer] registered into MaLiLib mod list'

TIME_OPEN = 120   # 等启动+进世界的秒数
STALL = 240


def wait_marker(log_path, markers, timeout):
    """轮询日志，返回命中的标记集合；timeout 秒超时。"""
    found = set()
    end = time.time() + timeout
    pos = 0
    while time.time() < end:
        try:
            with open(log_path, 'r', encoding='utf-8', errors='ignore') as f:
                f.seek(pos)
                chunk = f.read()
                pos = f.tell()
        except (FileNotFoundError, OSError):
            chunk = ''
        for m in markers:
            if m in chunk:
                found.add(m)
        if found >= set(markers):
            return found
        if any(x in chunk for x in ('Game crashed', 'End of Stream', 'BUILD FAILED')):
            # 崩溃冒烟不提前结束，等 markers 超时给出 FAIL
            pass
        time.sleep(1)
    return found


def kill_tree(pid):
    if os.name == 'nt':
        subprocess.run(['taskkill', '/F', '/T', '/PID', str(pid)],
                       capture_output=True)
    else:
        try:
            os.killpg(pid, 9)
        except OSError:
            pass


def run_module(mod, timeout):
    log = os.path.join(ROOT, 'mc_logs', f'autotest_{mod}.log')
    os.makedirs(os.path.dirname(log), exist_ok=True)
    env = dict(os.environ)
    env['KVM_DEV'] = '1'
    env['KVM_AUTOTEST'] = '1'
    env['KVM_DEV_VIEW'] = 'list'
    env['JAVA_HOME'] = r"C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot"
    env['PATH'] = env['JAVA_HOME'] + r'\bin;' + env.get('PATH', '')

    with open(log, 'w', encoding='utf-8', errors='ignore') as logf:
        proc = subprocess.Popen(
            [GRADLEW, f':versions:{mod}:runClient'],
            cwd=ROOT,
            env=env,
            stdout=logf,
            stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NEW_PROCESS_GROUP if os.name == 'nt' else 0,
        )

    markers = wait_marker(log, {MARK_SETTINGS, MARK_HUD, MARK_REGISTERED}, timeout)

    # 尝试多个可能的 dump 路径（模块 run 目录 / 根 run 目录）
    dump_candidates = [
        os.path.join(ROOT, 'versions', mod, 'run', 'kvm_bindings.json'),
        os.path.join(ROOT, 'run', 'kvm_bindings.json'),
    ]
    bindings_ok = False
    dead = False
    for _ in range(STALL):
        for d in dump_candidates:
            try:
                if os.path.getsize(d) > 0:
                    bindings_ok = True
                    break
            except OSError:
                pass
        if bindings_ok:
            break
        if proc.poll() is not None:
            dead = True
            # 进程已退出：若标记已集齐则收工；否则再耐心多等几秒看 logs
            if markers >= {MARK_SETTINGS, MARK_HUD}:
                break
        time.sleep(1)

    kill_tree(proc.pid)

    opened = MARK_SETTINGS in markers
    hud = MARK_HUD in markers
    reg = MARK_REGISTERED in markers
    return dict(opened=opened, hud=hud, registered=reg, bindings=bindings_ok, log=log, exited=dead)


def main():
    only = None
    timeout = TIME_OPEN
    args = sys.argv[1:]
    if '--module' in args:
        only = args[args.index('--module') + 1]
    if '--timeout' in args:
        timeout = int(args[args.index('--timeout') + 1])

    print('=' * 78)
    print('备用 Key Map Viewer 跨版本自动验证')
    print('=' * 78)
    print(f"{'模块':<8}{'覆盖':<18}{'启动/进世界':<12}{'K键开菜单':<12}{'HUD渲染':<10}{'键位读取':<10}")
    print('-' * 78)
    results = []
    for mod, cov in MODULES:
        if only and mod != only:
            continue
        print(f'{mod:<8}{cov:<18}', end='', flush=True)
        r = run_module(mod, timeout)
        ok_start = '✔' if (r['opened'] or r['hud']) else '✘'
        ok_menu = '✔' if r['opened'] else '✘'
        ok_hud = '✔' if r['hud'] else '✘'
        ok_keys = '✔' if r['bindings'] else '✘'
        print(f'{ok_start:<12}{ok_menu:<12}{ok_hud:<10}{ok_keys:<10}  日志: {os.path.basename(r["log"])}')
        results.append((mod, r))
    print('-' * 78)
    print('判定说明：启动/进世界 = 日志出现设置标记（说明引擎跑起来且 tick 正常）；'
          'K键开菜单 = settings-opened 标记；HUD渲染 = hud-ok 标记；键位读取 = kvm_bindings.json 非空。')
    print('✔ 全部通过 / ✘ 未达标记：请把对应 mc_logs/autotest_<模块>.log 发来分析。')


if __name__ == '__main__':
    main()