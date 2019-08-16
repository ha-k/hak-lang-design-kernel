//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Thu Mar 24 12:06:48 2016 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

import hlt.language.util.Locatable;
import hlt.language.util.Location;
import hlt.language.util.Span;
import hlt.language.tools.Misc;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is the mother of all expressions in the kernel language.
 * It specifies the prototypes of the methods that must be implemented
 * by all expression subclasses. The subclasses of <tt>Expression</tt>
 * are:
 * <p>
 *
 * <ul>
 *
 * <li><a href="Constant.html"><tt>Constant</tt></a>:
 *     constant (void, boolean, <a href="Int.html"><tt>integer</tt></a>,
 *     <a href="Real.html"><tt>real number</tt></a>,
 *     <a href="BuiltinObjectConstant.html"><tt>object</tt></a>);
 * <p>
 * <li><a href="Abstraction.html"><tt>Abstraction</tt></a>:
 *     functional abstraction (<i>&agrave; la</i> lambda-calculus);
 * <p>
 * <li><a href="Application.html"><tt>Application</tt></a>:
 *     functional application;
 * <p>
 * <li><a href="Local.html"><tt>Local</tt></a>:
 *     local name;
 * <p>
 * <li><a href="Global.html"><tt>Global</tt></a>:
 *     global name;
 * <p>
 * <li><a href="IfThenElse.html"><tt>IfThenElse</tt></a>:
 *     conditional;
 * <p>
 * <li><a href="AndOr.html"><tt>AndOr</tt></a>:
 *     non-strict boolean conjunction and disjunction;
 * <p>
 * <li><a href="Sequence.html"><tt>Sequence</tt></a>:
 *     sequence of expressions (presumably with side-effects);
 * <p>
 * <li><a href="Let.html"><tt>Let</tt></a>:
 *     lexical scoping construct;
 * <p>
 * <li><a href="Loop.html"><tt>Loop</tt></a>:
 *     conditional iteration construct;
 * <p>
 * <li><a href="Definition.html"><tt>Definition</tt></a>:
 *     definition of a global name with an expression defining it
 *     in a global store;
 * <p>
 * <li><a href="Parameter.html"><tt>Parameter</tt></a>:
 *     a function's formal parameter (really a pseudo-expression
 *     as it is not fully processed as a real expression and is used
 *     as a shared type information repository for all occurrences
 *     in a function's body of the variable it stands for);
 * <p>
 * <li><a href="Assignment.html"><tt>Assignment</tt></a>:
 *     construct to set the value of a <a href="LocalAssignment.html"><tt>local</tt></a>
 *     or a <a href="GlobalAssignment.html"><tt>global</tt></a> variable;
 * <p>
 * <li><a href="NewObject.html"><tt>NewObject</tt></a>:
 *     construct to create a new object;
 * <p>
 * <li><a href="FieldUpdate.html"><tt>FieldUpdate</tt></a>:
 *     construct to update the value of an object's field;
 * <p>
 * <li><a href="NewArray.html"><tt>NewArray</tt></a>:
 *     construct to create a new (multidimensional) array;
 * <p>
 * <li><a href="ArraySlot.html"><tt>ArraySlot</tt></a>:
 *     construct to access the element of an array;
 * <p>
 * <li><a href="ArraySlotUpdate.html"><tt>ArraySlotUpdate</tt></a>:
 *     construct to update the element of an array;
 * <p>
 * <li><a href="Tuple.html"><tt>Tuple</tt></a>:
 *     construct to create a new position-indexed tuple;
 * <p>
 * <li><a href="NamedTuple.html"><tt>NamedTuple</tt></a>:
 *     construct to create a new name-indexed tuple;
 * <p>
 * <li><a href="TupleProjection.html"><tt>TupleProjection</tt></a>:
 *     construct to access the component of a tuple;
 * <p>
 * <li><a href="TupleUpdate.html"><tt>TupleUpdate</tt></a>:
 *     construct to update the component of a tuple;
 * <p>
 * <li><a href="Dummy.html"><tt>Dummy</tt></a>:
 *     temporary place holder in lieu of a name prior to being discriminated
 *     into a local or global one.
 * </ul>
 * Typically, upon being read, an <tt>Expression</tt> will be:
 * <p>
 *
 * <ol>
 *
 * <li><i>"name-sanitized"</i> - in the context of a <a
 * href="Sanitizer.html"><tt>Sanitizer</tt></a> to discriminate
 * between local names and global names, and establish pointers from the
 * local variable occurrences to the abstraction that introduces them,
 * and from global names to entries in the global symbol table;
 *
 * <p>
 * <li><i>type-checked</i> - in the context of a <a
 * href="../types/TypeChecker.html"><tt>TypeChecker</tt></a> to discover
 * whether it has a type at all, or several possible ones (only
 * expressions that have a unique unambiguous type are further
 * processed);
 *
 * <p>
 * <li> <i>"sort-sanitized</i>" - in the context of a <a
 * href="Sanitizer.html"><tt>Sanitizer</tt></a> to discriminate
 * between those local variables that are of primitive Java types
 * (<tt>int</tt> or <tt>double</tt>) or of <tt>Object</tt> type (this is
 * necessary because the set-up means to use unboxed values for primitive
 * types for efficiency reasons); this second "sanitization" phase is
 * also used to compute offsets for local names (<i>i.e.</i>, so-called
 * <i>de Bruijn indices</i>) for each type sort;
 *
 * <p>
 * <li><i>compiled</i> - in the context of a <a href="Compiler.html">
 * <tt>Compiler</tt></a> to generate the sequence of instructions whose
 * execution in an appropriate runtime environment will evaluate the expression;
 *
 * <p>
 * <li><i>executed</i> - in the context of a <a href="../backend/Runtime.html">
 * <tt>Runtime</tt></a> to execute its sequence of instructions.
 *
 * </ol>
 * */
public abstract class Expression implements Locatable
{
  /**
   * When this is <tt>true</tt>, all assignments have a <tt>void</tt> type (and return
   * no value); otherwise, they have the same type as the assigned value (which is also
   * the value they return). This is useful for, <i>e.g.</i>, overloading the equal
   * sign '<tt>=</tt>' to mean something else besign assignment depending on the type.
   * The kernel expressions that are assigments are:
   * <ul>
   * <li> <a href="LocalAssignment.html"><tt>LocalAssignment</tt></a>,
   * <li> <a href="GlobalAssignment.html"><tt>GlobalAssignment</tt></a>,
   * <li> <a href="ArraySlotUpdate.html"><tt>ArraySlotUpdate</tt></a>,
   * <li> <a href="FieldUpdate.html"><tt>FieldUpdate</tt></a>,
   * <li> <a href="TupleUpdate.html"><tt>TupleUpdate</tt></a>.
   * </ul>
   * Each of these will use this flag to alter they type-cehcking and compiling rules
   * accordingly.
   */
  public static boolean VOID_ASSIGNMENTS = false;

  /**
   * Returns a deep copy of this expression. The types are not not copied.
   * <b>NB:</b> This is a convenience for building expressions that should be used
   * exclusively before any processing (in particular sanitizing), is done on this
   * expression.
   */
  public abstract Expression copy ();

  /**
   * Returns a deep copy of this expression this and all its subexpressions share the
   * same type as their copy's counterparts.  <b>NB:</b> This is a convenience for
   * building expressions that should be used exclusively before any processing (in
   * particular sanitizing), is done on this expression.
   */
  public abstract Expression typedCopy ();

  /**
   * Prevents type-checking this expression more than once.
   */
  protected boolean _typeCheckLocked = false;
  public final boolean typeCheckLocked ()
    {
      if (!_typeCheckLocked)
        {
          _typeCheckLocked = true;
          return false;
        }

      return true;
    }

  /**
   * Prevents setting the checked type for this expression more than once.
   */
  protected boolean _setCheckedTypeLocked = false;
  public final boolean setCheckedTypeLocked ()
    {
      if (!_setCheckedTypeLocked)
        {
          _setCheckedTypeLocked = true;
          return false;
        }

      return true;
    }

  /**
   * Returns <tt>true</tt> iff this expression is a slicing filter for the specified
   * parameter.
   */
  public boolean isSlicing (Tables tables, Parameter parameter)
    {
      return false;
    }

  /**
   * Returns <tt>true</tt> iff this expression is a "hidden" slicing filter for the
   * specified parameter; <i>i.e.</i>, one that uses accessor projections rather than
   * explicit tuple projections.
   */
  public boolean isHiddenSlicing (Tables tables, Parameter parameter)
    {
      return false;
    }

  /**
   * Returns <tt>true</tt> iff this expression is a selector filter for the specified
   * parameter.
   */
  public boolean isSelector (Tables tables, Parameter parameter)
    {
      return false;
    }

  public final boolean isEquality (Tables tables) throws UndefinedEqualityException
    {
      if (!(this instanceof Dummy || this instanceof Global))
        return false;

      return tables.isEquality(this instanceof Dummy
                               ? ((Dummy)this).name()
                               : ((Global)this).name());
    }

  /**
   * This contains any other types that may be redudantly ascribed to this
   * expression prior to being type-checked. For example, a grammar constructing
   * an expression from a concrete syntax may allow several types to be attached
   * a single expression. The type-checker will then ensure that all these types
   * are compatible, and can be reduced to a single consistent one.
   */
  protected HashSet _otherTypes;

  /**
   * Returns <tt>true</tt> iff this expression is a
   * <a href="Constant.html"><tt>Constant</tt></a>.
   */
  public final boolean isConstant ()
    {
      return this instanceof Constant;
    }

  /**
   * Returns <tt>true</tt> iff this expression denotes the constant <tt>void</tt>.
   */
  public final boolean isVoid ()
    {
      return this == Constant.VOID;
    }

  /**
   * Returns <tt>true</tt> iff this expression denotes the constant <tt>true</tt>.
   */
  public boolean isTrue ()
    {
      return isConstant() && ((Constant)this).isTrue();
    }

  /**
   * Returns <tt>true</tt> iff this expression denotes the constant <tt>false</tt>.
   */
  public boolean isFalse ()
    {
      return isConstant() && ((Constant)this).isFalse();
    }

  /**
   * Returns <tt>true</tt> iff this expression denotes the constant <tt>null</tt>.
   */
  public boolean isNull ()
    {
      return isConstant() && ((Constant)this).isNull();
    }

  /**
   * This method returns the current type of this expression. It will typically, but not
   * necessarily, be the dereferenced value of a <a href="TypeParameter.html">
   * <tt>TypeParameter</tt></a> field.  A notable exception is a <a href="Local.html">
   * <tt>Local</tt></a> since its type method will always return the type of the <a
   * href="Parameter.html"> <tt>Parameter</tt></a> it stands for.
   */
  public abstract Type type ();

  /**
   * This method forcibly sets this expression's type field, if it has one, to the
   * specified type. It will be an empty method otherwise.
   */
  public abstract void setType (Type type);

  /**
   * This method returns this expression's undereferenced type field if it has one. It
   * will return <tt>null</tt> otherwise.
   */
  public abstract Type typeRef ();

  /**
   * This method returns the final unambiguously checked type of this expression.
   */
  public abstract Type checkedType ();

  /**
   * This method sets the final unambiguously checked type of this expression, possibly
   * along with other final information that may be needed by the compiler for
   * generating code.
   */
  public abstract void setCheckedType ();

  /**
   * This method sets the final unambiguously checked type of this expression to the
   * specified type. This is used when expression are synthesized on the fly after the
   * type-checking phase and prior to the compiling phase.
   */
  public abstract void setCheckedType (Type type);

  /**
   * Returns the expression resulting from substituting all the free occurrences
   * of the parameters's names as specified by the given substitution.
   */
  public Expression substitute (HashMap substitution)
    {
      if (!substitution.isEmpty())
        for (int i=numberOfSubexpressions(); i-->0;)
          setSubexpression(i,subexpression(i).substitute(substitution));

      return this;
    }

  /**
   * Returns this expression enclosing scope (if it has been computed by <tt>linkScopeTree</tt>);
   * <tt>null</tt> otherwise.
   */
  public Expression enclosingScope ()
    {
      return null;
    }

  /**
   * The number of comprehensions nested inside this expression.
   */
  protected int _nestedComprehensionCount;
  
  /**
   * Returns the number of comprehensions nested inside this expression.
   */
  final int nestedComprehensionCount ()
    {
      return _nestedComprehensionCount;
    }

  protected boolean _scopeTreeIsLinked;

  /**
   * Visits all subexpressions to link up the scope tree of nested comprehensions.
   * It also sets and returns the nummber of such nested comprehensions. This is
   * invoked in <a href="Sanitizer.html"><tt>Sanitizer</tt></a> before sanitizing
   * an outermost expression.
   */
  int linkScopeTree (Expression ancestor)
    {
      if (_scopeTreeIsLinked)
        return _nestedComprehensionCount;
        
      for (int i=numberOfSubexpressions(); i-->0;)
        _nestedComprehensionCount += subexpression(i).linkScopeTree(ancestor);

      _scopeTreeIsLinked = true;

      return _nestedComprehensionCount;
    }

  /**
   * Visits all subexpressions to desugar the patterens of comprehensions if any are nested.
   */
  void desugarPatterns ()
    {
      if (_nestedComprehensionCount > 0)
        for (int i=numberOfSubexpressions(); i-->0;)
          subexpression(i).desugarPatterns();
    }

  /**
   * If there are nested comprehensions, this visits all subexpressions to unnest
   * comprehension filters.
   */
  void unnestInnerFilters ()
    {
      if (_nestedComprehensionCount > 0)
        for (int i=numberOfSubexpressions(); i-->0;)
          subexpression(i).unnestInnerFilters();
    }

  /**
   * Returns <tt>true</tt> iff the specified name occurs free in this expression.
   */
  public boolean containsFreeName (String name)
    {
      for (int i=numberOfSubexpressions(); i-->0;)
        if (subexpression(i).containsFreeName(name))
          return true;

      return false;
    }

  /**
   * This method returns the actual expression corresponding to this one after
   * eliminating all the <a href="Dummy.html"><tt>Dummy</tt></a> expressions that occur
   * in it where names where read. It takes as an argument the stack of parameters it
   * encounters along the way of visiting all its components. A <a href="ParameterStack.html">
   * <tt>ParameterStack</tt></a> is simply a <tt>Stack</tt> of <a href="Parameter.html">
   * <tt>Parameter</tt></a> objects along with a method to retrieve the latest one with
   * a given name.
   */
  public Expression sanitizeNames (ParameterStack parameters)
    {
      for (int i=numberOfSubexpressions(); i-->0;)
        setSubexpression(i,subexpression(i).sanitizeNames(parameters));
      return this;
    }

  /**
   * This method walks down the expression gathering information relating each <a
   * href="Local.html"><tt>Local</tt></a> object to the <a href="Scope.html">
   * <tt>Scope</tt></a> containing the corresponding <a href="Parameter.html">
   * <tt>Parameter</tt></a> in its array of formal parameters. It takes as an argument
   * an <a href="Enclosure.html"><tt>Enclosure</tt></a>, which is a <tt>Stack</tt>
   * containing all <a href="Scope.html"><tt>Scope</tt></a>s encountered
   * along the way, equipped with a method to compute all the information necessary in
   * computing the local's offset.
   */
  public void sanitizeSorts (Enclosure enclosure)
    {
      for (int i=numberOfSubexpressions(); i-->0;)
        subexpression(i).sanitizeSorts(enclosure);
    }

  /**
   * This methods visits this expression shifting the offset of all the <a
   * href="Local.html"><tt>Local</tt></a>s it encounters by the specified amount (per
   * sort) as long as the offset it sees is referencing a parameter within the specified
   * scope depth (per sort); This method is used when the compiler needs to
   * synthesize an scope from an already typechecked expression (<i>i.e.</i>, for
   * currying or un/boxing - <i>e.g.</i>, see the <tt>pad</tt> method below).
   */
  public Expression shiftOffsets (int intShift, int realShift, int objectShift,
                                  int intDepth, int realDepth, int objectDepth)

    {
      for (int i=numberOfSubexpressions(); i-->0;)
        setSubexpression(i,subexpression(i).shiftOffsets(intShift,realShift,objectShift,
                                                         intDepth,realDepth,objectDepth));
      return this;
    }

  public final Expression shiftOffsets (int intShift, int realShift, int objectShift)
    {
      return shiftOffsets(intShift,realShift,objectShift,0,0,0);
    }

  /**
   * This method type-checks this expression in the context of the specified
    *<a href="../types/TypeChecker.html"> <tt>TypeChecker</tt></a>.
   */
  public abstract void typeCheck (TypeChecker typeChecker) throws TypingErrorException;

  /**
   * This method ascertains that this expression has the specified type in
   * the context of the specified <a href="../types/TypeChecker.html">
   * <tt>TypeChecker</tt></a>. It amounts to first type-checking this
   * expression in the type-checker, then making sure the type found for it
   * by the type-checker agrees (<i>i.e.</i>unifies) with the specified type.
   */
  public void typeCheck (Type type, TypeChecker typeChecker) throws TypingErrorException
    {
      typeChecker.unify(typeRef(),type,this);
      typeCheck(typeChecker);
    }
    
  /**
   * This method type-checks this expression in the context of the specified typechecker,
   * using the type of the specified <a href="Global.html"><tt>Global</tt></a> as a filter.
   */
  public final void typeCheck (Global filter, TypeChecker typeChecker) throws TypingErrorException
    {
      typeCheck(typeChecker);
      typeChecker.prune(filter,typeRef(),this);
      filter.typeCheck(typeRef(),typeChecker);
    }
    
  /**
   * This method compiles this expression in the context of the specified
   * <a href="Compiler.html"><tt>Compiler</tt></a>.
   */
  public abstract void compile (Compiler compiler);

  /**
   * Returns the number of subexpressions
   */
  public int numberOfSubexpressions ()
    {
      return 0;
    }

  /**
   * Returns the n-th subexpression of this expression; if there is no subexpression
   * at the given position, this throws a <tt>NoSuchSubexpressionException</tt>
   */
  public Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      throw new NoSuchSubexpressionException(this,n);
    }

  /**
   * Returns this expression after setting its n-th subexpression to the specified
   * expression; if there is no subexpression at the given position, this throws a
   * <tt>NoSuchSubexpressionException</tt>
   */
  public Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
    {
      return this;
    }

  /**
   * This is only to enable uniform visitors on expressions. It will return non-null
   * only for scopes.
   */
  public Parameter[] parameters ()
    {
      return null;
    }

  /**
   * Returns the sort of this expression's type after it has been
   * checked.  Sorts are of three kinds and defined as global constants
   * in <a href="../types/Type.html"><tt>Type</tt></a>: <tt>Type.INT_SORT</tt>,
   * <tt>Type.REAL_SORT</tt>, and <tt>Type.OBJECT_SORT</tt>.
   */
   public final byte sort ()
    {
      return checkedType().sort();
    }

  /**
   * This returns the runtime sort for this expression unless its type
   * has been marked as boxed - in which case it returns <tt>OBJECT_SORT</tt>.
   */
  public /* final */ byte boxSort ()
    {
      return checkedType().boxSort();
    }

  /**
   * This adds the specified type as another type for this expression. <b>NB:</b>
   * This method must <i>never</i> be used on an expression, or with a type, that
   * has been (even partially) type-checked. This is because, rather than making
   * different type parameters separate types for this expression, it hard-binds
   * them together or to the original type if there is one. It is meant to be used
   * exclusively at expression construction time prior to type-checking anything.
   */
  public final Expression addType (Type newType)
    {
      Type oldType = type();

      if (newType == null)
        return this;

      if (oldType == null)
        setType(newType);
      else
        {
          oldType = oldType.value();
          newType = newType.value();

          if (oldType == newType)
            return this;

          if (oldType.kind() == Type.PARAMETER)
            ((TypeParameter)oldType).bind(newType);
          else
            if (newType.kind() == Type.PARAMETER)
              ((TypeParameter)newType).bind(oldType);
            else
              {
                if (_otherTypes == null)
                  _otherTypes = new HashSet(1);
                _otherTypes.add(newType);
              }
        }

      return this;
    }

  public final HashSet otherTypes ()
    {
      return _otherTypes;
    }

  public final Expression setOtherTypes (HashSet otherTypes)
    {
      _otherTypes = otherTypes;
      return this;
    }

  /**
   * Adds all the types prescribed for the specified expression to this one.
   */
  public final Expression addTypes (Expression expression)
    {
      addType(expression.typeRef());

      HashSet types = expression.otherTypes();
      if (types != null)
        for (Iterator i=types.iterator();i.hasNext();)
          addType((Type)i.next());
      
      return this;
    }

  /**
   * This field may be used to contain this expression's concrete syntax origin.
   */
  private Locatable _extent;

  public final Locatable extent ()
    {
      return _extent;
    }

  public final Expression setExtent (Locatable extent)
    {
      _extent = extent;
      return this;
    }

  /**
   * Returns the start of this expressions's extent as a <a href="../../util/Location.html">
   * <tt>Location</tt></a>.
   */
  public final Location getStart ()
    {
      return _extent == null ? null : _extent.getStart();
    }

  /**
   * Returns the end of this expressions's extent as a <a href="../../util/Location.html">
   * <tt>Location</tt></a>.
   */
  public final Location getEnd ()
    {
      return _extent == null ? null : _extent.getEnd();
    }

  /**
   * Sets the start of this expressions's extent to the specified <a href="../../util/Location.html">
   * <tt>Location</tt></a> and returns this.
   */
  public final Locatable setStart (Location location)
    {
      if (_extent == null) _extent = new Span();
      _extent.setStart(location);
      return this;
    }
    
  /**
   * Sets the end of this expressions's extent to the specified <a href="../../util/Location.html">
   * <tt>Location</tt></a> and returns this.
   */
  public final Locatable setEnd (Location location)
    {
      if (_extent == null) _extent = new Span();
      _extent.setEnd(location);
      return this;
    }

  /**
   * Sets the extent of the specified <a href="../types/StaticSemanticsErrorException.html">
   * <tt>StaticSemanticsErrorException</tt></a> to this expression.
   */
  protected final StaticSemanticsErrorException locate (StaticSemanticsErrorException e)
    {
      return e.setExtent(this);
    }

  /**
   * Returns an explicit string for this expression's extent's location.
   */
  public final String locationString ()
    {
      return Misc.locationString(_extent);
    }

  /**
   * This method analyzes the type boxing information for this expression when it
   * occurs as a <i>functional</i> argument of an application. This returns an
   * expression equivalent to this one <i>"padded"</i> with un/wrappers as needed to
   * reconcile its type boxing information with that of the formal type expected by the
   * argument of the function of which it is ac actual argument, and return the
   * possibly so-modified expression.  It is used by the <tt>compile</tt> method of an
   * <a href="Application.html"> <tt>application</tt></a> that takes this expression as
   * an argument with a functional type. Then, its type information and the type of the
   * function's argument position where it appears (passed as argument to this method)
   * must be used to determine whether to box or unbox its arguments and result. It is
   * defined here as opposed to a concrete subclass of <tt>Expression</tt> because
   * instances of several such classes may have a function type (namely, <a
   * href="Scope.html"> <tt>Scope</tt></a>s, <a href="Local.html"> <tt>Local</tt></a>s,
   * and <a href="Global.html"> <tt>Global</tt></a>s).
   */
  final Expression pad (FunctionType formalType)
    {
      // This expression has necessarily a function type

      FunctionType actualType = (FunctionType)checkedType();
      
      int arity = actualType.arity();
      boolean argumentSortsAgree = true;
      
      for (int i=0; i<arity; i++)
        if (formalType.argumentSortsDisagree(actualType,i))
          {
            argumentSortsAgree = false;
            break;
          }

//        /* start of code for debugging only */
//        hlt.language.tools.Debug.step("Expression = "+this+
//                                       "\nActual type = "+actualType+"\t"+actualType.maskRef()+
//                                       "\nFormal type = "+formalType+"\t"+formalType.maskRef());
//        /* end of code for debugging only */

      if (argumentSortsAgree && !formalType.resultSortsDisagree(actualType))
        return this;

      Application innerPadding;
      Application outerPadding;
      Abstraction abstraction;

      Parameter [] parameters   = new Parameter [arity];
      Local     [] locals       = new Local     [arity];
      Expression[] paddings     = new Expression[arity];

      for (int i=0; i<arity; i++)
        {
          parameters[i] = new Parameter();

          locals[i] = new Local(parameters[i]);
          locals[i].setOffset(arity-1-i);
          
          if (formalType.mustWrapArgument(actualType,i))
            {
              paddings[i] = new Application(formalType.domain(i).wrapper(),locals[i]);

              if (formalType.domain(i).sort() == Type.INT_SORT)
                {
                  parameters[i].setCheckedType(Type.INT());
                  paddings[i].setCheckedType(Type.BOXED_INT());
                }
              else // must have: (formalType.domain(i).sort() == Type.REAL_SORT)
                {
                  parameters[i].setCheckedType(Type.REAL());
                  paddings[i].setCheckedType(Type.BOXED_REAL());
                }                
            }

          else

          if (formalType.mustUnwrapArgument(actualType,i))
            {
              paddings[i] = new Application(formalType.domain(i).unwrapper(),locals[i]);
              if (formalType.domain(i).sort() == Type.INT_SORT)
                {
                  parameters[i].setCheckedType(Type.BOXED_INT());
                  paddings[i].setCheckedType(Type.INT());
                }
              else // must have: (formalType.domain(i).sort() == Type.REAL_SORT)
                {
                  parameters[i].setCheckedType(Type.BOXED_REAL());
                  paddings[i].setCheckedType(Type.REAL());
                }
            }

          else

            {
              parameters[i].setCheckedType(actualType.domain(i));
              paddings[i] = locals[i];
            }
        }

      innerPadding = new Application(this,paddings);

      outerPadding = innerPadding;

      if (formalType.mustWrapResult(actualType))
        {
          outerPadding = new Application(formalType.range().wrapper(),innerPadding);
          if (formalType.range().sort() == Type.INT_SORT)
            {
              innerPadding.setCheckedType(Type.INT());
              outerPadding.setCheckedType(Type.BOXED_INT());
            }
          else // must have: (formalType.range().sort() == Type.REAL_SORT)
            {
              innerPadding.setCheckedType(Type.REAL());
              outerPadding.setCheckedType(Type.BOXED_REAL());
            }
        }

      else

      if (formalType.mustUnwrapResult(actualType))
        {
          outerPadding = new Application(formalType.range().unwrapper(),innerPadding);
          if (formalType.range().sort() == Type.INT_SORT)
            {
              innerPadding.setCheckedType(Type.BOXED_INT());
              outerPadding.setCheckedType(Type.INT());
            }
          else // must have: (formalType.range().sort() == Type.REAL_SORT)
            {
              innerPadding.setCheckedType(Type.BOXED_REAL());
              outerPadding.setCheckedType(Type.REAL());
            }
        }

      else

        innerPadding.setCheckedType(actualType.range());

      abstraction = new Abstraction(parameters,outerPadding);
      abstraction.setNonExitable();
      abstraction.setSortedArities();
      shiftOffsets(abstraction.intArity(),
                   abstraction.realArity(),
                   abstraction.objectArity());

//        /* start of code for debugging only */
//        String s = parameters.length > 1 ? "(" : "";
//        for (int i=0; i<parameters.length; i++)
//          s += parameters[i].checkedType()+(i<parameters.length-1?",":"");
//        s += (parameters.length > 1 ? ")" : "") + " -> " + outerPadding.checkedType();
//        hlt.language.tools.Debug.step(this+" has been padded into: "+abstraction + " : "+s);
//        /* end of code for debugging only */

      return abstraction;
    }

}
