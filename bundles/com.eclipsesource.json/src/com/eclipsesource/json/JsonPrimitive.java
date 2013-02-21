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


class JsonPrimitive extends JsonValue {

  private final String value;

  JsonPrimitive( String value ) {
    this.value = value;
  }

  @Override
  public void write( JsonWriter writer ) throws IOException {
    writer.write( value );
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean isNull() {
    return this == NULL;
  }

  @Override
  public boolean isBoolean() {
    return this == TRUE || this == FALSE;
  }

  @Override
  public boolean isTrue() {
    return this == TRUE;
  }

}