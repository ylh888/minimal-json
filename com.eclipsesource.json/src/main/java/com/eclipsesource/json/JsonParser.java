/*******************************************************************************
 * Copyright (c) 2013 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package com.eclipsesource.json;

import java.io.IOException;
import java.io.Reader;


class JsonParser {

  private final BufferedTextReader reader;
  private int current;

  JsonParser( Reader reader ) {
    this.reader = new BufferedTextReader( reader );
  }

  JsonValue parse() throws IOException {
    read();
    skipWhiteSpace();
    JsonValue result = readValue();
    skipWhiteSpace();
    if( !isEndOfText( current ) ) {
      throw error( "Unexpected character" );
    }
    return result;
  }

  private JsonValue readValue() throws IOException {
    switch( current ) {
    case 'n':
      return readNull();
    case 't':
      return readTrue();
    case 'f':
      return readFalse();
    case '"':
      return readString();
    case '[':
      return readArray();
    case '{':
      return readObject();
    case '-':
    case '0':
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
      return readNumber();
    default:
      throw expected( "value" );
    }
  }

  private JsonArray readArray() throws IOException {
    read();
    JsonArray array = new JsonArray();
    skipWhiteSpace();
    if( readChar( ']' ) ) {
      return array;
    }
    do {
      skipWhiteSpace();
      array.add( readValue() );
      skipWhiteSpace();
    } while( readChar( ',' ) );
    if( !readChar( ']' ) ) {
      throw expected( "',' or ']'" );
    }
    return array;
  }

  private JsonObject readObject() throws IOException {
    read();
    JsonObject object = new JsonObject();
    skipWhiteSpace();
    if( readChar( '}' ) ) {
      return object;
    }
    do {
      skipWhiteSpace();
      String name = readName();
      skipWhiteSpace();
      if( !readChar( ':' ) ) {
        throw expected( "':'" );
      }
      skipWhiteSpace();
      object.add( name, readValue() );
      skipWhiteSpace();
    } while( readChar( ',' ) );
    if( !readChar( '}' ) ) {
      throw expected( "',' or '}'" );
    }
    return object;
  }

  private JsonValue readNull() throws IOException {
    read();
    readRequiredChar( 'u' );
    readRequiredChar( 'l' );
    readRequiredChar( 'l' );
    return JsonValue.NULL;
  }

  private JsonValue readTrue() throws IOException {
    read();
    readRequiredChar( 'r' );
    readRequiredChar( 'u' );
    readRequiredChar( 'e' );
    return JsonValue.TRUE;
  }

  private JsonValue readFalse() throws IOException {
    read();
    readRequiredChar( 'a' );
    readRequiredChar( 'l' );
    readRequiredChar( 's' );
    readRequiredChar( 'e' );
    return JsonValue.FALSE;
  }

  private void readRequiredChar( char ch ) throws IOException {
    if( !readChar( ch ) ) {
      throw expected( "'" + ch + "'" );
    }
  }

  private JsonValue readString() throws IOException {
    read();
    StringBuilder buffer = null;
    reader.startCapture();
    while( current != '"' ) {
      if( current == '\\' ) {
        if( buffer == null ) {
          buffer = new StringBuilder();
        }
        buffer.append( reader.endCapture() );
        readEscape( buffer );
        reader.startCapture();
      } else if( current < 0x20 ) {
        throw expected( "valid string character" );
      } else {
        read();
      }
    }
    String capture = reader.endCapture();
    if( buffer != null ) {
      buffer.append( capture );
      capture = buffer.toString();
      buffer.setLength( 0 );
    }
    read();
    return new JsonString( capture );
  }

  private void readEscape( StringBuilder buffer ) throws IOException {
    read();
    switch( current ) {
    case '"':
    case '/':
    case '\\':
      buffer.append( (char)current );
      break;
    case 'b':
      buffer.append( '\b' );
      break;
    case 'f':
      buffer.append( '\f' );
      break;
    case 'n':
      buffer.append( '\n' );
      break;
    case 'r':
      buffer.append( '\r' );
      break;
    case 't':
      buffer.append( '\t' );
      break;
    case 'u':
      char[] hexChars = new char[4];
      for( int i = 0; i < 4; i++ ) {
        read();
        if( !isHexDigit( current ) ) {
          throw expected( "hexadecimal digit" );
        }
        hexChars[i] = (char)current;
      }
      buffer.append( (char)Integer.parseInt( String.valueOf( hexChars ), 16 ) );
      break;
    default:
      throw expected( "valid escape sequence" );
    }
    read();
  }

  private JsonValue readNumber() throws IOException {
    reader.startCapture();
    readChar( '-' );
    int firstDigit = current;
    if( !readDigit() ) {
      throw expected( "digit" );
    }
    if( firstDigit != '0' ) {
      while( readDigit() ) {
      }
    }
    readFraction();
    readExponent();
    return new JsonNumber( reader.endCapture() );
  }

  private boolean readFraction() throws IOException {
    if( !readChar( '.' ) ) {
      return false;
    }
    if( !readDigit() ) {
      throw expected( "digit" );
    }
    while( readDigit() ) {
    }
    return true;
  }

  private boolean readExponent() throws IOException {
    if( !readChar( 'e' ) && !readChar( 'E' ) ) {
      return false;
    }
    if( !readChar( '+' ) ) {
      readChar( '-' );
    }
    if( !readDigit() ) {
      throw expected( "digit" );
    }
    while( readDigit() ) {
    }
    return true;
  }

  private String readName() throws IOException {
    if( current != '"' ) {
      throw expected( "name" );
    }
    return readString().asString();
  }

  private boolean readChar( char ch ) throws IOException {
    if( current != ch ) {
      return false;
    }
    read();
    return true;
  }

  private boolean readDigit() throws IOException {
    if( !isDigit( current ) ) {
      return false;
    }
    read();
    return true;
  }

  private void skipWhiteSpace() throws IOException {
    while( isWhiteSpace( current ) && !isEndOfText( current ) ) {
      read();
    }
  }

  private void read() throws IOException {
    if( isEndOfText( current ) ) {
      throw error( "Unexpected end of input" );
    }
    current = reader.read();
  }

  private ParseException expected( String expected ) {
    if( isEndOfText( current ) ) {
      return error( "Unexpected end of input" );
    }
    return error( "Expected " + expected );
  }

  private ParseException error( String message ) {
    int offset = isEndOfText( current ) ? reader.getIndex() : reader.getIndex() - 1;
    return new ParseException( message, offset, reader.getLine(), reader.getColumn() -1 );
  }

  private static boolean isWhiteSpace( int ch ) {
    return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
  }

  private static boolean isDigit( int ch ) {
    return ch >= '0' && ch <= '9';
  }

  private static boolean isHexDigit( int ch ) {
    return ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
  }

  private static boolean isEndOfText( int ch ) {
    return ch == -1;
  }

}