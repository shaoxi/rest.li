/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.data.it;


import com.linkedin.data.element.DataElement;


/**
 * Evaluate if the Data object
 * is equal to the specified value.
 *
 * @author slim.
 */
public class ValueEqualsPredicate implements Predicate
{
  public ValueEqualsPredicate(Object value)
  {
    _value = value;
  }

  @Override
  public boolean evaluate(DataElement element)
  {
    return element.getValue().equals(_value);
  }

  private final Object _value;
}