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
 * This is the class denoting an array literal - <i>i.e.</i>, expressions enumerating
 * elements that comprise the extension of an array or map. This is always a unidimensional
 * array. In addition, an array extension may be constrained by an index set coming from
 * an <a href="ArrayInitializer.html"><tt>ArrayInitializer</tt></a>. In this case, care
 * must be taken to reconcile the contents of the extension with the constraining index set.
 * This somewhat complicates (1) type-checking as it must proceed to verify that indexing types
 * are such that they can be made to agree, and (2) compiling that must generate instructions
 * to perform appropriate runtime tests.
 */

public class ArrayExtension extends ProtoExpression 
{
  /**
   * This contains the elements.
   */
  private Expression[] _elements;

  /**
   * This is the indexable of the extension, if there is one.
   */
  private Expression _indexable;

  /**
   * This expression is set by an <a href="ArrayInitializer.html"><tt>ArrayInitializer</tt></a>
   * to hold the value of the index allocation expression that must be consistent with the
   * structure of this array extension.
   */
  private Expression _indexSet;

  public ArrayExtension (AbstractList elements, AbstractList indices)
    {
      if (elements != null)
        {
          _elements = new Expression[elements.size()];

          for (int i=_elements.length; i-->0;)
            _elements[i] = (Expression)elements.get(i);

          if (indices != null)
            _indexable = new NewSet(indices).setExtent(this);
        }
    }

  private ArrayExtension (Expression[] elements, Expression indexable, Expression indexSet)
    {
      _elements = elements;
      _indexable = indexable;
      _indexSet = indexSet;
    }

  public final Expression copy ()
    {
      Expression[] elements = new Expression[_elements.length];
      for (int i=elements.length; i-->0;)
        elements[i] = _elements[i].copy();

      Expression indexable = _indexable == null ? null : _indexable.copy();
      Expression indexSet  = _indexSet  == null ? null : _indexSet.copy();

      return new ArrayExtension(elements,indexable,indexSet);
    }

  public final Expression typedCopy ()
    {
      Expression[] elements = new Expression[_elements.length];
      for (int i=elements.length; i-->0;)
        elements[i] = _elements[i].typedCopy();

      Expression indexable = _indexable == null ? null : _indexable.typedCopy();
      Expression indexSet  = _indexSet  == null ? null : _indexSet.typedCopy();

      return new ArrayExtension(elements,indexable,indexSet);
    }

  public final void setIndexSet (Expression indexSet)
    {
      _indexSet = indexSet;
    }

  public final int size ()
    {
      return _elements == null ? 0 : _elements.length;
    }

  public final int numberOfSubexpressions ()
    {
      return size() + (_indexable == null ? 0 : 1)
                    + (_indexSet  == null ? 0 : 1);
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (0 <= n && n < size())
        return _elements[n];

      if (n == size())
        if (_indexable != null)
          return _indexable;
        else
          if (_indexSet != null)
            return _indexSet;
          else
            throw new NoSuchSubexpressionException(this,n);

      if (n == size()+1 && _indexSet != null)
        return _indexSet;

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      if (0 <= n && n < size())
        _elements[n] = expression;
      else
        if (n == size())
          if (_indexable != null)
            _indexable = expression;
          else
            if (_indexSet != null)
              _indexSet = expression;
            else
              throw new NoSuchSubexpressionException(this,n);
        else
          if (n == size()+1 && _indexSet != null)
            _indexSet = expression;
          else
            throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final Expression[] elements ()
    {
      return _elements;
    }

  public final Expression indexable ()
    {
      return _indexable;
    }

  public final Expression indexSet ()
    {
      return _indexSet;
    }

  public final void setCheckedType ()    
    {
      if (setCheckedTypeLocked()) return;
      setCheckedType(type().copy());
      for (int i=size(); i-->0;)
        _elements[i].setCheckedType();
      if (_indexable != null)
        _indexable.setCheckedType();
    }

  /**
   * Typechecking proceeds as follows:
   * <ol>
   * <li> type-check all the elements to have the same type;
   * <li> if there is no indexable (<i>i.e.</i>, this is not a map)
   *      <ul>
   *      <li> force the indexer type to be int
   *      </ul>
   * <li> else (<i>i.e.</i>, this is a set-indexed map)
   *      <ul>
   *      <li> typecheck the indexable as a set
   *      <li> unify the indexable type and the indexer type
   *      </ul>
   * <li> if there is an index set (<i>i.e.</i>, this is constrained by an <tt>ArrayInitializer</tt>)
   *      <ul>
   *      <li> typecheck the index set to be a legal indexing type
   *      <li> unify the base type of the index set type with the indexable' base type if there is one
   *      </ul>
   * <li> type-check this array extension as an array with the extension's element type
   *      as array base type, and the array's indexer type as indexer type.
   * </ol>
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      TypeParameter elementType = new TypeParameter();
      TypeParameter indexerType = new TypeParameter();

      for (int i=size(); i-->0;)
        _elements[i].typeCheck(elementType,typeChecker);

      if (_indexable == null)
        typeChecker.unify(indexerType,Type.INT(),this);
      else
        {
          _indexable.typeCheck(new SetType(),typeChecker);
          typeChecker.unify(_indexable.typeRef(),indexerType,this);
        }

      if (_indexSet != null)
        {
          _indexSet.typeCheck((Global)Global.dummyIndexSet().setExtent(this),typeChecker);
          if (_indexable != null)
            {
              TypeParameter indexableBase = new TypeParameter();
              typeChecker.prove(new UnifyBaseTypeGoal(_indexable.typeRef(),indexableBase,this));
              typeChecker.prove(new BaseTypeGoal(_indexSet,indexableBase));
            }
        }

      typeChecker.typeCheck(this,new ArrayType(elementType,indexerType));
    }

  /**
   * <ol>
   * <li> compile the elements
   * <li> compile the size
   * <li> if there is no index set constraint:
   *      <ol>
   *      <li> compile the indexable, if there is one
   *      <li> generate <tt>MAKE_[ARRAY,MAP]_[I,R,O]</tt>
   *      </ol>
   *      if there is an index set constraint:
   *      <ol>
   *      <li> compile the index set
   *      <li> generate size tests and conversion check for index set
   *      <li> generate <tt>MAKE_ARRAY_[I,R,O]</tt> or <tt>SHUFFLE_MAP_[I,R,O]</tt> depending
   *           on whether or not indexables have been reconciled with a permutation array
   *      </ol>
   * </ol>
   */
  public final void compile (Compiler compiler)
    {
      // compile the elements
      for (int i = size(); i-->0;)
        _elements[i].compile(compiler);

      // compile the size
      compiler.generate(new PushValueInt(size()));
      
      if (_indexSet == null)
        {
          if (_indexable == null)
            // generate MAKE_ARRAY_[I,R,O]
            switch (((ArrayType)_checkedType).baseType().boxSort())
              {
              case Type.INT_SORT:
                compiler.generate(Instruction.MAKE_ARRAY_I);
                return;
              case Type.REAL_SORT:
                compiler.generate(Instruction.MAKE_ARRAY_R);
                return;
              case Type.OBJECT_SORT:
                compiler.generate(Instruction.MAKE_ARRAY_O);
                return;
              }

          // compile the indexable
          _indexable.compile(compiler);

          // generate MAKE_MAP_[I,R,O]
          switch (((ArrayType)_checkedType).baseType().boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.MAKE_MAP_I);
              return;
            case Type.REAL_SORT:
              compiler.generate(Instruction.MAKE_MAP_R);
              return;
            case Type.OBJECT_SORT:
              compiler.generate(Instruction.MAKE_MAP_O);
              return;
            }
        }

      // compile the index set
      _indexSet.compile(compiler);
        
      // generate size tests and conversion check for the index set
      if (_indexable == null)
        { // need to check that the size of the index set matches the actual size of the array
          if (_indexSet.checkedType().boxSort() == Type.INT_SORT)
            compiler.generate(Instruction.I_TO_O);
          compiler.generate(new PushValueInt(size()));
          // leave the index set on the stack if not int and check that its size is respected
          compiler.generate(Instruction.CHECK_ARRAY_SIZE);
        }
      else
        { // compile the indexable
          _indexable.compile(compiler);
          // convert the indexable from int set to range if needed, and check that it equals the indexset
          // leave the index set on the stack and push a permutation array
          compiler.generate(Instruction.RECONCILE_INDEXABLES);
        }

      if (_indexSet.checkedType().isInt()) // NB: necessarily index set has been popped off the stack
        // generate MAKE_ARRAY_[I,R,O]
        switch (((ArrayType)_checkedType).baseType().boxSort())
          {
          case Type.INT_SORT:
            compiler.generate(Instruction.MAKE_ARRAY_I);
            return;
          case Type.REAL_SORT:
            compiler.generate(Instruction.MAKE_ARRAY_R);
            return;
          default:
            compiler.generate(Instruction.MAKE_ARRAY_O);
            return;
          }

      // generate SHUFFLE_MAP_[I,R,O]
      switch (((ArrayType)_checkedType).baseType().boxSort())
        {
        case Type.INT_SORT:
          compiler.generate(Instruction.SHUFFLE_MAP_I);
          return;
        case Type.REAL_SORT:
          compiler.generate(Instruction.SHUFFLE_MAP_R);
          return;
        default:
          compiler.generate(Instruction.SHUFFLE_MAP_O);
          return;
        }
    }

  public final String toString ()
    {
      StringBuilder buf = new StringBuilder();

      if (_indexSet != null)
        buf.append("|").append(_indexSet).append("|");

      buf.append("#[");

      int size = size();

      if (_indexable == null)
        for (int i=0; i<size; i++)
          buf.append(_elements[i])
             .append(i == size-1 ? "" : ",");
      else
        {
          Expression[] elements = ((NewSet)_indexable).elements();
          for (int i=0; i<size; i++)
            buf.append(elements[i]).append(":").append(_elements[i])
               .append(i == size-1 ? "" : ",");;
        }

      buf.append("]#");

      return buf.toString();
    }

}




