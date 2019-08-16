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
 * This is the class of array slot update expressions.
 */
public class ArraySlotUpdate extends ProtoExpression
{
  /**
   * The array slot being updated.
   */
  private Expression _slot;

  /**
   * The expression whose value is assigned to the array slot.
   */
  private Expression _value;

  /**
   * Constructs an array slot update with the specified array slot and value.
   */
  public ArraySlotUpdate (Expression slot, Expression value)
    {
      _slot = slot;
      _value = value; 
    }

  public final Expression copy ()
    {
      return new ArraySlotUpdate(_slot.copy(),_value.copy());
    }

  public final Expression typedCopy ()
    {
      return new ArraySlotUpdate(_slot.typedCopy(),_value.typedCopy()).addTypes(this);
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
          return _slot;
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
          _slot = expression;
          break;
        case 1:
          _value = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  /**
   *  Returns the array of this array slot update;
   */
  public final Expression array ()
    {
      return ((ArraySlot)_slot).array();
    }

  /**
   *  Returns the index of this array slot update;
   */
  public final Expression index ()
    {
      return ((ArraySlot)_slot).index();
    }

  /**
   * Sets the checked type of this array slot update.
   */
  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      setCheckedType(type().copy());
      _slot.setCheckedType();
      _value.setCheckedType();
    }

  private boolean _slotIsBoxed = false;
  private boolean _valueIsBoxed = false;

  /**
   * Type-checks this array slot update in the context of the specified
   * <a href="TypeChecker.html"> <tt>TypeChecker</tt></a>.
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _slot.typeCheck(typeChecker);
      _slotIsBoxed = _slot.type().isBoxedType();        // DANGER: this should be trailed!

      _value.typeCheck(typeChecker);
      _valueIsBoxed = _value.type().isBoxedType();      // DANGER: this should be trailed!

      // NB: assigning void is prevented by the array slot type-check

      if (VOID_ASSIGNMENTS)
        {
          typeChecker.unify(_slot.typeRef(),_value.typeRef());
          typeChecker.typeCheck(this,Type.VOID);
        }
      else
        {
          typeChecker.typeCheck(this,_slot.typeRef());
          typeChecker.typeCheck(this,_value.typeRef());
          type().setBoxed(_slotIsBoxed);
        }
    }

  /**
   * To compile an array slot update, proceed thus:
   * <ol>
   * <li> compile the index,
   * <li> possibly unbox the index,
   * <li> compile the array,
   * <li> compile the value,
   * <li> possibly un/box the value,
   * <li> generate the 'set' instruction,
   * <li> possibly generate a pop instruction.
   * </ol>
   */
  public final void compile (Compiler compiler)
    { 
      ArrayType arrayType = (ArrayType)array().checkedType();
      boolean isMap = arrayType.isMap();
      boolean isIntIndexed = arrayType.indexType().boxSort() == Type.INT_SORT;

      index().compile(compiler);
      if (isIntIndexed && index().checkedType().isBoxedType())
        compiler.generateUnwrapper(Type.INT_SORT);

      array().compile(compiler);
      _value.compile(compiler);

      if (_slotIsBoxed)
        {
          if (!_valueIsBoxed)
            compiler.generateWrapper(_value.sort());
        }
      else
        if (_valueIsBoxed)
          compiler.generateUnwrapper(_value.sort());

      byte sort = _slotIsBoxed ? Type.OBJECT_SORT : _value.sort();

      compiler.generate(_setArraySlot(isMap,
                                      isIntIndexed,
                                      sort));

      if (VOID_ASSIGNMENTS)
        compiler.generateStackPop(sort);
    }

  private final Instruction _setArraySlot(boolean isMap, boolean isIntIndexed, byte sort)
    {
      if (isMap)
        switch (sort)
          {
          case Type.INT_SORT:
            return isIntIndexed ? Instruction.SET_INT_INDEXED_MAP_I
                                : Instruction.SET_MAP_I;
          case Type.REAL_SORT:
            return isIntIndexed ? Instruction.SET_INT_INDEXED_MAP_R
                                : Instruction.SET_MAP_R;
          default:
            return isIntIndexed ? Instruction.SET_INT_INDEXED_MAP_O
                                : Instruction.SET_MAP_O;
          }
      else
        switch (sort)
          {
          case Type.INT_SORT:
            return Instruction.SET_ARRAY_I;
          case Type.REAL_SORT:
            return Instruction.SET_ARRAY_R;
          default:
            return Instruction.SET_ARRAY_O;
          }
    }

  public final String toString ()
    {
      return _slot + " = " + _value;
    }

}
