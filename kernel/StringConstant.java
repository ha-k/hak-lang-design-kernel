//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

/**
 * This is the class of string constants.
 */
public class StringConstant extends BuiltinObjectConstant
{
  public StringConstant ()
    {
      this("");
    }

  public StringConstant (String value)
    {
      super(Type.STRING);
      _setValue(value);
    }

  private void _setValue (String value)
    {
      _value = value.intern();
      if (_value == "") setIsNull();
    }

  public final String stringValue ()
    {
      return (String)_value;
    }

  public boolean equals (Object other)
    {
      if (!(other instanceof StringConstant))
        return false;
      
      return _value == ((StringConstant)other).value();
    }

  public final String toString ()
    {
      return "\""+_value+"\"";
    }
}
