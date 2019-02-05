#!/usr/bin/env python

from __future__ import print_function

import sys
import subprocess
import time


_, ssh_host, ssh_user, ssh_pass = sys.argv

start_time = time.time()


def time_elapsed_in_seconds():
    return time.time() - start_time


def time_limit_exceeded():
    return (time_elapsed_in_seconds() // 60) > 10


command = [
    'sshpass',
    '-p',
    ssh_pass,
    'ssh',
    '-o',
    'StrictHostKeyChecking=no',
    '-o',
    'ConnectTimeout=2',
    '{}@{}'.format(ssh_user, ssh_host),
    'dir'
]

status = 255

while not time_limit_exceeded():
    status = subprocess.call(command)
    print('called command, status = {}; sleeping 5 secs (elapsed {} secs)'.format(status, time_elapsed_in_seconds()))
    if status == 0:
        break
    time.sleep(5)

exit(status)
