//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Thu Mar 24 11:59:25 2016 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

import hlt.language.tools.Misc;

import hlt.language.util.ArrayList;

/**
 * This is the class of objects denoting (monoid) homomorphisms. Such an expression
 * means to iterate through a collection, applying a function to each element,
 * accumulating the results along the way with an operation, and returning the end
 * result. More precisely, it is the built-in version of the general computation
 * scheme whose instance is the following <i>"hom"</i> functional, which may be
 * formulated recursively, for the case of a list collection, as:
 *
 * <p>
 * <pre>
 * <span style="color:brown">
 *      hom[list,op,id](f) = <b>if</b> isEmpty(list)
 *                           <b>then</b> id
 *                           <b>else</b> op(f(head(list)),
 *                                   hom[tail(list),op,id](f))
 * </span>
 * </pre>
 * <p>
 *
 * Clearly, this scheme extends a function <i>f</i> to a homomorphism of monoids,
 * from the monoid of lists to the monoid defined by <i>(op,id)</i>.
 *
 * <p>
 *
 * Thus, an object of this class denotes the result of applying such a homomorphic
 * extension of a function (<i>f</i>) to an element of collection monoid
 * (<i>i.e.</i>, a data structure such as a set, a list, or a bag), the image
 * monoid being implicitly defined by the binary operation (<i>op</i>) - also
 * called the <i>accumulation</i> operation. It is made to work iteratively.
 *
 * <p>
 *
 * For technical reasons, we need to treat specially so-called <i>collection</i>
 * homomorphisms; <i>i.e.</i>, those whose accumulation operation constructs a
 * collection, such as a set. Although a collection homomorphism can conceptualy be
 * expressed with the general scheme, the function applied to an element of the
 * collection will return a collection (<i>i.e.</i>, a <i>free</i> monoid) element,
 * and the result of the homomorphism is then the result of tallying the partial
 * collections coming from applying the function to each element into a final
 * "concatenation." 
 *
 * <p>
 *
 * Other (non-collection) homomorphisms are called <i>primitive</i> homomorphisms.
 * For those, the function applied to all elements of the collection will return a
 * <i>computed</i> element that may be directly composed with the other results.
 * Thus, the difference between the two kinds of (collection or primitive)
 * homomorphisms will appear in the typing and the code generated (a collection
 * homomorphism requiring an extra loop for tallying partial results into the final
 * collection). It is easy to make the distinction between the two kinds of
 * homomorphisms thanks to the type of the accumulation operation (see below).
 *
 * <p>
 *
 * Therefore, a <i>collection homomorphism</i> expression constructing a collection
 * of type <tt>c2(B)</tt> consists of:
 * <ul>
 * <li> the collection iterated over - of type <tt>c1(A)</tt>;
 * <li> the iterated function applied to each element - of type <tt>A -&gt; c2(B)</tt>; and,
 * <li> the operation "adding" an element to a collection - of type <tt>B,c2(B) -&gt; c2(B)</tt>.
 * </ul>
 *
 * A  <i>primitive homomorphism</i> computing a value of type <tt>B</tt> consists of:
 * <ul>
 * <li> the collection iterated over - of type <tt>c1(A)</tt>;
 * <li> the iterated function applied to each element - of type <tt>A -&gt; B</tt>; and,
 * <li> the monoid operation - of type <tt>B,B -&gt; B</tt>.
 * </ul>
 *
 * <p>
 *
 * Even though the scheme of computation for homomorphisms described above is
 * correct, it is not often used, especially when the function already encapsulates
 * the accumulation operation, as is always the case when the homomorphism comes
 * from the desugaring of a <i>comprehension</i> - see below). Then, such a
 * homomorphism will directly side-effect the structure specified as the identity
 * element with a function of the form <tt>&lambda;x.op(x,id)</tt> and dispense
 * altogether with the need to accumulate intermediate results. We shall call those
 * homomorphisms <i>in-place</i> homomorphisms. To distinguish them and enable the
 * suprression of intermediate computations, a flag indicating that the
 * homomorphism is to be computed in-place is provided.  Both primitive and
 * collection homomorphisms can be specified to be in-place. If nothing regarding
 * in-place computation is specified for a homomorphism, the default behavior will
 * depend on whether the homomorphism is collection (default is in-place), or
 * primitive (default is <i>not</i> in-place). Methods to override the defaults
 * are provided.
 *
 * <p>
 *
 * For an in-place homomorphism, the iterated function encapsulates the operation,
 * which affects the identity element, which thus accumulates intermediate results
 * and no further composition using the operation is needed.  This is especially
 * handy for collections that are often represented, for (space and time)
 * efficiency reasons, by iteratable bulk structures constructed by allocating an
 * empty structure that is filled in-place with elements using a built-in
 * <i>"add"</i> method guaranteeing that the resulting data structure is canonical
 * - <i>i.e.</i>, that it abides by the algebraic properties of its type of
 * collection (<i>e.g.</i>, adding an element to a set will not create duplicates,
 * <i>etc.</i>).
 *
 * <p>
 *
 * Although monoid homomorphisms are defined as expressions in the kernel, they are
 * not meant to be represented directly in a surface syntax (although they could,
 * but would lead to rather cumbersome and not very legible expressions). Rather,
 * they are meant to be used for expressing higher-level expressions known as <a
 * href="Comprehension.html"><i>monoid comprehensions</i></a>, which offer the
 * advantage of the familar (set) comprehension notation used in mathematics, and
 * can be translated into monoid homomorphisms to be type-checked and
 * evaluated.
 *
 * <p>
 *
 * A monoid comprehension is an expression of the form:
 * <pre>
 * <span style="color:brown">
 * [op,id] { e | q<sub>1</sub>, ..., q<sub>n</sub> }
 * </span>
 * </pre>
 * where <tt>[op,id]</tt> define a monoid, <tt>e</tt> is an expression, and the
 * <tt>q<sub>i</sub></tt>'s are <i>qualifiers</i>. A qualifier is either a
 * <i>boolean</i> expression or a pair <tt>x &lt;- e</tt>, where <tt>x</tt> is
 * a variable and <tt>e</tt> is an expression. The sequence of qualifiers may
 * also be empty. Such a monoid comprehension is just syntactic sugar that
 * can be expressed in terms of homomorphisms as follows:
 * <p>
 * <pre>
 * <span style="color:brown">
 * [op,id]{e | } = op(e,id);
 *
 * [op,id]{e | x &lt;- e', Q} = <b>hom</b>(e', &lambda;x.[op,id]{e | Q}, op, id);
 *
 * [op,id]{e | c, Q} = <b>if</b> c <b>then</b> [op,id]{e | Q} <b>else</b> id;
 * </span>
 * </pre>
 * 
 * <p>
 *
 * Thus, monoid comprehensions allow the formulation of "declarative iteration."
 * Note the fact mentioned earlier that a homomorphism coming from the translation
 * of a comprehension encapsulates the operation in its function. Thus, this is
 * generally taken to advantage with operations that cause a side-effect on their
 * second argument to enable an in-place homomorphism to dispense with unneeded
 * intermediate computation.
 *
 * <p>
 *
 * Comprehensions are also interesting as they may be subject to transformations
 * leading to more efficient evaluation than their simple "nested loops"
 * operational semantics (by using "unnesting" techniques and using relational
 * operations as implementation instructions). At any rate, homomorphisms are
 * here treated "naively" and compiled as simple loops.
 */

public class Homomorphism extends ProtoExpression 
{
  /**
   * This is the collection over which the iteration is performed.
   */
  protected Expression _collection;

  /**
   * This is the function applied to each element of the collection.
   */
  protected Expression _function;

  /**
   * This is the accumulation operation of the image monoid.
   */
  protected Expression _operation;

  /**
   * This is the identity element of the image monoid.
   */
  protected Expression _identity;

  /**
   * This is the type of the elements of the collection. Handy as a protected
   * attribute when typechecking a <a href="FilteredHomomorphism.html">filtered homomorphism</a>
   *
   */
  protected Type _elementType;

  /**
   * This contains slicing expressions if any. Each is of the form <tt>x.s == e</tt>, where
   * <tt>x</tt> is the generator parameter, and <tt>x</tt> does not occur free in <tt>e</tt>.
   */
  protected Expression[] _slicings;

  /**
   * This flag indicates whteher this is an <i>in-place</i> homomorphism.
   */
  protected byte _inPlace = DEFAULT_IN_PLACE;

  public static final byte DEFAULT_IN_PLACE  = 0;
  public static final byte ENABLED_IN_PLACE  = 1;
  public static final byte DISABLED_IN_PLACE = 2;

  public Homomorphism (Expression collection, Expression function,
                       Expression operation, Expression identity)
    {
      _collection = collection;
      _function   = function;
      _operation  = operation;
      _identity   = identity;
    }

  public Homomorphism (Expression collection, Expression function,
                       Expression operation, Expression identity, boolean inPlace)
    {
      this(collection,function,operation,identity);
      if (inPlace)
        enableInPlace();
      else
        disableInPlace();
    }

  private Homomorphism (Expression collection, Expression function,
                       Expression operation, Expression identity, byte inPlace)
    {
      this(collection,function,operation,identity);
      _inPlace = inPlace;
    }

  public Expression copy ()
    {
      Homomorphism copy = new Homomorphism(_collection.copy(),
                                           _function.copy(),
                                           _operation.copy(),
                                           _identity.copy(),
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

  public Expression typedCopy ()
    {
      Homomorphism copy = new Homomorphism(_collection.typedCopy(),
                                           _function.typedCopy(),
                                           _operation.typedCopy(),
                                           _identity.typedCopy(),
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
      return 4 + (_slicings == null ? 0 : _slicings.length);
    }

  /**
   * Returns the n-th subexpression of this expression; if there is no subexpression
   * at the given position, this throws a <tt>NoSuchSubexpressionException</tt>
   */
  public Expression subexpression (int n) throws NoSuchSubexpressionException
    {
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
        default:
          int s = n-4;
          if (_slicings != null && s >= 0 && s < _slicings.length)
            return _slicings[s];
          throw new NoSuchSubexpressionException(this,n);
        }
    }

  public Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
    {
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
        default:
          int s = n-4;
          if (_slicings != null && s >= 0 && s < _slicings.length)
            _slicings[s] = expression;
          else
            throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  public final void setSlicings (ArrayList slicings)
    {
      _slicings = new Expression[slicings.size()];
      for (int i = _slicings.length; i-->0;)
        _slicings[i] = (Expression)slicings.get(i);
    }

  public final void setSlicings (Expression[] slicings)
    {
      _slicings = slicings;
    }

  public final Homomorphism enableInPlace ()
    {
      _inPlace = ENABLED_IN_PLACE;
      return this;
    }      

  public final Homomorphism disableInPlace ()
    {
      _inPlace = DISABLED_IN_PLACE;
      return this;
    }      

  public void setCheckedType ()    
    {
      if (setCheckedTypeLocked()) return;
      _collection.setCheckedType();
      _function.setCheckedType();
      _operation.setCheckedType();
      _identity.setCheckedType();
      if (_slicings != null)
        for (int i = _slicings.length; i-->0;)
          _slicings[i].setCheckedType();
      setCheckedType(type().copy());
    }

  /**
   * <a name="typeChecking"></a>
   * Checking the type of a homomorphism depends on whether the homomorphism is...
   * <pre>
   * <span style="color:brown; font-size:smaller">
   *    <b>Primitive:</b>                     OR     <b>Collection:</b>
   *
   * _collection : from_collection(A)       _collection : from_collection(A)
   *   _function : A -&gt; B                     _function : A -&gt; to_collection(B)
   *  _operation : B,B -&gt; B                  _operation : B, to_collection(B) -&gt; to_collection(B)
   *  _identity  : B                          _identity : to_collection(B)
   *        this : B                               this : to_collection(B)
   * </span>
   * </pre>
   *
   * <p>
   * Also, if slicing expressions are present, they must be typed as booleans.
   * <p>
   *
   * <b>NB:</b> In order to accommodate primitive homomorphisms whose monoid operation
   * is not strictly-speaking internal but may involve subtyping, we use a work-around that
   * consists of unifying the <a href="../types/Type.html#shadowType"><i>shadow types</i></a>
   * of the operation's arguments rather than the types themselves. The changes from the
   * lines needed for typing such "loose" monoids are marked explicitly and the original lines
   * they replace are commented out. Unifying shadow types is done using a special goal:
   * a <a href="../types/ShadowUnifyGoal.html"><tt>ShadowUnifyGoal</tt></a>.
   */
  public void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _elementType = new TypeParameter();       // type A in comment above
      Type imageType = new TypeParameter();     // type B in comment above

      _collection.typeCheck(Global.dummyCollection(),typeChecker);

      _identity.typeCheck(_type,typeChecker);

      Type shadowType = new TypeParameter();    // shadow of _type        /* workaround [1] */

      typeChecker.prove(new BaseTypeGoal(_collection,_elementType));
      typeChecker.prove(new ShadowUnifyGoal(shadowType,_type));           /* workaround [1] */

      //FunctionType opType = new FunctionType(imageType,_type,_type);    /* original   [1] */
      FunctionType opType = new FunctionType(imageType,shadowType,_type); /* workaround [1] */
      _operation.typeCheck(opType,typeChecker);

      FunctionType funType = new FunctionType(_elementType,_type);
      _function.typeCheck(funType,typeChecker);

      if (type().rank() == imageType.value().rank())
        // do this image type check only for a primitive homomorphism
        //typeChecker.typeCheck(new UnifyGoal(_type,imageType));          /* original   [2] */
        typeChecker.prove(new ShadowUnifyGoal(_type,imageType));          /* workaround [2] */
      else
        // do this base type check only for a collection homomorphism
        typeChecker.prove(new BaseTypeGoal(this,imageType));

      if (_slicings != null)
        for (int i = _slicings.length; i-->0;)
          _slicings[i].typeCheck(Type.BOOLEAN(),typeChecker);
    }

  /**
   * Return <tt>true</tt> iff this is a collection homomorphism - <i>i.e.</i>, iff the
   * operation is not strictly speaking a monoid operation (<i>i.e.</i>, taking two
   * arguments of a type and return a result of same type), but a collection-add
   * operation (whose first argument type is the base type of the collection type
   * of the second argument and the result). Thus, to determine that this is a
   * collection homomorphism, it is sufficient to check that the second domain
   * type's rank is equal to the first domain type's rank plus one.
   */
  protected final boolean _isCollection ()
    {
      FunctionType otype = (FunctionType)_operation.checkedType();
      return otype.domain(1).rank() == 1 + otype.domain(0).rank();      
    }

  /**
   * Return <tt>true</tt> iff this is an in-place homomorphism.
   */
  protected final boolean _isInPlace ()
    {
      if (_inPlace == DEFAULT_IN_PLACE)
        return _isCollection();

      return _inPlace == ENABLED_IN_PLACE;
    }

  /**
   * This fixes the boxing of the monoid operator and identity by systematically unboxing
   * all occurrences of the collection element type. This is a necessary hack [:( sigh!]
   * because collection-building built-in dummy instructions like <tt>SET_ADD</tt> have a
   * needlessly polymorphic type that becomes instantiated only when it is applied. However,
   * a homomorphism construct compiles the operation as a non-applied closure. This fix
   * will avoid boxing the types corresponding to the collection's elements.
   */
  protected void _fixTypeBoxing ()
    {
      Type itype = _identity.checkedType();
      FunctionType otype = (FunctionType)_operation.checkedType();
      FunctionType ftype = (FunctionType)_function.checkedType();

      if (itype.kind() == Type.BOXABLE)
        ((BoxableTypeConstant)itype).setBoxed(false);

      if (otype.domain(0).kind() == Type.BOXABLE)
        {
          ((BoxableTypeConstant)otype.domain(0)).setBoxed(false);
          otype.unsetDomainBox(0);

          if (otype.domain(0).isEqualTo(otype.domain(1)))   // primitive homomorphism
            {
              ((BoxableTypeConstant)otype.domain(1)).setBoxed(false);
              otype.unsetDomainBox(1);

              ((BoxableTypeConstant)otype.range()).setBoxed(false);
              otype.unsetRangeBox();
            }
        }

      if (ftype.domain(0).kind() == Type.BOXABLE)
        {
          ((BoxableTypeConstant)ftype.domain(0)).setBoxed(false);
          ftype.unsetDomainBox(0);
        }
    }

  public void compile (Compiler compiler)
    {
      int[][] slices = null;
      _fixTypeBoxing();

      _identity.compile(compiler);
      if (!_isInPlace()) _operation.compile(compiler);
      _function.compile(compiler);
      if (_slicings != null)
        slices = _compileSlicings(compiler);
      _collection.compile(compiler);

      if (_isInPlace())
        switch (((Collection)_collection.checkedType()).baseType().sort())
          {
          case Type.INT_SORT:
            compiler.generate(Instruction.APPLY_IP_HOM_I);              
            break;
          case Type.REAL_SORT:
            compiler.generate(Instruction.APPLY_IP_HOM_R);              
            break;
          case Type.OBJECT_SORT:
            if (_slicings == null)
              compiler.generate(Instruction.APPLY_IP_HOM_O);                
            else
              compiler.generate(new ApplySlicedInPlaceObjectHomomorphism(slices));                
          }
      else
        switch (((Collection)_collection.checkedType()).baseType().sort())
          {
          case Type.INT_SORT:
            if (_isCollection())
              compiler.generate(new ApplyIntCollectionHomomorphism(_tally()));
            else
              compiler.generate(Instruction.APPLY_HOM_I);
            break;
          case Type.REAL_SORT:
            if (_isCollection())
              compiler.generate(new ApplyRealCollectionHomomorphism(_tally()));
            else
              compiler.generate(Instruction.APPLY_HOM_R);
            break;
          case Type.OBJECT_SORT:
            if (_slicings == null)
              if (_isCollection())
                compiler.generate(new ApplyObjectCollectionHomomorphism(_tally()));
              else
                compiler.generate(Instruction.APPLY_HOM_O);
            else
              if (_isCollection())
                compiler.generate(new ApplySlicedObjectCollectionHomomorphism(slices,_tally()));
              else
                compiler.generate(new ApplySlicedObjectHomomorphism(slices));
          }
    }

  protected final int[][] _compileSlicings (Compiler compiler)
    {
      int[][] slices = new int[_slicings.length][];
      for (int i = _slicings.length; i-->0;)
        slices[i] = _compileSlicing(_slicings[i],compiler);
      return slices;
    }

  private static final int[] _compileSlicing (Expression slicing, Compiler compiler)
    {
      TupleProjection projection = (TupleProjection)((Application)slicing).argument(0);
      int depth = projection.depth();
      int[] slice = new int[depth+1];           // one more for the sort
      slice[depth] = projection.boxSort();      // store the sort in the last slot

      for (int i = depth; i-->0;)
        {
          slice[i] = projection.offset();
          if (i > 0)
            projection = (TupleProjection)projection.tuple();
        }

      Expression slicer = ((Application)slicing).argument(1);
      slicer.compile(compiler);
      if (!slicer.checkedType().isBoxedType())  // systematically box the slicer
        compiler.generateWrapper(slicer.sort());

      return slice;
    }

  protected final Instruction _tally ()
    {
      switch (((FunctionType)_operation.checkedType()).domain(0).sort())
        {
        case Type.INT_SORT:
          return Instruction.APPLY_COLL_I;
        case Type.REAL_SORT:
          return Instruction.APPLY_COLL_R;
        }

      return Instruction.APPLY_COLL_O;
    }

  public String toString ()
    {
      StringBuilder buf = new StringBuilder("hom(");

      buf.append(_collection).append(',')
         .append(_function).append(',')
         .append(_operation).append(',')
         .append(_identity);
      if (_slicings != null)
        buf.append(',').append(Misc.arrayToString(_slicings));
      buf.append(')');

      return buf.toString();
    }

  protected final String _inPlace ()
    {
      switch (_inPlace)
        {
        case ENABLED_IN_PLACE:
          return "enabled in place";
        case DISABLED_IN_PLACE:
          return "disabled in place";
        }

      return "default: "+(_isInPlace()?"in ":"not in ")+"place";
    }

}
