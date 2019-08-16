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
 * This is the class of expressions for updating tuple components.
 */
public class TupleUpdate extends ProtoExpression
{
  private Expression _component;
  private Expression _value;

  public TupleUpdate (Expression tupleComponent, Expression value)
    {
      _component = tupleComponent;
      _value = value;
    }

  public final Expression copy ()
    {
      return new TupleUpdate(_component.copy(),_value.copy());
    }

  public final Expression typedCopy ()
    {
      return new TupleUpdate(_component.typedCopy(),_value.typedCopy()).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return 2;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          return _component;
        case 1:
          return _value;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _component = expression;
          break;
        case 1:
          _value = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  public final Expression tuple ()
    {
      return ((TupleProjection)_component).tuple();
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _component.setCheckedType();
      _value.setCheckedType();
      _checkedType = type().copy();
    }

  private boolean _componentIsBoxed = false;
  private boolean _valueIsBoxed = false;

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _component.typeCheck(typeChecker);
      _componentIsBoxed = _component.type().isBoxedType();      // DANGER: this should be trailed!

      _value.typeCheck(typeChecker);
      _valueIsBoxed = _value.type().isBoxedType();              // DANGER: this should be trailed!

      typeChecker.disallowVoid(_value.type(),_value,"assigned value");

      if (VOID_ASSIGNMENTS)
        {
          typeChecker.unify(_component.typeRef(),_value.typeRef());
          typeChecker.typeCheck(this,Type.VOID);
        }
      else
        {
          typeChecker.typeCheck(this,_component.typeRef());
          typeChecker.typeCheck(this,_value.typeRef());
          type().setBoxed(_componentIsBoxed);
        }
    }

  /**
   * To compile a tuple update, proceed thus:
   * <ol>
   * <li> compile the tuple,
   * <li> compile the value,
   * <li> possibly un/box the value,
   * <li> generate the 'set' instruction,
   * <li> possibly generate a pop instruction.
   * </ol>
   */
  public final void compile (Compiler compiler)
    {
      tuple().compile(compiler);
      _value.compile(compiler);

      if (_componentIsBoxed)
        {
          if (!_valueIsBoxed)
            compiler.generateWrapper(_value.sort());
        }
      else
        if (_valueIsBoxed)
          compiler.generateUnwrapper(_value.sort());

      byte sort = _componentIsBoxed ? Type.OBJECT_SORT : _value.sort();
      compiler.generate(_setTupleComponent(sort));

      if (VOID_ASSIGNMENTS)
        compiler.generateStackPop(sort);
    }

  private final Instruction _setTupleComponent(byte sort)
    {
      int offset = ((TupleProjection)_component).offset();

      switch (sort)
        {
        case Type.INT_SORT:
          return new SetIntTupleComponent(offset);
        case Type.REAL_SORT:
          return new SetRealTupleComponent(offset);
        default:
          return new SetObjectTupleComponent(offset);
        }
    }

  public final String toString ()
    {
      return _component + " = " + _value;
    }
}
