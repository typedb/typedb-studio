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

import CodeMirror from 'codemirror';
import placeholder from 'codemirror/addon/display/placeholder'; // eslint-disable-line no-unused-vars
import simpleMode from 'codemirror/addon/mode/simple'; // eslint-disable-line no-unused-vars

CodeMirror.defineSimpleMode('typeql', {
  // The start state contains the rules that are intially used
  start: [
    { regex: /#.*/, token: 'comment' },
    { regex: /(".*?")|('.*?')/, token: 'string' },
    { regex: /(match|isa|isa!|sub|sub!|has|id|type|limit|offset|sort|asc|desc|get|compute|path|from|to)(?![-a-zA-Z_0-9])/, // eslint-disable-line max-len
      token: 'keyword' },
    { regex: /true|false/, token: 'number' },
    { regex: /\$[-a-zA-Z_0-9]+/, token: 'variable' },
    { regex: /[-a-zA-Z_][-a-zA-Z_0-9]*/, token: 'identifier' },
    { regex: /[0-9]+(\.[0-9][0-9]*)?/, token: 'number' },
    { regex: /=|!=|>|<|>=|<=|\[|\]|contains|regex/, token: 'operator' },
  ],
  comment: [],
  // The meta property contains global information about the mode. It
  // can contain properties like lineComment, which are supported by
  // all modes, and also directives like dontIndentStates, which are
  // specific to simple modes.
  meta: {
    dontIndentStates: ['comment'],
    lineComment: '#',
  },
});

function TypeQLEditorHistory(typeQLCodeMirror) {
  const codeMirror = typeQLCodeMirror;

  let historyIndex = 0;
  let typeQLEditorHistory = [''];

  this.addToHistory = function addToHistory(query) {
    if (typeQLEditorHistory[typeQLEditorHistory.length - 1] !== query) {
      historyIndex += 1;
      typeQLEditorHistory.push(query);
    }
  };

  this.undo = function undo() {
    if (historyIndex > 0) {
      historyIndex -= 1;
      codeMirror.setValue(typeQLEditorHistory[historyIndex]);
    }
  };

  this.redo = function redo() {
    if (historyIndex < typeQLEditorHistory.length - 1) {
      historyIndex += 1;
      codeMirror.setValue(typeQLEditorHistory[historyIndex]);
    }
  };

  this.clearHistory = function clearHistory() {
    historyIndex = 0;
    typeQLEditorHistory = [''];
  };
}

function createTypeQLEditorHistory(codeMirror) {
  return new TypeQLEditorHistory(codeMirror);
}

function getCodeMirror(textArea) {
  return CodeMirror.fromTextArea(textArea, {
    lineNumbers: false,
    theme: 'dracula',
    mode: 'typeql',
    viewportMargin: Infinity,
    autofocus: true,
  });
}

export default { getCodeMirror, createTypeQLEditorHistory };
