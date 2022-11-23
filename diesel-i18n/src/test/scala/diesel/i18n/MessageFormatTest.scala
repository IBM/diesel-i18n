/*
 * Copyright 2021 The Diesel Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diesel.i18n

import munit.FunSuite

class MessageFormatTest extends FunSuite {
  import MessageFormat._

  test("no parameters") {
    assertEquals(MessageFormat("toto"), MessageFormat(Seq(TextSegment("toto"))))
  }

  test("escaped quote") {
    assertEquals(
      MessageFormat("foo '' bar"),
      MessageFormat(Seq(TextSegment("foo ' bar")))
    )
  }

  test("quoted text") {
    assertEquals(
      MessageFormat("foo 'toto' bar"),
      MessageFormat(Seq(
        TextSegment("foo toto bar")
      ))
    )
  }

  test("one parameter") {
    assertEquals(
      MessageFormat("foo {0} bar"),
      MessageFormat(Seq(TextSegment("foo "), ParameterSegment(0), TextSegment(" bar")))
    )
  }

  test("quoted non parameter") {
    assertEquals(
      MessageFormat("foo '{0}' bar"),
      MessageFormat(Seq(TextSegment("foo {0} bar")))
    )
  }

  test("leading parameter") {
    assertEquals(
      MessageFormat("{0} bar"),
      MessageFormat(Seq(
        ParameterSegment(0),
        TextSegment(" bar")
      ))
    )
  }

  test("trailing parameter") {
    assertEquals(
      MessageFormat("foo {0}"),
      MessageFormat(Seq(
        TextSegment("foo "),
        ParameterSegment(0)
      ))
    )
  }

  test("two parameter") {
    assertEquals(
      MessageFormat("foo {0} bar {13} gnu"),
      MessageFormat(Seq(
        TextSegment("foo "),
        ParameterSegment(0),
        TextSegment(" bar "),
        ParameterSegment(13),
        TextSegment(" gnu")
      ))
    )
  }

  test("specials from specs".ignore) {
    assertEquals(
      MessageFormat("'{''}'"),
      MessageFormat(Seq(TextSegment("{''}")))
    )
  }

  test("specials from specs: unmatched quote") {
    assertEquals(
      MessageFormat("'{0}"),
      MessageFormat(Seq(TextSegment("{0}")))
    )
  }

  test("specials from specs: unbalanced brace".ignore) {
    assertEquals(
      MessageFormat("ab {0'}' de"),
      MessageFormat(Seq())
    )
    assertEquals(
      MessageFormat("''{''"),
      MessageFormat(Seq())
    )
  }

  test("apply arguments") {
    assertEquals(MessageFormat("foo {0}")("bar"), "foo bar")
    assertEquals(MessageFormat("foo {1}")("?", "bar"), "foo bar")
    assertEquals(MessageFormat("foo {13}")("bar"), "foo ")
  }

  test("with arity") {
    assert(MessageFormat("foo {0}").withArity(1).isRight)
    assert(MessageFormat("foo {1} {0}").withArity(2).isRight)
    assert(MessageFormat("foo {0} {1}").withArity(2).isRight)
    assertEquals(MessageFormat("foo {0}").withArity(0), Left(1))
    assert(MessageFormat("foo {12}").withArity(13).isRight)
    assertEquals(MessageFormat("foo {12}").withArity(3), Left(13))
  }

  test("with arity and omitted parameters") {
    assert(MessageFormat("foo {0}").withArity(13).isRight)
    assert(MessageFormat("foo {1}").withArity(2).isRight)
    assert(MessageFormat("foo {1} {0}").withArity(13).isRight)
  }

  test("arity mismatch") {
    assertEquals(MessageFormat("foo {13} bar")("bla"), "foo  bar")
  }

  test("with prefix") {
    assertEquals(MessageFormat("foo {0}").withPrefix("gnu ")("bar"), "gnu foo bar")
  }

}

/*
https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html

Patterns and Their Interpretation
MessageFormat uses patterns of the following form:
 MessageFormatPattern:
         String
         MessageFormatPattern FormatElement String

 FormatElement:
         { ArgumentIndex }
         { ArgumentIndex , FormatType }
         { ArgumentIndex , FormatType , FormatStyle }

 FormatType: one of
         number date time choice

 FormatStyle:
         short
         medium
         long
         full
         integer
         currency
         percent
         SubformatPattern

Within a String, a pair of single quotes can be used to quote any arbitrary characters except single quotes.
For example, pattern string "'{0}'" represents string "{0}", not a FormatElement.

A single quote itself must be represented by doubled single quotes '' throughout a String.
For example, pattern string "'{''}'" is interpreted as a sequence of
'{ (start of quoting and a left curly brace),
    '' (a single quote), and }' (a right curly brace and end of quoting),
    not '{' and '}' (quoted left and right curly braces):
        representing string "{'}", not "{}".

A SubformatPattern is interpreted by its corresponding subformat, and subformat-dependent pattern rules apply. For example, pattern string "{1,number,$'#',##}" (SubformatPattern with underline) will produce a number format with the pound-sign quoted, with a result such as: "$#31,45". Refer to each Format subclass documentation for details.

Any unmatched quote is treated as closed at the end of the given pattern.
For example, pattern string "'{0}" is treated as pattern "'{0}'".

Any curly braces within an unquoted pattern must be balanced.
For example, "ab {0} de" and "ab '}' de" are valid patterns, but "ab {0'}' de", "ab } de" and "''{''" are not.

Warning:
The rules for using quotes within message format patterns unfortunately have shown to be somewhat confusing. In particular, it isn't always obvious to localizers whether single quotes need to be doubled or not. Make sure to inform localizers about the rules, and tell them (for example, by using comments in resource bundle source files) which strings will be processed by MessageFormat. Note that localizers may need to use single quotes in translated strings where the original version doesn't have them.
The ArgumentIndex value is a non-negative integer written using the digits '0' through '9', and represents an index into the arguments array passed to the format methods or the result array returned by the parse methods.

The FormatType and FormatStyle values are used to create a Format instance for the format element. The following table shows how the values map to Format instances. Combinations not shown in the table are illegal. A SubformatPattern must be a valid pattern string for the Format subclass used.
 */
