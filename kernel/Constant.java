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

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

public class Constant extends ProtoExpression
{
  private static final byte _OTHER = 0;
  private static final byte _FALSE = 1;
  private static final byte _TRUE  = 2;

  public static final Constant VOID = new Constant(Type.VOID);

  private boolean _isNull = false;
  private byte _id = _OTHER;

  public Constant ()
    {
    }

  public Constant (Type type)
    {
      setType(type);
    }
    
  public final Expression copy ()
    {
      return this;
    }

  public final Expression typedCopy ()
    {
      return this;
    }

  private final Constant _setId (byte id)
    {
      _id = id;
      return this;
    }

  public static final Constant TRUE ()
    {
      return new Constant(Type.BOOLEAN())._setId(_TRUE);
    }
  
  public final boolean isTrue ()
    {
      return _id == _TRUE;
    }

  public static final Constant FALSE ()
    {
      return new Constant(Type.BOOLEAN())._setId(_FALSE);
    }
  
  public final boolean isFalse ()
    {
      return _id == _FALSE;
    }

  public static final Constant NULL ()
    {
      return (new Constant(new TypeParameter())).setIsNull();
    }
  
  public final Constant setIsNull ()
    {
      _isNull = true;
      return this;
    }

  public final boolean isNull ()
    {
      return _isNull;
    }

  public static final Constant NULL (Type type)
    {
      if (type.isVoid()) return VOID;
      if (type.isBoolean()) return FALSE();
      if (type.isInt()) return new Int(0);
      if (type.isReal()) return new Real(0.0);
      if (type.isString()) return new StringConstant();
      return (new Constant(type)).setIsNull();
    }

  public static Global WRAP_INT;
  public static Global UNWRAP_INT;
  public static Global WRAP_REAL;
  public static Global UNWRAP_REAL;

  /**
   * Initialization of some constants.
   */
  public static final void initialize (Tables tables)
    {
      WRAP_INT    = new Global(tables,"wrapInt").globalConstant(Type.INT(),Type.BOXED_INT());;
      UNWRAP_INT  = new Global(tables,"unwrapInt").globalConstant(Type.BOXED_INT(),Type.INT());
      WRAP_REAL   = new Global(tables,"wrapReal").globalConstant(Type.REAL(),Type.BOXED_REAL());
      UNWRAP_REAL = new Global(tables,"unwrapReal").globalConstant(Type.BOXED_REAL(),Type.REAL());
    }
      
  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _checkedType = type().copy();
    }

  public void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      if (_otherTypes != null)
        for (Iterator i=_otherTypes.iterator(); i.hasNext();)
          typeChecker.unify(type(),(Type)i.next(),this);
    }

  public void compile (Compiler compiler)
    {
      if (isVoid())
        {
          return;
        }

      if (isTrue())
        {
          compiler.generate(Instruction.PUSH_TRUE);
          return;
        }

      if (isFalse() || _isNull && checkedType().isBoolean())
        {
          compiler.generate(Instruction.PUSH_FALSE);
          return;
        }       

      if (_isNull)
        switch (boxSort())
          {
          case Type.VOID_SORT:
            return;
          case Type.INT_SORT:
            if (checkedType().isBoxedType())            
              compiler.generate(Instruction.PUSH_ZERO_I);
            else
              compiler.generate(Instruction.PUSH_0_I);
            return;
          case Type.REAL_SORT:
            if (checkedType().isBoxedType())
              compiler.generate(Instruction.PUSH_ZERO_R);
            else
              compiler.generate(Instruction.PUSH_0_R);
            return;
          default:
            if (checkedType().isString())
              {
                compiler.generate(Instruction.PUSH_EMPTY_STR);
                return;
              }

            compiler.generate(Instruction.PUSH_NULL);
            return;
          }

      compiler.generate(new PushValueObject(this));
    }

  public boolean equals (Object other)
    {
      if (!(other instanceof Constant))
        return false;

      if (_isNull)
        return ((Constant)other).isNull();

      return (this == other);
    }    

  private Type _getType ()
    {
      if (_checkedType != null)
        return _checkedType;

      return type();
    }

  public String toString ()
    {
      if (isVoid()) return "()";
      if (isFalse()) return "false";
      if (isTrue()) return "true";
      if (_isNull)
        return "null("+_getType()+")";
      return "<unknown constant>";
    }

}
