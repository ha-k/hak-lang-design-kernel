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
 * This is the class of native java objects that are built-in constants.
 */
public class BuiltinObjectConstant extends Constant
{
  protected Object _value;

  public BuiltinObjectConstant (Type type)
    {
      super(type);
    }

  public BuiltinObjectConstant (Object value, Type type)
    {
      super(type);
      _setValue(value);
    }

  public final Object value ()
    {
      return _value;
    }

  private void _setValue (Object value)
    {
      _value = value;
      if (value == null) setIsNull();
    }

  public final void compile (Compiler compiler)
    {
      compiler.generate(new PushValueObject(_value));
    }

  public boolean equals (Object other)
    {
      if (!(other instanceof BuiltinObjectConstant))
        return false;
      
      return _value.equals(((BuiltinObjectConstant)other).value());
    }

  public String toString ()
    {
      return _value.toString();
    }

}
