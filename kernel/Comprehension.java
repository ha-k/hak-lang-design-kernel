//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Thu Mar 24 12:01:52 2016 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

import hlt.language.tools.Debug;

import hlt.language.util.ArrayList;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class represents a monoid comprehension whose actual form is interpreted as a
 * construct involving the parts of the syntactic form of the comprehension. The syntax
 * of a monoid comprehension is given by an expression of the form:
 *
 * <pre>
 * <span style="color:brown">
 * [op,id] { e | q<sub>1</sub>, ..., q<sub>n</sub> }
 * </span>
 * </pre>
 *
 * where <tt>[op,id]</tt> define a monoid, <tt>e</tt> is an expression, and the
 * <tt>q<sub>i</sub></tt>s are <i>qualifiers</i>. A qualifier is either a <i>boolean</i>
 * expression or a pair <tt>p &lt;- e</tt>, where <tt>p</tt> is a pattern (any expression)
 * and <tt>e</tt> is an expression. The sequence of qualifiers may also be empty. Such a
 * monoid comprehension is syntactic sugar that is in fact translated into a combination
 * of homomorphisms and/or filtering tests, possibly wrapped inside a let factoring out
 * some computation.
 */

public class Comprehension extends ProtoExpression
{
  public static boolean OPAQUE_PARAMETERS = true;

  protected Tables _tables;
  protected RawInfo _raw;
  protected Expression _construct;
  protected Expression _operation;
  protected Expression _identity;

  protected Expression _enclosingScope;

  /**
   * Constructs an already translated comprehension as a let. This is provided
   * as a public constructor but should be used with care as it trusts that the
   * specified arguments are correctly set up.
   */
  public Comprehension (AbstractList parameters, AbstractList values, Expression body)
    {
      _construct = new Let(parameters,values,body);
    }

  /**
   * Constructs a <i>raw</i> comprehension with the specifed arguments and assuming
   * the default in-place mode for performing the monoid operation.
   */
  public Comprehension (Tables tables, Expression operation, Expression identity,
                        Expression expression, AbstractList patterns, AbstractList expressions)
    {
      this(tables,operation,identity,expression,patterns,expressions,Homomorphism.DEFAULT_IN_PLACE);
    }

  /**
   * Constructs a <i>raw</i> comprehension with the specifed arguments. A comprehension
   * is <i>raw</i> as long as it has not been translated into a meaningful expression.
   * Translation will happen automatically as soon as the meaning expresssion is needed.
   */
  public Comprehension (Tables tables, Expression operation, Expression identity,
                        Expression expression, AbstractList patterns, AbstractList expressions,
                        byte inPlace)
    {
      _tables = tables;

      _operation = operation;
      _identity = identity;

      if (patterns == null)
        patterns = expressions = new ArrayList(0);

      _raw = new RawInfo(new Dummy("$OP$").addTypes(operation).setExtent(operation),
                         new Dummy("$ID$").addTypes(identity).setExtent(identity),
                         expression,patterns,expressions,inPlace);
    }

  /**
   * Constructs a fully translated comprehension using the specified expression as
   * its meaning expression.
   */
  private Comprehension (Expression construct)
    {
      _construct = construct;
    }

  public boolean _doLetWrapping = true;

  public final Comprehension setNoLetWrapping ()
    {
      _doLetWrapping = false;
      _raw.operation = _operation;
      _raw.identity = _identity;
      return this;
    }

  public final Tables tables ()
    {
      return _tables;
    }

  public final Expression operation ()
    {
      return _operation;
    }

  public final Expression identity ()
    {
      return _identity;
    }

  public final Expression copy ()
    {
      if (_raw == null)
        return new Comprehension(_construct.copy());

      ArrayList patterns = new ArrayList(_raw.patterns.size());
      for (int i=0; i<_raw.patterns.size(); i++)
        {
          Expression pattern = (Expression)_raw.patterns.get(i);
          if (pattern != null) patterns.add(pattern.copy());
        }

      ArrayList expressions = new ArrayList(_raw.expressions.size());
      for (int i=0; i<_raw.expressions.size(); i++)
        expressions.add(((Expression)_raw.expressions.get(i)).copy());

      return new Comprehension(_tables,_operation.copy(),_identity.copy(),
                               _raw.expression.copy(),patterns,expressions,
                               _raw.inPlace);
    }

  public final Expression typedCopy ()
    {
      if (_raw == null)
        return new Comprehension(_construct.typedCopy());

      ArrayList patterns = new ArrayList(_raw.patterns.size());
      for (int i=0; i<_raw.patterns.size(); i++)
        {
          Expression pattern = (Expression)_raw.patterns.get(i);
          if (pattern != null) patterns.add(pattern.typedCopy());
        }

      ArrayList expressions = new ArrayList(_raw.expressions.size());
      for (int i=0; i<_raw.expressions.size(); i++)
        expressions.add(((Expression)_raw.expressions.get(i)).typedCopy());

      return new Comprehension(_tables,_operation.typedCopy(),_identity.typedCopy(),
                               _raw.expression.typedCopy(),patterns,expressions,
                               _raw.inPlace).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      if (_raw != null) _construct();
      return _construct.numberOfSubexpressions();
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (_raw != null) _construct();
      return _construct.subexpression(n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      if (_raw != null) _construct();
      return _construct.setSubexpression(n,expression);
    }

  public final Expression sanitizeNames (ParameterStack parameters)
    {
      if (_raw != null) _construct();
      _construct = _construct.sanitizeNames(parameters);
      return this;
    }

  public final void sanitizeSorts (Enclosure enclosure)
    {
      _construct.sanitizeSorts(enclosure);
    }

  /**
   * Constructs this monoid comprehension by first desugaring its patterns into simple
   * parameters, then normalizing its qualifiers by unnestings filters as far to the left
   * as possible, and finally translating the transformed raw comprehension into its meaning
   * expression.
   */
  private final void _construct () throws UndefinedEqualityException
    {
      desugarPatterns();
      unnestInnerFilters();
    }

  /**
   * Returns the comprehension obtained after applying the specified substitution
   * to the subexpressions of this. If this comprehension is already translated, this
   * simply amounts to setting the construct to the substituted construct. If this is
   * a raw comprehension, care must be taken to proceed from left to right over the
   * qualifiers and preventing generator variables from being substituted inside
   * expressions to their right (including the main expression of the comprehension).
   */
  public Expression substitute (HashMap substitution)
    {
      if (_raw == null)
        {
          _construct = _construct.substitute(substitution);
          return this;
        }

      if (!substitution.isEmpty())
        {
          _operation = _operation.substitute(substitution);
          _identity = _identity.substitute(substitution);

          _substituteQualifiers(0,substitution);
        }

      return this;
    }

  /**
   * Proceeds through the raw qualifiers substituting expressions making sure that
   * generator parameters are removed from the substitution before applying it to
   * what lies to the right of the specified index (including the main expression
   * of the comprehension).
   */
  protected void _substituteQualifiers (int index, HashMap substitution)
    {
      if (index == _raw.patterns.size())
        _raw.expression = _raw.expression.substitute(substitution);
      else
        {
          Expression pattern = (Expression)_raw.patterns.get(index);
          Expression expression = (Expression)_raw.expressions.get(index);

          // in all cases, apply the substitution to the qualifying expression
          _raw.expressions.set(index,expression.substitute(substitution));

          if (pattern == null)
            // this is a filter - simply proceed
            _substituteQualifiers(index+1,substitution);
          else
            // this is a generator - must check whether pattern is an opaque parameter
            if (pattern instanceof Parameter || (pattern instanceof Dummy && OPAQUE_PARAMETERS))
              {
                // this is an opaque parameter - it is removed from the substitution
                // before proceeding further to the right, and reinstated afterwards
                String name = pattern instanceof Dummy ? ((Dummy)pattern).name()
                                                       : ((Parameter)pattern).name();
                Object value = substitution.remove(name);
                _substituteQualifiers(index+1,substitution);
                if (value != null) substitution.put(name,value);
              }
            else
              { // this is not a parameter - apply the substitution to the pattern and proceed
                _raw.patterns.set(index,pattern.substitute(substitution));
                _substituteQualifiers(index+1,substitution);
              }
        }
    }

  /**
   * Sets the link to the enclosing scope of this comprehension to the specified expression,
   * then visits all the qualifier expressions to link up their scope trees of nested
   * comprehensions to this, and returns the number of such nested comprehensions.
   */
  final int linkScopeTree (Expression ancestor)
    {
      if (_scopeTreeIsLinked)
        return _nestedComprehensionCount;
        
      _enclosingScope = ancestor;
      _nestedComprehensionCount = _raw.expression.linkScopeTree(this);
      
      for (int i=_raw.expressions.size(); i-->0;)
        _nestedComprehensionCount += ((Expression)_raw.expressions.get(i)).linkScopeTree(this);

      _scopeTreeIsLinked = true;

      return 1 + _nestedComprehensionCount;
    }

  /**
   * Desugars the patterns of this comprehension into simple parameters, substituting
   * expression in terms of these parameters inside the comprehension where appropriate.
   * Then, this proceeds desugaring the patterns of nested comprehensions if any. It is
   * important for this method to proceed top down so that the patterns of potential inner
   * comprehensions may be affected by the desugaring of outer ones.
   */
  final protected void desugarPatterns () throws UndefinedEqualityException
    {
      if (_raw == null || _raw.isDesugared)
        return;

      _desugarPatterns();

      if (_nestedComprehensionCount > 0)
        {
          for (int i=0; i<_raw.patterns.size(); i++)
            {
              Expression pattern = (Expression)_raw.patterns.get(i);
              if (pattern != null) pattern.desugarPatterns();
              ((Expression)_raw.expressions.get(i)).desugarPatterns();
            }

          _raw.expression.desugarPatterns();
        }
    }

  /**
   * Converts the patterns into simple parameters and substitutes free occurrences of
   * the formal names from the patterns by what is appropriate in terms of the new
   * parameters inside the raw expression (and any other pertinent expression in
   * raw expressions - <i>i.e.</i>, those to the right of a pattern generator).
   * While desugaring, new filters may be generated along the way upon repeated
   * occurrences of formal names or the presence of interpretable expressions in the
   * patterns. These are simply appended to the raw list of expressions. Because of
   * this, we need to append as many <tt>null</tt>s to the list of patterns in order
   * to maintain the two lists at equal lengths.
   */
  private final void _desugarPatterns () throws UndefinedEqualityException
    {
      HashMap substitution = _initialSubstitution();

      int size = _raw.patterns.size();
      for (int i=0; i<size; i++)
        _raw.patterns.set(i,_desugarPattern(i,(Expression)_raw.patterns.get(i),substitution));

      for (int i=_raw.expressions.size()-size; i-->0;) _raw.patterns.add(null);

      _substituteDesugaring(substitution);

      _raw.isDesugared = true;
    }

  /**
   * Returns a substitution initialized with the local parameters of the
   * enclosing scopes of this comprehension if any.
   */
  protected HashMap _initialSubstitution ()
    {
      HashMap substitution = new HashMap();

      for (Expression e = _enclosingScope; e != null; e = e.enclosingScope())
        if (e instanceof Scope)
          {
            Scope s = (Scope)e;
            for (int i = s.arity(); i-->0;)
              {
                String name = s.parameter(i).name();
                if (substitution.get(name) == null)
                  substitution.put(name,
                                   new IndexedExpression(new Dummy(s.parameter(i))));
              }
          }

      return substitution;
    }

  /**
   * Transforms the specified pattern into a parameter and records in the specified
   * substitution any appropriate expression in terms of this parameter for
   * corresponding occurrences of the pattern components in the qualifying
   * expressions at index higher than the specified index.
   *
   * <p>
   *
   * In <tt>OPAQUE_PARAMETERS</tt> mode (the default), an outer pattern consisting
   * of just an identifier is always considered new and creates an opaque scope for
   * its free occurrences in the qualifier expressions lying on its right as well
   * as for the main expression of the comprehension. If on the other hand
   * <tt>OPAQUE_PARAMETERS</tt> is <tt>false</tt>, such an identifier is deemed
   * sensitive to its namesakes in the substitution and global scalar (<i>i.e.</i>,
   * non-functional) definitions. Then, it will be considered a repeated or
   * interpreted occurrence, whichever the case may be.
   *
   * @return the parameter desugaring the specified pattern
   */
  protected Parameter _desugarPattern (int index, Expression pattern, HashMap substitution)
    throws UndefinedEqualityException
    {
      if (pattern == null || pattern instanceof Parameter)
        return (Parameter)pattern;

      Parameter parameter = null;
      Dummy variable = null;

      if (pattern instanceof Dummy)
        { // the pattern is an identifier
          variable = (Dummy)pattern;
          parameter = new Parameter(variable);

          if (!OPAQUE_PARAMETERS)
            {
              IndexedExpression value = (IndexedExpression)substitution.get(variable.name());

              if (value == null)
                { // this is the first occurrence - record only if not a global scalar
                  if (!_tables.isDefinedScalar(variable.name()))
                    substitution.put(variable.name(),new IndexedExpression(index,variable));
                }
              else
                { // this is a repeated occurrence - generate an equality filter
                  variable = new Dummy(parameter = new Parameter(value.expression.typeRef()));
                  _raw.expressions.add(new Application(_tables.equality(),
                                                       variable,
                                                       value.expression.typedCopy()));
                }
            }

          return parameter;
        }

      parameter = new Parameter(pattern.typeRef());
      variable = new Dummy(parameter.name());

      if (pattern instanceof Tuple)
        // the pattern is a tuple - proceed with desugaring it
        _desugarTuplePattern(index,(Tuple)pattern,variable,substitution);
      else
        // the pattern is an interpreted expression - generate an equality filter
        _raw.expressions.add(new Application(_tables.equality(),variable,pattern));

      return parameter;
    }

  /**
   * Desugars the specified <tt>tuple</tt> pattern given that the expression in which it
   * is immediately nested is the expression specified as <tt>father</tt>.
   */
  protected final void _desugarTuplePattern (int index, Tuple tuple, Expression father,
					     HashMap substitution)
    throws UndefinedEqualityException
    {
      if (tuple instanceof NamedTuple)
        { // treat named tuples specially
          _desugarNamedTuplePattern(index,(NamedTuple)tuple,father,substitution);
          return;
        }

      int dimension = tuple.dimension();
      for (int i=0; i<dimension; i++)
        // desugar each tuple component using the appropriate tuple projection as father
        _desugarTupleComponent(index,
                               tuple.component(i),
                               new TupleProjection(father,new Int(i+1)),
                               substitution);
    }

  /**
   * Desugars the specified named <tt>tuple</tt> pattern given that the expression in which it
   * is immediately nested is the expression specified as <tt>father</tt>.
   */
  private final void _desugarNamedTuplePattern (int index, NamedTuple tuple, Expression father,
                                                HashMap substitution)
    throws UndefinedEqualityException
    {
      TupleFieldName[] fields = tuple.fields();
      int dimension = fields.length;
      for (int i=0; i<dimension; i++)
        // desugar each tuple component using the appropriate tuple projection as father
        _desugarTupleComponent(index,
                               tuple.component(i),
                               new TupleProjection(father,new StringConstant(fields[i].name())),
                               substitution);
    }

  /**
   * Desugars the specified tuple component corresponding to the specified tuple projection.
   */
  protected void _desugarTupleComponent (int index, Expression component,
					 TupleProjection projection, HashMap substitution)
    throws UndefinedEqualityException
    {
      if (component instanceof Dummy)
        { // it is a leaf consisting of a name
          Dummy variable = (Dummy)component;
          IndexedExpression value = (IndexedExpression)substitution.get(variable.name());

          if (value == null && !_tables.isDefinedScalar(variable.name()))
            // record only if first occurrence and not a global scalar
            substitution.put(variable.name(),new IndexedExpression(index,projection));
          else
            // it is a repeated occurrence or a global scalar - generate an equality filter
            _raw.expressions.add(new Application(_tables.equality(),
                                                 projection,
                                                 variable.typedCopy()));

          return;
        }

      if (component instanceof Tuple)
        // it is a nested tuple pattern - desugar the nested pattern
        _desugarTuplePattern(index,(Tuple)component,projection,substitution);
      else
        // it is an interpreted expression - generate an equality filter
        _raw.expressions.add(new Application(_tables.equality(),projection,component));
    }

  /**
   * Applies the desugaring substitutions to each qualifier expression and the main
   * expression, taking care of enabling only those substitutions at indices less
   * than the index of the qualifier (and all of them for the main expression).
   */
  private final void _substituteDesugaring (HashMap substitution)
    {
      if (substitution.isEmpty())
        return;

      int index = 0;
      int size = _raw.expressions.size();

      while (index < size) // skip to the first desugared pattern
        {
          Parameter parameter = (Parameter)_raw.patterns.get(index);
          if (parameter != null && parameter.isInternal())
            break;
          index++;
        }

      HashMap partialSubstitution = new HashMap(substitution.size());
      for (int i=index; i < size; i++)
        {
          _updateSubstitution(i,partialSubstitution,substitution);
          _raw.expressions.set(i,((Expression)_raw.expressions.get(i))
                                 .substitute(partialSubstitution));
        }

      _raw.expression = _raw.expression.substitute(partialSubstitution);
    }

  /**
   * Adds to the specified partial substitution any expression from <tt>reference</tt>
   * (an indexed-expression substitution) with an index less than, or equal to, the
   * specified index, removing such indexed-expressions from <tt>reference</tt>.
   */
  private final static void _updateSubstitution (int index, HashMap partial, HashMap reference)
    {
      ArrayList keys = new ArrayList();

      for (Iterator i=reference.entrySet().iterator(); i.hasNext();)
        {
          Map.Entry entry = (Map.Entry)i.next();
          String key = (String)entry.getKey();
          IndexedExpression value = (IndexedExpression)entry.getValue();
          if (index <= value.index)
            {
              partial.put(key,value.expression);
              keys.add(key);
            }
        }

      int size = keys.size();
      for (int i=size; i-->0;)
        reference.remove(keys.get(i));
    }

  /**
   * First unnests the filters of all nested comprehensions if any, then unnests the
   * filters of this comprehension. It is important to proceed bottom up because filters
   * may migrate up from inner comprehensions, and therefore the filters of a comprehension
   * must be unnested only after those of its nested comprehensions have been unnested.
   */
  final protected void unnestInnerFilters ()
    {
      if (_nestedComprehensionCount > 0)
        {
          _raw.expression.unnestInnerFilters();
          for (int i=_raw.expressions.size(); i-->0;)
            ((Expression)_raw.expressions.get(i)).unnestInnerFilters();
          }

      _unnestFilters();
    }

  /**
   * Normalizes the qualifiers of this comprehension by unnesting the filters to the
   * left as far as they may go, recognizing selectors and slicing filters, and sets
   * the construct to the translation of the comprehension.
   */
  private final void _unnestFilters ()
    {
      Qualifier[] qualifiers = new Qualifier[_raw.patterns == null ? 0 : _raw.patterns.size()];
      for (int i=qualifiers.length; i-->0;)
        qualifiers[i] = new Qualifier((Parameter)_raw.patterns.get(i),
                                      (Expression)_raw.expressions.get(i));

      if (qualifiers.length > 0) _normalize(qualifiers);

      _construct = _translate(qualifiers,0);
      if (_doLetWrapping && !_isLetWrapped())
        {
          Parameter[] monoidParameters = { new Parameter("$OP$"), new Parameter("$ID$") };
          Expression[] monoidComponents = { _operation, _identity };

          _construct = new Let(monoidParameters,monoidComponents,_construct);
        }

      _raw = null;
      //Debug.step(this);
    }

  /**
   * Returns <tt>true</tt> iff the first comprehension in which this is nested (if
   * any) is one involving the same monoid - then, as it is already wrapped inside
   * that comprehension <tt>Let</tt> over the same operation and identity, there is
   * no needed to wrap it again.
   */
  private final boolean _isLetWrapped ()
    {
      for (Expression e = _enclosingScope; e != null; e = e.enclosingScope())
        if (e instanceof Comprehension)
          {
            Comprehension c = (Comprehension)e;
            if (operation().equals(c.operation()) && identity().equals(c.identity()))
              return true;

            return false;
          }

      return false;
    }

  /**
   * Reshapes the specified array of qualifiers unnesting all boolean filters by moving
   * them to the left as far as they may go (<i>i.e.</i>, no further than a generator
   * whose parameter occurs free in the filter), and merging all filters related to the
   * same generator into an common <tt>and</tt>. A selector or slicing condition is
   * recognized and treated specially: it is passed to its generator qualifier where
   * it is then processed appropriately.
   */
  private final void _normalize (Qualifier[] qualifiers)
    {
      //System.out.print("Before normalization..."); Debug.step(qualifiers);
      _unnestFilters(qualifiers.length-1,qualifiers);
      //System.out.print("After normalization...");  Debug.step(qualifiers);
    }

  /**
   * Normalizes the specified array of qualifiers up to the specified index minus
   * one, then proceeds to unnest leftward the qualifier at the specified index.
   */
  private final void _unnestFilters (int index, Qualifier[] qualifiers)
    {
      if (index == -1) return;

      int upperLimit = index;
      _unnestFilters(upperLimit-1,qualifiers);
      Qualifier qualifier = qualifiers[index];

      // push this qualifier to the left over null qualifiers if any
      int i = index-1;
      while (i >= 0 && qualifiers[i] == null) i--;
      if (i < index-1)
        {
          qualifiers[index] = null;
          qualifiers[index = i+1] = qualifier;
        }

      if (qualifier.isGenerator()) return;

      // this qualifier is then a filter - unnest it as far as it can go
      while (index > 0)
        if (qualifiers[index-1].isGenerator())
          if (qualifier.expression.containsFreeName(qualifiers[index-1].parameter.name()))
            { // collect if selector, or slicing with no selectors; else, leave the filter there
              if (qualifier.isSelector(qualifiers[index-1].parameter))
                {
                  qualifiers[index-1].addSelector(qualifier.expression);
                  _eraseQualifier(index,upperLimit,qualifiers);
                }
              else
                if (qualifiers[index-1].selectors == null
                    && qualifier.isSlicing(qualifiers[index-1].parameter))
                  {
                    qualifiers[index-1].addSlicing(qualifier.expression);
                    _eraseQualifier(index,upperLimit,qualifiers);
                  }
              return; // this is as far as it can go
            }
          else // move this filter over one step to the left
            {
              qualifiers[index] = qualifiers[index-1];
              qualifiers[index = index-1] = qualifier;
            }
        else // qualifiers[index-1] is a filter
          if (index > 1) // if qualifiers[index-2] exists, it must contain a generator
            if (!qualifier.expression.containsFreeName(qualifiers[index-2].parameter.name()))
              { // move this filter over two steps to the left
                qualifiers[index] = qualifiers[index-1];
                qualifiers[index-1] = qualifiers[index-2];
                qualifiers[index = index-2] = qualifier;
              }
            else // collect if selector, or slicing with no selectors; else, merge into the filter
              {
                if (qualifier.isSelector(qualifiers[index-2].parameter))
                  qualifiers[index-2].addSelector(qualifier.expression);
                else
                  if (qualifiers[index-2].selectors == null
                      && qualifier.isSlicing(qualifiers[index-2].parameter))
                    qualifiers[index-2].addSlicing(qualifier.expression);
                  else // merge this filter with the previous one using an 'and'
                    qualifiers[index-1].expression = new And(qualifiers[index-1].expression,
                                                             qualifier.expression);
                _eraseQualifier(index,upperLimit,qualifiers);
                return; // this is as far as it can go
              }
          else // unnest further up, or merge this filter into the previous one using an 'and'
            {
              if (!_isFurtherUnnestable(qualifier.expression))
                qualifiers[index-1].expression = new And(qualifiers[index-1].expression,
                                                         qualifier.expression);
              _eraseQualifier(index,upperLimit,qualifiers);
              return; // this is as far as it can go
            }

      // index == 0
      if (_isFurtherUnnestable(qualifier.expression))
        _eraseQualifier(index,upperLimit,qualifiers);      
    }

  /**
   * Goes up the scope tree as far as it can without crossing a scope that either
   * contains more than one nested comprehension or captures a free variable in the
   * specified filter, until it reaches a comprehension. If it can do so and the found
   * comprehension is of same nature as this one, adds the filter to that comprehension,
   * and returns <tt>true</tt>; otherwise, returns <tt>false</tt>.
   */
  private final boolean _isFurtherUnnestable (Expression filter)
    {
      Expression enclosingScope = _enclosingScope;

      while (enclosingScope != null && enclosingScope.nestedComprehensionCount() == 1)
        {
          if (enclosingScope instanceof Comprehension)
            {
              Comprehension comp = (Comprehension)enclosingScope;
              if (operation().equals(comp.operation()) && identity().equals(comp.identity()))
                {
                  comp.addFilter(filter);
                  return true;
                }

              return false;
            }

          Scope scope = (Scope)enclosingScope;
          for (int i=scope.arity(); i-->0;)
            if (filter.containsFreeName(scope.parameter(i).name()))
              return false;

          enclosingScope = scope.enclosingScope();
        }

      return false;
    }

  /**
   * Adds the specified filter to the qualifiers of this comprehension.
   */
  final void addFilter (Expression filter)
    {
      _raw.patterns.add(null);
      _raw.expressions.add(filter);
    }

  /**
   * Sets the qualifier at the specified <tt>index</tt> to <tt>null</tt> and percolates this
   * <tt>null</tt> as far to the right as it may go.
   */
  private final static void _eraseQualifier (int index, int upperLimit, Qualifier[] qualifiers)
    {
      qualifiers[index] = null;
      for (int i=index; i < upperLimit && qualifiers[i+1] != null; i++)
        {
          qualifiers[i] = qualifiers[i+1];
          qualifiers[i+1] = null;
        }
    }

  /**
   * This translates monoid comprehension syntax using (possibly filtered) homomorphisms. It
   * assumes that the array of qualifiers has been normalized. The translation scheme is as
   * follows:
   * <p>
   * <pre>
   * <span style="color:navy">
   * [op,id]{e | } = op(e,id);
   *
   * [op,id]{e | c} = <b>if</b> c <b>then</b> op(e,id) <b>else</b> id;
   *
   * [op,id]{e | x &lt;- e', c, Q} = <b>f_hom</b>(e', &lambda;x.[op,id]{e | Q}, op, id, &lambda;x.c);
   *
   * [op,id]{e | x &lt;- e', y &lt;- e'', Q} = <b>hom</b>(e', &lambda;x.[op,id]{e | y &lt;- e'', Q}, op, id);
   * </span>
   * </pre>
   */
  private final Expression _translate (Qualifier[] qualifiers, int index)
    {
      if (index == qualifiers.length || qualifiers[index] == null)
        return new Application(_raw.op(),_raw.expression,_raw.id());

      Expression body = null;
      Homomorphism hom = null;
      
      if (index < qualifiers.length-1 && qualifiers[index].parameter != null
          && qualifiers[index+1] != null && qualifiers[index+1].parameter == null)
        {
          body = _translate(qualifiers,index+2);

          if (qualifiers[index].selectors != null)
            return _selectorExpression(qualifiers[index],qualifiers[index+1].expression,body);

          hom = new FilterHomomorphism(_tables,
                                       qualifiers[index].expression,
                                       new Scope(qualifiers[index].parameter,body),
                                       _raw.op(),_raw.id(),
                                       new Scope((Parameter)qualifiers[index].parameter.typedCopy(),
                                                 qualifiers[index+1].expression));
        }
      else
        {
          body = _translate(qualifiers,index+1);

          if (qualifiers[index].parameter == null)
            return new IfThenElse(qualifiers[index].expression,body,_raw.id());

          if (qualifiers[index].selectors != null)
            return _selectorExpression(qualifiers[index],null,body);

          hom = new Homomorphism(qualifiers[index].expression,
                                 new Scope(qualifiers[index].parameter,body),
                                 _raw.op(),_raw.id());
        }

      if (qualifiers[index].slicings != null)
        hom.setSlicings(qualifiers[index].slicings);
      
      if (_raw.inPlace == Homomorphism.ENABLED_IN_PLACE)
        return hom.enableInPlace();
      if (_raw.inPlace == Homomorphism.DISABLED_IN_PLACE)
        return hom.disableInPlace();

      return hom;
    }

  /**
   * This returns a <tt>Let</tt> wrapping an <tt>IfThenElse</tt> as the transformed
   * expression resulting from a (possibly filtered) generator that contains at least
   * one selector expression.
   *
   * More precisely, if the generator is of the form:
   * <pre>
   * <span style="color:navy">
   * x &lt;- e <b>such that</b> f
   *        <b>sliced by</b> s1, ..., sm
   *        <b>selected by</b> v1, ..., vn
   * </span>
   * </pre>
   * and the body of translating the remaining qualifiers is <span style="color:navy">
   * <tt>body</tt></span>, then the resulting selector expression is:
   * <pre>
   * <span style="color:navy">
   * <b>let</b> x = v1
   *  <b>in</b> <b>if</b> x <b>is_in</b> e
   *        <b>and</b> x == v2 <b>and</b> ... <b>and</b> x == vn
   *        <b>and</b> s1 <b>and</b> ... <b>and</b> sm <b>and</b> f
   *     <b>then</b> body
   *     <b>else</b> id
   * </span>
   * </pre>
   * where each slicing has its slicing variable unsanitized from a dummy local back to a dummy. 
   */
  private final Expression _selectorExpression (Qualifier generator, Expression filter,
                                                Expression body)
    {
      Expression condition = new Application(_tables.in(),
                                             new Dummy(generator.parameter),
                                             generator.expression);

      for (int i=1; i<generator.selectors.size(); i++)
        condition = new And(condition,
                            (Expression)generator.selectors.get(i));

      if (generator.slicings != null)
        for (int i=0; i<generator.slicings.size(); i++)
          condition = new And(condition,
                              ((Application)generator.slicings.get(i)).undoDummyLocal());

      if (filter != null)
        condition = new And(condition,filter);

      return new Let(generator.parameter,
                     ((Application)generator.selectors.get(0)).argument(1),
                     new IfThenElse(condition,body,_raw.id()));
    }
                                            

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _construct.setCheckedType();
      setCheckedType(_construct.checkedType());
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      if (!(_construct instanceof Let))
        {
          _construct.typeCheck(_type,typeChecker);
          return;
        }

      Let let = (Let)_construct;
      let.setType(_type);

      Scope scope = (Scope)let.function();
      typeChecker.unify(scope.parameter(0).typeRef(),operation().typeRef(),this);
      typeChecker.unify(scope.parameter(1).typeRef(),identity().typeRef(),this);

      Expression operation = let.argument(0);
      Expression identity = let.argument(1);

      Type[] argumentTypes = { operation.typeRef(), identity.typeRef() };
      FunctionType functionType = new FunctionType(argumentTypes,let.typeRef()).setNoCurrying();

      identity.typeCheck(typeChecker);
      let.function().typeCheck(functionType,typeChecker);
      operation.typeCheck(functionType.domains()[0],typeChecker);
    }

  public final void compile (Compiler compiler)
    {
      if (_construct instanceof Let) _fixTypeBoxing();
      _construct.compile(compiler);
    }

  /**
   * This fixes the boxing of the monoid operator and identity by systematically unboxing
   * all occurrences of the collection element type. This is a necessary hack [:( sigh!]
   * because collection-building built-in dummy instructions like <tt>SET_ADD</tt> have a
   * needlessly polymorphic type that becomes instantiated only when it is applied. However,
   * a monoid comprehension construct is a <a href="Let.html"><tt>Let</tt></a> that
   * abstracts the monoid operation and identity. Now, when the operation is <tt>SET_ADD</tt>,
   * for example, as the compiler compiles the application corresponding to the "let", it sees
   * it as a non-applied function argument with a polymorphic type and will proceed to "pad" it
   * (see <a href="Expression.html"><tt>Expression</tt></a>). This "padding" must be avoided,
   * as well as all boxing of the types corresponding to the elements of the collection.
   */
  private final void _fixTypeBoxing ()
    {
      Let let = (Let)_construct;
      
      FunctionType potype = (FunctionType)((FunctionType)let.function().checkedType()).domain(0);
      FunctionType otype = (FunctionType)let.argument(0).checkedType();
      Type itype = let.argument(1).checkedType();

      if (itype.kind() == Type.BOXABLE)
        ((BoxableTypeConstant)itype).setBoxed(false);

      if (otype.domain(0).kind() == Type.BOXABLE)
        {
          ((BoxableTypeConstant)otype.domain(0)).setBoxed(false);
          otype.unsetDomainBox(0);

          ((BoxableTypeConstant)potype.domain(0)).setBoxed(false);
          potype.unsetDomainBox(0);

          if (otype.domain(0).isEqualTo(otype.domain(1)))   // primitive comprehension
            {
              ((BoxableTypeConstant)otype.domain(1)).setBoxed(false);
              otype.unsetDomainBox(1);

              ((BoxableTypeConstant)potype.domain(1)).setBoxed(false);
              potype.unsetDomainBox(1);

              ((BoxableTypeConstant)otype.range()).setBoxed(false);
              otype.unsetRangeBox();

              ((BoxableTypeConstant)potype.range()).setBoxed(false);
              potype.unsetRangeBox();
            }
        }
    }

  public final String toString ()
    {
      return _raw == null ? _construct.toString() : _raw.toString();
    }

  public class RawInfo
    {
      Expression operation;
      Expression identity;
      public Expression expression;
      public AbstractList patterns;
      public AbstractList expressions;
      byte inPlace;

      boolean isDesugared;

      RawInfo (Expression operation, Expression identity, Expression expression,
               AbstractList patterns, AbstractList expressions, byte inPlace)
        {
          this.expression = expression;
          this.operation = operation;
          this.identity = identity;
          this.patterns = patterns;
          this.expressions = expressions;
          this.inPlace = inPlace;
        }

      final Expression op ()
        {
          return operation.typedCopy();
        }

      final public Expression id ()
        {
          return identity.typedCopy();
        }

      public final String toString ()
        {
          StringBuilder buf = new StringBuilder("[")
                                 .append(operation())
                                 .append(",")
                                 .append(identity())
                                 .append("] { ")
                                 .append(expression)
                                 .append(" | ");

          for (int i=0; i<patterns.size(); i++)
            {
              Object pattern = patterns.get(i);
              if (pattern != null)
                buf.append(pattern).append(" <- ");
              buf.append(expressions.get(i));
              if (i < patterns.size() - 1)
                buf.append(", ");
            }

          return buf.append(" }").toString();
        }
    }

  public static class IndexedExpression
    {
      int index = -1;
      public Expression expression;

      public IndexedExpression (Expression expression)
        {
          this.expression = expression;
        }

      public IndexedExpression (int index, Expression expression)
        {
          this.index = index;
          this.expression = expression;
        }

      public final String toString ()
        {
          return expression + "/" + index;
        }
    }

  public class Qualifier
    {
      public Parameter parameter;
      public Expression expression;
      public ArrayList slicings;
      public ArrayList selectors;

      public Qualifier (Parameter parameter, Expression expression)
        {
          this.parameter = parameter;
          this.expression = expression;
        }

      final boolean isGenerator ()
        {
          return parameter != null;
        }

      final boolean isSlicing (Parameter parameter)
        {
          return expression.isSlicing(tables(),parameter);
        }

      final void addSlicing (Expression slicing)
        {
          if (slicings == null)
            slicings = new ArrayList();
          slicings.add(slicing);
        }

      final boolean isSelector (Parameter parameter)
        {
          return expression.isSelector(tables(),parameter);
        }

      final void addSelector (Expression selector)
        {
          if (selectors == null)
            selectors = new ArrayList();
          selectors.add(selector);
        }

      public final String toString ()
        {
          if (parameter == null)
            return expression.toString();

          return parameter + " <- " + expression +
                 (selectors == null ? "" : " selected by " + selectors) +
                 (slicings == null ? "" : " sliced by " + slicings);
        }
    }
}
