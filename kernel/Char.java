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
 * This is the class of character constants.
 */
public class Char extends Constant
{
  private char _value;

  public Char (char value, boolean flag)
    {
      if (flag)
        _type = Type.CHAR;
      else
        _type = Type.CHAR();      
      _value = value;
    }

  public Char (char value)
    {
      super(Type.CHAR());
      _value = value;
    }

  public final char value ()
    {
      return _value;
    }

  public final void compile (Compiler compiler)
    {
      compiler.generate(new PushValueInt(_value));
    }

  public final boolean equals (Object other)
    {
      if (!(other instanceof Char))
        return false;
      
      return _value == ((Char)other).value();
    }

  public final String toString ()
    {
      return "'"+String.valueOf(_value)+"'";
    }

}
