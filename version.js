#!/usr/bin/env node

const fs = require('fs');

const package_json_filename = 'package.json';
const version_filename = 'VERSION';

const version = fs.readFileSync(version_filename).toString().trim();
const packageJsonContent = fs.readFileSync(package_json_filename).toString();
const packageJson = JSON.parse(packageJsonContent);

packageJson['version'] = version;

fs.writeFileSync(package_json_filename, JSON.stringify(packageJson, null, 4));
