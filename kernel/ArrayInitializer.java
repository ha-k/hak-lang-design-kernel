//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Thu Mar 24 11:46:02 2016 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import java.util.AbstractList;

import hlt.language.util.ArrayList;
import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

/**
 * This is the class for a special kind of expressions used essentially for initializing
 * an array with a specified dimension vector from an expression that evaluates to an
 * array whose dimension vectors is <i>almost</i> identical to that of the specified one.
 * By "almost", we mean that:
 * <ul>
 * <li> where an integer is expected as the specified dimension's indexable, the array's
 *      corresponding indexable must be the same integer;
 * <li> where a set is expected as the specified dimension's indexable, the array's
 *      corresponding indexable must be either an integer that must be equal to the set's
 *      size, or the same set;
 * <li> where an int range is expected as the specified dimension's indexable, the array's
 *      corresponding indexable must be either an integer that must be equal to the range's
 *      size, or the same range, or a set of integers that contains all the elements of
 *      the range.
 * </ul>
 * The type of the elements of the specified array must correspond to the one specified
 * as the element type in this expression.
 */
public class ArrayInitializer extends ProtoExpression 
{
  /**
   * This is the type of the array's elements. For example, if the dimension vector is
   * <tt>[d<sub>1</sub>,...,d<sub>n</sub>]</tt>, then the array must be of type
   * <tt>t[t<sub>1</sub>]...[t<sub>m</sub>]</tt>, where <tt>m >= n</tt>, and the element
   * type is then <tt>t</tt> if <tt>m=n</tt>, or <tt>t[t<sub>n+1</sub>]...[t<sub>m</sub>]</tt>
   * if <tt>m>n</tt>.
   */
  private Type _elementType;

  /**
   * This contains the expressions that comprise the dimension.
   */
  private Expression[] _dimension;

  /**
   * This is the array expression.
   */
  private Expression _array;

  /**
   * This is the depth of dimension up to which it is guaranteed that runtime-check
   * instructions have been generated thanks to having an explicit
   * <a href="ArrayExtension.html"><tt>ArrayExtension</tt></a>. Beyond this depth,
   * the <tt>ARRAY_INITIALIZE</TT> instruction must perform itself the necessary checks.
   */
  private int _extensionDepth;

  /**
   * Construct an array initializer with the specified element type, dimension
   * expressions, and array expression. <b>NB:</b> this is not a public constructor because
   * it is better - for performance reasons - to systematically wrap it into a
   * <a href="Let.html"><tt>Let</tt></tt></a> expression factoring out the computation
   * of the dimension expressions. This avoids recomputing those each and every time
   * they are needed in the evaluation of an array initializer, which first transforms
   * the array expression by peppering the dimension's indexables over its subarrays.
   * Thus, the static method <tt>construct</tt> is instead provided as a public proxy
   * doing the let-wrapping automatically.
   */
  private ArrayInitializer (Type elementType, ArrayList dimension, Expression array)
    {
      _elementType = elementType;
      _dimension = new Expression[dimension.size()];
      for (int i=_dimension.length; i-->0;)       
        _dimension[i] = (Expression)dimension.get(i);
      _extensionDepth = maxExtensionDepth(_array = array,0);
    }  

  private ArrayInitializer (Type elementType, Expression dimension[], Expression array, int extensionDepth)
    {
      _elementType = elementType;
      _dimension = dimension;
      _array = array;
      _extensionDepth = extensionDepth;
    }

  public final Expression copy ()
    {
      Expression[] dimension = new Expression[_dimension.length];
      for (int i=dimension.length; i-->0;)
        dimension[i] = _dimension[i].copy();
      return new ArrayInitializer(_elementType,dimension,_array.copy(),_extensionDepth);
    }   

  public final Expression typedCopy ()
    {
      Expression[] dimension = new Expression[_dimension.length];
      for (int i=dimension.length; i-->0;)
        dimension[i] = _dimension[i].typedCopy();
      return new ArrayInitializer(_elementType,dimension,_array.typedCopy(),_extensionDepth);
    }   

  /**
   * Returns a <a href="Let.html"><tt>Let</tt></a> wrapping a new array initializer
   * constructed with the specified parameters.
   */
  public static final Let construct (Type elementType, AbstractList dimension, Expression array)
    {
      ArrayList names = new ArrayList(dimension.size());
      ArrayList dims = new ArrayList(dimension.size());

      for (int i=0; i<dimension.size(); i++)
        {
          String name = ("$dimension_"+(i+1)+"$").intern();
          names.add(name);
          dims.add(new Dummy(name).setExtent((Expression)dimension.get(i)));
        }

      return new Let(names,dimension,new ArrayInitializer(elementType,dims,array));
    }

  /**
   * Returns the maximum depth at which all elements used for initialization are
   * <a href="ArrayExtension.html"><tt>ArrayExtension</tt></a>s; along the way,
   * whenever the specified expression is indeed an <tt>ArrayExtension</tt>, this
   * will decorate it with the dimension expression at the specified depth. When
   * so, this proceeds recursively at <tt>depth+1</tt> on the extension elements
   * until either the depth level exceeds the number of dimensions, or the
   * expression is not an array extension.
   */
  private final  int maxExtensionDepth (Expression expression, int depth)
    {
      if (depth < _dimension.length && expression instanceof ArrayExtension)
        {
          ArrayExtension extension = (ArrayExtension)expression;
          extension.setIndexSet(_dimension[depth]);
          Expression[] elements = extension.elements();
          depth = depth+1;

          int extensionDepth = 0;
          for (int i=extension.size(); i-->0;)
            extensionDepth = Math.min(extensionDepth,maxExtensionDepth(elements[i],depth));

          return 1 + extensionDepth;
        }

      return 0;
    }

  public final int numberOfSubexpressions ()
    {
      return _dimension.length + 1;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (0 <= n && n < _dimension.length)
        return _dimension[n];

      if (n == _dimension.length)
        return _array;

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
    {
      if (0 <= n && n < _dimension.length)
        _dimension[n] = expression;
      else
        if (n == _dimension.length)
          _array = expression;
        else
          throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final Type elementType ()
    {
      return _elementType.value();
    }

  public final Expression[] dimension ()
    {
      return _dimension;
    }

  public final Expression array ()
    {
      return _array;
    }

  public final void setCheckedType ()    
    {
      if (setCheckedTypeLocked()) return;
      setCheckedType(type().copy());
      _array.setCheckedType();
      for (int i=_dimension.length; i-->0;)
        _dimension[i].setCheckedType();
    }

  /**
   * Type-checking proceeds as follows:
   * <ol>
   * <li> each dimension expression is type-checked to have an index set type;
   * <li> this is then checked as an array type corresponding to the element type
   *      and the dimensions' types;
   * <li> the array is type-checked to be an array of the same number of dimensions
   *       as this,
   * <li> each dimension type is verified to be such that:
   *      <ul>
   *      <li> where this dimension type is int, that of the array must be int too;
   *      <li> where this dimension type is set, that of the array must be either
   *           int, or set of same base type;
   *      <li> where this dimension type is int range, that of the array must be
   *           either int, int range, or a set of int.
   *      </ul>
   * </ol>
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      for (int i=_dimension.length; i-->0;)
        _dimension[i].typeCheck(Global.dummyIndexSet(),typeChecker);

      typeChecker.typeCheck(this,elementType().array(_dimension));

      _array.typeCheck(elementType().array(_dimension.length),typeChecker);

      if (elementType().isVoid())
        typeChecker.error(new TypingErrorException("void array base type"),
                          this);

      for (int i=_dimension.length; i-->0;)
        _dimensionDummy(_dimension[i].type(),i)
          .typeCheck(((ArrayType)_array.type()).dimension(i),typeChecker);
    }

  private Expression _dimensionDummy (Type type, int index)
    {
      Symbol dimDummy = new Symbol("indexer "+(index+1));

      dimDummy.getCodeEntry(Type.INT());

      if (type.isSet())
        dimDummy.getCodeEntry(new SetType(type.baseType()));

      if (type == Type.INT_RANGE)
        {
          dimDummy.getCodeEntry(Type.INT_RANGE);
          dimDummy.getCodeEntry(new SetType(Type.INT()));
        }

      return new Global(dimDummy).setExtent(_dimension[index]);
    }

  /**
   * To compile an <tt>ArrayInitializer</tt>, we:
   * <ol>
   * <li> compile the array,
   * <li> compile the dimensions (ints are systematically boxed so that the value of
   *      a dimension's indexable is always popped off the object stack),
   * <li> compile the number of dimensions,
   * <li> compile the extension depth,
   * <li> generate an <tt>ARRAY_INITIALIZE</tt>.
   * </ol>
   */
  public final void compile (Compiler compiler)
    {
      // compile the array
      _array.compile(compiler);
      // compile the dimensions (ints are systematically boxed)
      for (int i=_dimension.length; i-->0;)
        {
          _dimension[i].compile(compiler);
          if (_dimension[i].checkedType().boxSort() == Type.INT_SORT)
            compiler.generate(Instruction.I_TO_O);
        }
      // compile the number of dimensions
      compiler.generate(new PushValueInt(_dimension.length));
      // compile the number of extension depth
      compiler.generate(new PushValueInt(_extensionDepth));
      // generate ARRAY_INITIALIZE
      compiler.generate(Instruction.ARRAY_INITIALIZE);
    }

  public final String toString ()
    {
      StringBuilder buf = new StringBuilder();

      buf.append(_elementType.value());

      for (int i=0; i<_dimension.length; i++)
        buf.append("[").append(_dimension[i]).append("]");

      return buf.append(" = ").append(_array).toString();
    }

}




