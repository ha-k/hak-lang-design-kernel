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

import java.util.HashSet;
import java.util.HashMap;

public class Dummy extends Expression
{
  protected Tables _tables;
  protected String _name;
  protected Type _type = new TypeParameter();

  public Dummy (String name)
    {
      _name = name.intern();
    }

  public Dummy (Tables tables, String name)
    {
      _tables = tables;
      _name = name.intern();
    }

  public Dummy (Parameter parameter)
    {
      this(parameter.name());
      addTypes(parameter);
    }

  public Dummy (Tables tables, Parameter parameter)
    {
      this(tables,parameter.name());
      addTypes(parameter);
    }

  public Expression copy ()
    {
      return new Dummy(_tables,_name);
    }

  public Expression typedCopy ()
    {
      return new Dummy(_tables,_name).addTypes(this);
    }

  public final Tables tables ()
    {
      return _tables;
    }

  public final String name ()
    {
      return _name;
    }

  public final boolean containsFreeName (String name)
    {
      return name == _name;
    }  

  public final Type type ()
    {
      return _type;
    }

  public final void setType (Type type)
    {
      if (type != null) _type = type;
    }

  public final Type typeRef ()
    {
      return _type;
    }

  public final Expression substitute (HashMap substitution)
    {
      if (!substitution.isEmpty())
        {
          Expression expression = (Expression)substitution.get(_name);
          if (expression != null)
            return expression.typedCopy();
        }

      return this;
    }

  public Expression sanitizeNames (ParameterStack parameters)
    {
      Parameter parameter = parameters.getLocalParameter(_name);
      Expression actual = (parameter == null)
                        ? (Expression)new Global(_tables,_name)
                        : (Expression)new Local(parameter);
      return actual.addTypes(this).setExtent(this);
    }

  /**
   * A Dummy never gets to invoke this.
   */
  public final Type checkedType ()
    {
      throw new UnsupportedOperationException("method checkedType may not be called on a Dummy! ("
                                              +this+")");
      //      return null;
    }

  /**
   * A Dummy never gets to invoke this.
   */
  public final void setCheckedType ()
    {
      throw new UnsupportedOperationException("method setCheckedType may not be called on a Dummy! ("
                                              +this+")");
    }

  /**
   * A Dummy never gets to invoke this.
   */
  public final void setCheckedType (Type type)
    {
      throw new UnsupportedOperationException("method setCheckedType may not be called on a Dummy! ("
                                              +this+")");
    }

  /**
   * A Dummy never gets to invoke this.
   */
  public final Expression shiftOffsets (int intShift, int realShift, int objectShift,
                                        int intDepth, int realDepth, int objectDepth)
    {
      throw new UnsupportedOperationException("method shiftOffsets may not be called on a Dummy! ("
                                              +this+")");
      //      return this;
    }

  /**
   * A Dummy never gets to invoke this.
   */
  public final void sanitizeSorts (Enclosure enclosure)
    {
      throw new UnsupportedOperationException("method sanitizeSorts may not be called on a Dummy! ("
                                              +this+")");
    }

  /**
   * A Dummy never gets to invoke this.
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      throw new UnsupportedOperationException("method typeCheck may not be called on a Dummy! ("
                                              +this+")");
    }

  /**
   * A Dummy never gets to invoke this.
   */
  public final void compile (Compiler compiler)
    {
      throw new UnsupportedOperationException("method compile may not be called on a Dummy! ("
                                              +this+")");
    }

  public final boolean equals (Object other)
    {
      if (this == other)
        return true;

      if (!(other instanceof Dummy))
        return false;

      return _name == ((Dummy)other).name();
    }

  public String toString ()
    {
      return _name;
    }

}
