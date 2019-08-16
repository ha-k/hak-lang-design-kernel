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

public class Parameter extends ProtoExpression
{
  private String _name;
  private boolean _isInternal;

  final static Parameter VOID = new Parameter("");
  static { VOID.setType(Type.VOID); }

  private static int _nameCounter;

  public Parameter ()
    {
      _name = ("?"+(_nameCounter++)).intern();
      _isInternal = true;
    }

  public Parameter (String name)
    {
      _name = name.intern();
    }

  public Parameter (Type type)
    {
      this();
      addType(type);
    }

  public Parameter (String name, Type type)
    {
      this(name);
      addType(type);
    }

  public Parameter (Dummy dummy)
    {
      this(dummy.name());
      addTypes(dummy);
    }

  public final Expression copy ()
    {
      return new Parameter(_name);
    }

  public final Expression typedCopy ()
    {
      return new Parameter(_name).addTypes(this);
    }

  public final boolean isInternal ()
    {
      return _isInternal;
    }

  public final String name ()
    {
      return _name;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _checkedType = type().copy();
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      typeChecker.typeCheck(this,_type);
    }

  /**
   * A Parameter never gets to invoke this.
   */
  public final void compile (Compiler compiler)
    {
      throw new UnsupportedOperationException("method compile may not be called on a Parameter! ("
                                              +this+")");
    }

  public final String toString ()
    {
      return name();
    }
}
