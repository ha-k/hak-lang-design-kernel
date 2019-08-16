//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import java.util.AbstractList;

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

/**
 * This is the class denoting arrays.  An array may be indexed by:
 * <ul>
 * <li> integers from 0 to the array's size minus one (<i>i.e.</i>,
 *      like a C or Java array);
 * <li> integers in a given range <i><tt>l..u</tt></i>;
 * <li> elements of a set (in which case the array is in fact an
 *      <i>associative</i> map.
 * </ul>
 * All arrays are represented as unidimensional arrays - multidimensional
 * arrays are simply arrays of arrays.
 */

public class NewArray extends ProtoExpression 
{
  /**
   * This is the array's <i>base type</i> (<i>i.e.</i>, the type of its
   * elements when viewed as a one-dimensional vector).
   */
  private Type _baseType;

  /**
   * This contains the sizes for all dimensions.
   */
  private Expression[] _dimension;

  public NewArray (Type baseType, Expression[] dimension)
    {
      _baseType = baseType;
      _dimension = dimension;
    }  

  public NewArray (Type baseType, AbstractList dimension)
    {
      _baseType = baseType;
      _dimension = new Expression[dimension.size()];
      for (int i = 0; i<_dimension.length; i++)
        _dimension[i] = (Expression)dimension.get(i);
    }  

  public final Expression copy ()
    {
      Expression[] dimension = new Expression[_dimension.length];
      for (int i=dimension.length; i-->0;)
        dimension[i] = _dimension[i].copy();

      return new NewArray(_baseType,dimension);
    }

  public final Expression typedCopy ()
    {
      Expression[] dimension = new Expression[_dimension.length];
      for (int i=dimension.length; i-->0;)
        dimension[i] = _dimension[i].typedCopy();

      return new NewArray(_baseType,dimension).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return _dimension.length;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (n >= 0 && n < _dimension.length)
        return _dimension[n];

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      if (n >= 0 && n < _dimension.length)
        _dimension[n] = expression;
      else
        throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final Expression[] dimension ()
    {
      return _dimension;
    }

  public final Type baseType ()
    {
      return _baseType.value();
    }

  public final void setCheckedType ()    
    {
      if (setCheckedTypeLocked()) return;
      for (int i=0; i<_dimension.length; i++)
        _dimension[i].setCheckedType();
      setCheckedType(type().copy());
    }

  /**
   * To typecheck a new array, we typecheck:
   * <ol>
   * <li> each dimension to have an allowed index set type (<i>i.e.</i>, int,
   *      int range, or set),
   * <li> this as an array type with the base type and dimension types.
   * </ol>
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      for (int i=0; i<_dimension.length; i++)
        _dimension[i].typeCheck(Global.dummyIndexSet(),typeChecker);
      
      typeChecker.typeCheck(this,_baseType.array(_dimension));
    }

  /**
   * To compile a new array, we:
   * <ol>
   * <li> compile the last dimension,
   * <li> generate a <tt>PUSH_[ARRAY,MAP]_[I,R,O]</tt> instruction,
   * <li> compile the next to last dimension,
   * <li> generate a <tt>FILL_[ARRAY,MAP]_[I,R,O][AM]</tt> instruction,
   * <li> compile each of the other dimensions followed with a <tt>FILL_[ARRAY,MAP]_O[A,M]</tt>
   *      instruction.
   * </ol>
   */
  public final void compile (Compiler compiler)
    {
      Expression dimension = _dimension[_dimension.length-1];
      Type dimensionType = dimension.checkedType();
      boolean hasIntIndex = dimensionType.isInt();
      byte innerSort = ((ArrayType)_checkedType).innerType(_dimension.length).boxSort();

      byte previous = 0;

      dimension.compile(compiler);

      if (hasIntIndex)
        {
          if (dimensionType.isBoxedType())
            compiler.generateUnwrapper(Type.INT_SORT);

          switch (innerSort)
            {
            case Type.INT_SORT:
              previous = _kind(compiler.generate(Instruction.PUSH_ARRAY_I));
              break;

            case Type.REAL_SORT:
              previous = _kind(compiler.generate(Instruction.PUSH_ARRAY_R));
              break;

          case Type.OBJECT_SORT:
              previous = _kind(compiler.generate(Instruction.PUSH_ARRAY_O));
            }
        }
      else
        switch (innerSort)
          {
          case Type.INT_SORT:
            previous = _kind(compiler.generate(Instruction.PUSH_MAP_I));
            break;

          case Type.REAL_SORT:
            previous = _kind(compiler.generate(Instruction.PUSH_MAP_R));
            break;

          case Type.OBJECT_SORT:
            previous = _kind(compiler.generate(Instruction.PUSH_MAP_O));
          }

      for (int i=_dimension.length-1; i-->0;)
        {
          dimension = _dimension[i];
          hasIntIndex = dimension.checkedType().isInt();
          dimension.compile(compiler);

          if (hasIntIndex)
            {
              switch (previous)
                {
                case _IA:
                  compiler.generate(Instruction.FILL_ARRAY_IA);
                  break;
                case _IM:
                  compiler.generate(Instruction.FILL_ARRAY_IM);
                  break;
                case _RA:
                  compiler.generate(Instruction.FILL_ARRAY_RA);
                  break;
                case _RM:
                  compiler.generate(Instruction.FILL_ARRAY_RM);
                  break;
                case _OA:
                  compiler.generate(Instruction.FILL_ARRAY_OA);
                  break;
                case _OM:
                  compiler.generate(Instruction.FILL_ARRAY_OM);
                  break;
                }
              previous = _OA;
            }
          else
            {
              switch (previous)
                {
                case _IA:
                  compiler.generate(Instruction.FILL_MAP_IA);
                  break;
                case _IM:
                  compiler.generate(Instruction.FILL_MAP_IM);
                  break;
                case _RA:
                  compiler.generate(Instruction.FILL_MAP_RA);
                  break;
                case _RM:
                  compiler.generate(Instruction.FILL_MAP_RM);
                  break;
                case _OA:
                  compiler.generate(Instruction.FILL_MAP_OA);
                  break;
                case _OM:
                  compiler.generate(Instruction.FILL_MAP_OM);
                  break;
                }
              previous = _OM;
            }
        }
    }

  private static final byte _IA = 0;
  private static final byte _IM = 1;
  private static final byte _RA = 2;
  private static final byte _RM = 3;
  private static final byte _OA = 4;
  private static final byte _OM = 5;

  private static final byte _kind (Instruction inst)
    {
      if (inst == Instruction.PUSH_ARRAY_I)
        return _IA;
      if (inst == Instruction.PUSH_ARRAY_R)
        return _RA;
      if (inst == Instruction.PUSH_ARRAY_O)
        return _OA;
      if (inst == Instruction.PUSH_MAP_I)
        return _IM;
      if (inst == Instruction.PUSH_MAP_R)
        return _RM;
      if (inst == Instruction.PUSH_MAP_O)
        return _OM;
      return _OM;
    }

  public final String toString ()
    {
      StringBuilder buf = new StringBuilder("new "+baseType());
      for (int i=0; i<_dimension.length; i++)
        buf.append("["+_dimension[i]+"]");
      return buf.toString();
    }

}
