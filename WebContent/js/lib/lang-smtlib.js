/*

 Copyright (C) 2008 Google Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

*/


PR['registerLangHandler'](
    PR['createSimpleLexer'](
        [
         ['opn', /^\(+/, null, '('],
         ['clo', /^\)+/, null, ')'],
         // A line comment that starts with ;
         [PR['PR_COMMENT'], /^;[^\r\n]*/, null, ';'],
         // Whitespace
         [PR['PR_PLAIN'], /^[\t\n\r \xA0]+/, null, '\t\n\r \xA0'],
         // A double quoted, possibly multi-line, string.
         [PR['PR_STRING'], /^\"(?:[^\"\\]|\\[\s\S])*(?:\"|$)/, null, '"']
        ],
        [
         [PR['PR_KEYWORD'],
          /^(?:continued-execution|error|false|immediate-exit|incomplete|logic|memout|sat|success|theory|true|unknown|unsupported|unsat|_|!|as|BINARY|DECIMAL|exists|HEXADECIMAL|forall|let|NUMERAL|par|STRING|assert|check-sat|check-sat-assuming|declare-const|declare-datatype|declare-datatypes|declare-fun|declare-sort|define-fun|define-fun-rec|define-sort|echo|exit|get-assertions|get-assignment|get-info|get-model|get-option|get-proof|get-unsat-assumptions|get-unsat-core|get-value|pop|push|reset|reset-assertions|set-info|set-logic|set-option)\b/, 
          null
         ],
         [PR['PR_PLAIN'], /^[0-9a-zA-Z~!@\$%^&\*_\-\+=<>.\?\/_]+/],
         [PR['PR_TYPE'], /^:[0-9a-zA-Z~!@\$%^&\*_\-\+=<>.\?\/_]+/]
        ]),
    ['smtlib', 'smt', 'smt2']);
