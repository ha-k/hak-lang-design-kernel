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
 * This is the class of integer constants.
 */
public class Int extends Constant
{
  private int _value;

  public Int (int value, boolean flag)
    {
      if (flag)
        _type = Type.INT;
      else
        _type = Type.INT();      
      _setValue(value);
    }

  public Int (int value)
    {
      super(Type.INT());
      _setValue(value);
    }

  public final int value ()
    {
      return _value;
    }

  private final void _setValue (int value)
    {
      _value = value;
      if (value == 0) setIsNull();
    }

  public final void compile (Compiler compiler)
    {
      compiler.generate(new PushValueInt(_value));
    }

  public final boolean equals (Object other)
    {
      if (!(other instanceof Int))
        return false;
      
      return _value == ((Int)other).value();
    }

  public final String toString ()
    {
      return String.valueOf(_value);
    }

}
