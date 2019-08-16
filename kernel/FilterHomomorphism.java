//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// Pleasehil DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Thu Mar 24 11:58:04 2016 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

import hlt.language.tools.Misc;

import hlt.language.util.ArrayList;

/**
 * This is a refinement of the <a href="Homomorphism.html"><tt>Homomorphism</tt></a>
 * class using a filter in the form of a boolean function sifting those elements of
 * the collection on which to apply the function. More precisely, it is the built-in
 * version of the general computation scheme whose instance is the following <i>"fhom"</i>
 * functional, which may be formulated recursively, for the case of a list collection, as:
 *
 * <p>
 * <pre>
 * <span style="color:brown">
 *      fhom[list,op,id](filter,f) = <b>if</b>   isEmpty(list)
 *                                   <b>then</b> id
 *                                   <b>else</b> <b>let</b> elt = head(list)
 *                                        <b>in</b>  <b>if</b>   filter(elt)
 *                                            <b>then</b> op(f(elt),fhom[tail(list),op,id](filter,f))
 *                                            <b>else</b> fhom[tail(list),op,id](filter,f)
 * </span>
 * </pre>
 *
 * This class is defined to enable an optimization of monoid comprehensions where a boolean
 * qualifier can be integrated directly into the homomorphism preceding it, thus doing away
 * with the generation of a larger set than necessary. When this is used after normalizing
 * monoid comprehension with unnesting of conditions to the uppermost scope they control
 * together with gathering them into a single logical <tt>and</tt>, the net effect is a
 * drastic reduction of the size of loops. Recall that a monoid comprehension
 * is an expression of the form:
 * <pre>
 * <span style="color:brown">
 * [op,id] { e | q<sub>1</sub>, ..., q<sub>n</sub> }
 * </span>
 * </pre>
 * where <tt>[op,id]</tt> define a monoid, <tt>e</tt> is an expression, and the
 * <tt>q<sub>i</sub></tt>'s are <i>qualifiers</i>. A qualifier is either a
 * (boolean) expression or a pair <tt>x &lt;- e</tt>, where <tt>x</tt> is
 * a variable and <tt>e</tt> is a (collection) expression. After normalization,
 * there may be at most <i>one</i> boolean qualifier in the sequence (which may
 * also be empty). Such a normalized monoid comprehension can then be translated
 * as follows:
 * <p>
 * <pre>
 * <span style="color:brown">
 * [op,id]{e | } = op(e,id);
 *
 * [op,id]{e | c} = <b>if</b> c <b>then</b> op(e,id) <b>else</b> id;
 *
 * [op,id]{e | x &lt;- e', c, Q} = <b>f_hom</b>(e', <b>lambda</b> x.[op,id]{e | Q}, op, id, <b>lambda</b> x.c);
 *
 * [op,id]{e | x &lt;- e', y &lt;- e'', Q} = <b>hom</b>(e', <b>lambda</b> x.[op,id]{e | y &lt;- e'', Q}, op, id);
 * </span>
 * </pre>
 */

public class FilterHomomorphism extends Homomorphism
{
  /**
   * This is the filtering boolean function applied to each element of the collection.
   */
  private Expression _filter;
  private Tables _tables;

  public FilterHomomorphism (Tables tables, Expression collection, Expression function,
                             Expression operation, Expression identity, Expression filter)
    {
      super(collection,function,operation,identity);
      _tables = tables;
      _filter = filter;
    }

  private FilterHomomorphism (Tables tables, Expression collection, Expression function,
                              Expression operation, Expression identity, Expression filter,
                              byte inPlace)
    {
      this(tables,collection,function,operation,identity,filter);
      _inPlace = inPlace;
    }

  public final Expression copy ()
    {
      FilterHomomorphism copy = new FilterHomomorphism(_tables,
                                                       _collection.copy(),
                                                       _function.copy(),
                                                       _operation.copy(),
                                                       _identity.copy(),
                                                       _filter.copy(),
                                                       _inPlace);
      if (_slicings != null)
        {
          Expression[] slicings = new Expression[_slicings.length];
          for (int i=slicings.length; i-->0;)
            slicings[i] = _slicings[i].copy();
          copy.setSlicings(slicings);
        }

      return copy;
    }

  public final Expression typedCopy ()
    {
      FilterHomomorphism copy = new FilterHomomorphism(_tables,
                                                       _collection.typedCopy(),
                                                       _function.typedCopy(),
                                                       _operation.typedCopy(),
                                                       _identity.typedCopy(),
                                                       _filter.copy(),
                                                       _inPlace);
      if (_slicings != null)
        {
          Expression[] slicings = new Expression[_slicings.length];
          for (int i=slicings.length; i-->0;)
            slicings[i] = _slicings[i].typedCopy();
          copy.setSlicings(slicings);
        }

      return copy.addTypes(this);
    }

  /**
   * Returns the number of subexpressions
   */
  public int numberOfSubexpressions ()
    {
      if (_filter == null)
        return super.numberOfSubexpressions();
      
      return super.numberOfSubexpressions() + 1;
    }

  /**
   * Returns the n-th subexpression of this expression; if there is no subexpression
   * at the given position, this throws a <tt>NoSuchSubexpressionException</tt>
   */
  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (_filter == null)
        return super.subexpression(n);
      
      switch (n)
        {
        case 0:
          return _collection;
        case 1:
          return _function;
        case 2:
          return _operation;
        case 3:
          return _identity;
        case 4:
          return _filter;
        default:
          int s = n-5;
          if (_slicings != null && s >= 0 && s < _slicings.length)
            return _slicings[s];
          throw new NoSuchSubexpressionException(this,n);
        }
    }

  public final Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
    {
      if (_filter == null)
        return super.setSubexpression(n,expression);
      
      switch (n)
        {
        case 0:
          _collection = expression;
          break;
        case 1:
          _function = expression;
          break;
        case 2:
          _operation = expression;
          break;
        case 3:
          _identity = expression;
          break;
        case 4:
          _filter = expression;
          break;
        default:
          int s = n-5;
          if (_slicings != null && s >= 0 && s < _slicings.length)
            _slicings[s] = expression;
          else
            throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      super.typeCheck(typeChecker);
      _filter.typeCheck(new FunctionType(_elementType,Type.BOOLEAN()),typeChecker);
    }

  public final void setCheckedType ()    
    {
      if (setCheckedTypeLocked()) return;

      _extractNewSlicings();
      if (_filter != null)
        _filter.setCheckedType();
      _setCheckedTypeLocked = false;
      super.setCheckedType();
    }

  /**
   * This visits the filter after type-checking to look for possible new slicings.
   * This is because some opaque types defined as tuple types may use accessor
   * applications rather than tuple projections. Identifying such slicings can
   * only be done after type-checking.
   */
  private final void _extractNewSlicings ()
    {
      ArrayList filters = new ArrayList();
      ArrayList newFilters = new ArrayList();
      ArrayList newSlicings = new ArrayList();

      Expression filter = ((Scope)_filter).body();
      Parameter parameter = ((Scope)_filter).parameter(0);
      _extractFilters(filter,filters);

      for (int i=0; i<filters.size(); i++)
        {
          filter = (Expression)filters.get(i);

          if (filter.isHiddenSlicing(_tables,parameter))
            newSlicings.add(filter);
          else
            newFilters.add(filter);
        }

      if (newFilters.isEmpty())
        _filter = null;
      else
        {
          filter = (Expression)newFilters.get(0);
          for (int i=1; i<newFilters.size(); i++)
            filter = new And(filter,(Expression)newFilters.get(i));
          ((Scope)_filter).setBody(filter);
        }

      if (!newSlicings.isEmpty())
        {
          int slicingsSize = _slicings == null ? 0 : _slicings.length;
          Expression[] slicings = new Expression[slicingsSize + newSlicings.size()];

          for (int i=0; i<slicingsSize; i++)
            slicings[i] = _slicings[i];

          for (int i=0; i<newSlicings.size(); i++)
            slicings[slicingsSize+i] = (Expression)newSlicings.get(i);

          _slicings = slicings;
        }
    }

  private final void _extractFilters (Expression filter, ArrayList filters)
    {
      if (!(filter instanceof And))
        {
          filters.add(filter);
          return;
        }

      _extractFilters(((And)filter).left(),filters);
      _extractFilters(((And)filter).right(),filters);
    }      

  protected final void _fixTypeBoxing ()
    {
      super._fixTypeBoxing();

      if (_filter != null)
        {
          FunctionType ftype = (FunctionType)_filter.checkedType();

          if (ftype.domain(0).kind() == Type.BOXABLE)
            {
              ((BoxableTypeConstant)ftype.domain(0)).setBoxed(false);
              ftype.unsetDomainBox(0);
            }
        }
    }

  public final void compile (Compiler compiler)
    {
      if (_filter == null)
        {
          super.compile(compiler);
          return;
        }

      int[][] slices = null;
      _fixTypeBoxing();

      _identity.compile(compiler);
      if (!_isInPlace()) _operation.compile(compiler);
      _function.compile(compiler);
      _filter.compile(compiler);
      if (_slicings != null)
        slices = _compileSlicings(compiler);
      _collection.compile(compiler);

      if (_isInPlace())
        switch (((Collection)_collection.checkedType()).baseType().sort())
          {
          case Type.INT_SORT:
            compiler.generate(Instruction.APPLY_IP_FHOM_I);              
            break;
          case Type.REAL_SORT:
            compiler.generate(Instruction.APPLY_IP_FHOM_R);
            break;
          case Type.OBJECT_SORT:
            if (_slicings == null)
              compiler.generate(Instruction.APPLY_IP_FHOM_O);                
            else
              compiler.generate(new ApplySlicedInPlaceObjectFilterHomomorphism(slices));
          }
      else
        switch (((Collection)_collection.checkedType()).baseType().sort())
          {
          case Type.INT_SORT:
            if (_isCollection())
              compiler.generate(new ApplyIntCollectionFilterHomomorphism(_tally()));
            else
              compiler.generate(Instruction.APPLY_FHOM_I);
            break;
          case Type.REAL_SORT:
            if (_isCollection())
              compiler.generate(new ApplyRealCollectionFilterHomomorphism(_tally()));
            else
              compiler.generate(Instruction.APPLY_FHOM_R);
            break;
          case Type.OBJECT_SORT:
            if (_slicings == null)
              if (_isCollection())
                compiler.generate(new ApplyObjectCollectionFilterHomomorphism(_tally()));
              else
                compiler.generate(Instruction.APPLY_FHOM_O);
            else
              if (_isCollection())
                compiler.generate(new ApplySlicedObjectCollectionFilterHomomorphism(slices,_tally()));
              else
                compiler.generate(new ApplySlicedObjectFilterHomomorphism(slices));
          }
    }

  public final String toString ()
    {
      if (_filter == null)
        return super.toString();
      
      StringBuilder buf = new StringBuilder("f_hom(");

      buf.append(_collection).append(',')
         .append(_function).append(',')
         .append(_operation).append(',')
         .append(_identity).append(',')
         .append(_filter);
      if (_slicings != null)
        buf.append(',').append(Misc.arrayToString(_slicings));
      buf.append(')');

      return buf.toString();
    }
}
