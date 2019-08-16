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
 * This constructs enables the conversion from a (possibly mutidimensional) array to
 * a indexed map as long as the base type and the number of dimensions agree.
 */

public class ArrayToMap extends ProtoExpression 
{
  private Expression _array;
  private Expression _indexable;

  public ArrayToMap (Expression array, Expression indexable)
    {
      _array = array;
      _indexable = indexable;
    }  

  public final Expression copy ()
    {
      return new ArrayToMap(_array.copy(),_indexable.copy());
    }

  public final Expression typedCopy ()
    {
      return new ArrayToMap(_array.typedCopy(),_indexable.typedCopy()).addTypes(this);
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
          return _indexable;
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
          _indexable = expression;
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

  public final Expression indexable ()
    {
      return _indexable;
    }

  public final void setCheckedType ()    
    {
      if (setCheckedTypeLocked()) return;
      _array.setCheckedType();
      _indexable.setCheckedType();
      setCheckedType(type().copy());
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      TypeParameter baseType = new TypeParameter();
      ArrayType arrayType = new ArrayType(baseType,Type.INT());
      _array.typeCheck(arrayType,typeChecker);
      _indexable.typeCheck(Global.dummyIndexable(),typeChecker);
      typeChecker.typeCheck(this,new ArrayType(baseType,_indexable.typeRef()));
    }
  
  public final void compile (Compiler compiler)
    { // compile the indexable first, then the array, then generate the 'ArrayToMap' instruction:

      _indexable.compile(compiler);
      _array.compile(compiler);

      switch (((ArrayType)_array.checkedType()).baseType().boxSort())
        {
        case Type.INT_SORT:
          compiler.generate(Instruction.ARRAY_TO_MAP_I);
          break;
        case Type.REAL_SORT:
          compiler.generate(Instruction.ARRAY_TO_MAP_R);
          break;
        case Type.OBJECT_SORT:
          compiler.generate(Instruction.ARRAY_TO_MAP_O);
        }
    }

  public final String toString ()
    {
      return "array2map" + "(" + _array + "," + _indexable + ")";
    }

}
