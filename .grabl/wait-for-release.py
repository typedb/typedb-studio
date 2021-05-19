#
# Copyright (C) 2021 Vaticle
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

import sys
import requests
import time
import datetime
import dateutil.parser

if len(sys.argv) <= 1:
    raise ValueError('Owner not specified.')
owner = sys.argv[1]

if len(sys.argv) <= 2:
    raise ValueError('Repository not specified.')
repo = sys.argv[2]

if len(sys.argv) <= 3:
    raise ValueError('Tag not specified.')
tag = sys.argv[3]

assets = sys.argv[4:]

startTime = datetime.datetime.now(datetime.timezone.utc)
waitTime = 10
while True:
    print('checking release...')
    res = requests.get(f'https://api.github.com/repos/{owner}/{repo}/releases/tags/{tag}')
    if res.status_code == 200:
        release = res.json()
        success = True
        for asset in assets:
            found = any(map(lambda x: x['name'] == asset and startTime < dateutil.parser.isoparse(x['created_at']), release['assets']))
            if not found:
                print(f'{asset} hasn\'t been released, waiting for {waitTime}s...')
                success = False
                break
        if success:
            break
    time.sleep(waitTime)
