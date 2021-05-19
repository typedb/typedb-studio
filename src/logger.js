/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

import * as winston from 'winston';
import path from 'path';
import fs from 'fs';
const electron = require('electron');
const { combine, timestamp, printf } = winston.format;


// Set the defaul logger to use console
// Default logger used to print exceptions when failing to instantiate a new customised logger
const console = new winston.transports.Console();
winston.add(console);


/**
* Windows:   %AppData%/typedb-workbase/logs
* Linux:     ~/.config/typedb-workbase/logs
* macOS DDL: ~/Library/Application Support/typedb-workbase/logs
*/
const logsDir = path.join(electron.remote.app.getPath('userData'), 'logs/');
if (!fs.existsSync(logsDir)) {
  try {
    fs.mkdirSync(logsDir);
  } catch (error) {
    winston.error(`Unable to create logs directory: ${error}`);
  }
}

const ERROR_LOG_FILE = path.join(logsDir, 'error.log');
// const COMBINED_LOG_FILE = path.join(logsDir, 'combined.log');

const messageFormat = printf(info => `${info.timestamp} - ${info.level}: ${info.message}`);

const logger = winston.createLogger({
  level: 'info',
  format: combine(
    timestamp(),
    messageFormat,
  ),
  transports: [
    //
    // - Write to all logs with level `info` and below to `combined.log`
    // - Write all logs error (and below) to `error.log`.
    //
    new winston.transports.File({ filename: ERROR_LOG_FILE, level: 'error' }),
    // new winston.transports.File({ filename: COMBINED_LOG_FILE }),
  ],
});

  //
  // If we're not in production then log to the `console` with the format:
  // `${info.level}: ${info.message} JSON.stringify({ ...rest }) `
  //
if (process.env.NODE_ENV !== 'production') {
  logger.add(new winston.transports.Console({
    format: winston.format.simple(),
  }));
}

export default logger;
