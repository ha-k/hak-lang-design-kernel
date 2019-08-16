//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import java.util.Iterator;
import java.util.HashSet;

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

public class Local extends Expression
{
  private Parameter _parameter;
  private int _offset = -1;

  public Local (Parameter parameter)
    {
      _parameter = parameter;
    }

  public Local (Parameter parameter, Type type)
    {
      _parameter = parameter;
      setType(type);
    }

  public final Expression copy ()
    {
      throw new UnsupportedOperationException("method copy may not be called on a Local! ("
                                              +this+")");
    }

  public final Expression typedCopy ()
    {
      throw new UnsupportedOperationException("method typedCopy may not be called on a Local! ("
                                              +this+")");
    }

  public final Parameter parameter ()
    {
      return _parameter;
    }

  public final int offset ()
    {
      return _offset;
    }

  public final void setOffset (int offset)
    {
      _offset = offset;
    }

  public final void incOffset ()
    {
      _offset++;
    }

  public final String name ()
    {
      return _parameter.name();
    }

  /**
   * Returns the type reference of this occurrence.
   */
  public final Type typeRef ()
    {
      return _parameter.typeRef();
    }

  /**
   * Returns the current type binding of this occurrence.
   */
  public final Type type ()
    {
      return _parameter.type();
    }

  public final void setType (Type type)
    {
      _parameter.addType(type);
    }

  /**
   * Returns the checked type of this occurrence.
   */
  public final Type checkedType ()
    {
      return _parameter.checkedType();
    }

  /**
   * No-op...
   */
  public void setCheckedType ()
    {
    }

  /**
   * No-op...
   */
  public final void setCheckedType (Type type)
    {
    }

  public final boolean containsFreeName (String name)
    {
      return name == name();
    }  

  /**
   * Ascertains that any other type for this local is that of the parameter this stands for.
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      if (_otherTypes != null)
        for (Iterator i=_otherTypes.iterator(); i.hasNext();)
          typeChecker.unify(type(),(Type)i.next(),this);
    }

  /**
   * A local is not supposed to be name-sanitized (it is already so!).
   */
  public Expression sanitizeNames (ParameterStack parameters)
    {
      throw new UnsupportedOperationException("method sanitizeNames may not be called on a Local! ("
                                              +this+")");
      //      return this;
    }

  /**
   * Sets this local's offset and its defining abstraction's depth of frame according
   * to its sort.
   */
  public void sanitizeSorts (Enclosure enclosure)
    {
      enclosure.setLocalInfo(this);
    }

  public Expression shiftOffsets (int intShift, int realShift, int objectShift,
                                  int intDepth, int realDepth, int objectDepth)
    {
      switch (boxSort())
        {
        case Type.INT_SORT:
          if (_offset >= intDepth) _offset += intShift;
          break;
        case Type.REAL_SORT:
          if (_offset >= realDepth) _offset += realShift;
          break;
          case Type.OBJECT_SORT:
          if (_offset >= objectDepth) _offset += objectShift;
          break;
        }

      return this;
    }

  public void compile (Compiler compiler)
    {   
      switch (boxSort())
        {
        case Type.INT_SORT:
          compiler.generate(new PushOffsetInt(_offset));
          return;
        case Type.REAL_SORT:
          compiler.generate(new PushOffsetReal(_offset));
          return;
        case Type.OBJECT_SORT:
          compiler.generate(new PushOffsetObject(_offset));
          return;
        }
    }

  public final String toString ()
    {
      return name();
    }

}
