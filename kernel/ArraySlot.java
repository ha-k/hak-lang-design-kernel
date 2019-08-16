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

public class ArraySlot extends ProtoExpression 
{
  private Expression _array;
  private Expression _index;

  public ArraySlot (Expression array, Expression index)
    {
      _array = array;
      _index = index;
    }  

  public final Expression copy ()
    {
      return new ArraySlot(_array.copy(),_index.copy());
    }

  public final Expression typedCopy ()
    {
      return new ArraySlot(_array.typedCopy(),_index.typedCopy()).addTypes(this);
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
          return _array;
        case 1:
          return _index;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _array = expression;
          break;
        case 1:
          _index = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  public final Expression array ()
    {
      return _array;
    }

  public final Expression index ()
    {
      return _index;
    }

  public final void setCheckedType ()    
    {
      if (setCheckedTypeLocked()) return;
      setCheckedType(type().copy());
      _array.setCheckedType();
      _index.setCheckedType();
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      Type indexSetType = new TypeParameter();
      ArrayType arrayType = new ArrayType(typeRef(),indexSetType);
      _array.typeCheck(arrayType,typeChecker);

      Global.dummyIndexSet().typeCheck(indexSetType,typeChecker);

      _index.typeCheck(typeChecker);
      typeChecker.prove(new ArrayIndexTypeGoal(_index,arrayType));

      typeChecker.disallowVoid(type(),this,"array base");
    }

  public final void compile (Compiler compiler)
    { // compile the index first, then the array, then the 'get' instruction:

      ArrayType arrayType = (ArrayType)_array.checkedType();
      boolean isMap = arrayType.isMap();
      boolean isIntIndexed = arrayType.indexType().boxSort() == Type.INT_SORT;

      _index.compile(compiler);
      if (isIntIndexed && _index.checkedType().isBoxedType())
        compiler.generateUnwrapper(Type.INT_SORT);

      _array.compile(compiler);

      switch (boxSort())
        {
        case Type.INT_SORT:
          if (isMap)
            if (isIntIndexed)
              compiler.generate(Instruction.GET_INT_INDEXED_MAP_I);
            else
              compiler.generate(Instruction.GET_MAP_I);
          else
            compiler.generate(Instruction.GET_ARRAY_I);
          return;
          
        case Type.REAL_SORT:
          if (isMap)
            if (isIntIndexed)
              compiler.generate(Instruction.GET_INT_INDEXED_MAP_R);
            else
              compiler.generate(Instruction.GET_MAP_R);
          else
            compiler.generate(Instruction.GET_ARRAY_R);
          return;
          
        default:
          if (isMap)
            if (isIntIndexed)
              compiler.generate(Instruction.GET_INT_INDEXED_MAP_O);
            else
              compiler.generate(Instruction.GET_MAP_O);
          else
            compiler.generate(Instruction.GET_ARRAY_O);
          return;
        }           
    }

  public final String toString ()
    {
      return _array + "[" + _index + "]";
    }
}
