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
 * This is the class of real number constants.
 */
public class Real extends Constant
{
  private double _value;

  public Real (double value, boolean flag)
    {
      if (flag)
        _type = Type.REAL;
      else
        _type = Type.REAL();      
      _setValue(value);
    }

  public Real (double value)
    {
      super(Type.REAL());
      _setValue(value);
    }

  public final double value ()
    {
      return _value;
    }

  private final void _setValue (double value)
    {
      _value = value;
      if (value == 0.0) setIsNull();
    }

  public final void compile (Compiler compiler)
    {
      compiler.generate(new PushValueReal(_value));
    }

  public final boolean equals (Object other)
    {
      if (!(other instanceof Real))
        return false;
      
      return _value == ((Real)other).value();
    }

  public final String toString ()
    {
      return String.valueOf(_value);
    }
}
