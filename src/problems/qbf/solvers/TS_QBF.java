package problems.qbf.solvers;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;
import java.util.NavigableMap;

import metaheuristics.tabusearch.AbstractTS;
import metaheuristics.tabusearch.Intensificator;
import problems.qbf.QBF_Inverse;
import solutions.Solution;

/**
 * Metaheuristic TS (Tabu Search) for obtaining an optimal solution to a QBF
 * (Quadractive Binary Function -- {@link #QuadracticBinaryFunction}).
 * Since by default this TS considers minimization problems, an inverse QBF
 *  function is adopted.
 * 
 * @author ccavellucci, fusberti, einnarelli, jmenezes, vferrari
 */
public class TS_QBF extends AbstractTS<Integer> {
	
	private final Integer fake = -1;

	/**
	 * Set of fixed variables, always empty if intensification is disabled.
	 */
	private Set<Integer> fixed;

	/**
	 * Constructor for the TS_QBF class. An inverse QBF objective function is
	 * passed as argument for the superclass constructor.
	 * 
	 * @param tenure
	 *		The Tabu tenure parameter.
	 * @param iterations
	 *		The number of iterations which the TS will be executed.
	 * @param filename
	 *		Name of the file for which the objective function parameters
	 *		should be read.
	 * @param intensificator
	 * 		Intensificator parameters. If {@code null}, intensification is not
	 * 		applied.
	 * @throws IOException
	 *             necessary for I/O operations.
	 */
	public TS_QBF(
		Integer tenure,
		Integer iterations, 
		String filename, 
		Intensificator intensificator
	) throws IOException {
		super(new QBF_Inverse(filename), tenure, iterations, intensificator);
		fixed = new HashSet<Integer>();
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeCL()
	 */
	@Override
	public ArrayList<Integer> makeCL() {

		ArrayList<Integer> _CL = new ArrayList<Integer>();
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer cand = i;
			_CL.add(cand);
		}

		return _CL;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeRCL()
	 */
	@Override
	public ArrayList<Integer> makeRCL() {

		ArrayList<Integer> _RCL = new ArrayList<Integer>();

		return _RCL;

	}
	
	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeTL()
	 */
	@Override
	public ArrayDeque<Integer> makeTL() {

		ArrayDeque<Integer> _TS = new ArrayDeque<Integer>(2*tenure);
		for (int i=0; i<2*tenure; i++) {
			_TS.add(fake);
		}

		return _TS;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#updateCL()
	 */
	@Override
	public void updateCL() {

		// do nothing

	}

	/**
	 * {@inheritDoc}
	 * 
	 * This createEmptySol instantiates an empty solution and it attributes a
	 * zero cost, since it is known that a QBF solution with all variables set
	 * to zero has also zero cost.
	 */
	@Override
	public Solution<Integer> createEmptySol() {
		Solution<Integer> sol = new Solution<Integer>();
		sol.cost = 0.0;
		return sol;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The local search operator developed for the QBF objective function is
	 * composed by the neighborhood moves Insertion, Removal and 2-Exchange.
	 */
	@Override
	public Solution<Integer> neighborhoodMove() {

		Double minDeltaCost;
		Integer bestCandIn = null, bestCandOut = null;

		minDeltaCost = Double.POSITIVE_INFINITY;
		updateCL();

		// Evaluate insertions
		for (Integer candIn : CL) {

			Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, currentSol);
			Boolean ignoreCand = TL.contains(candIn) || fixed.contains(candIn);

			if (!ignoreCand || currentSol.cost+deltaCost < incumbentSol.cost) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = candIn;
					bestCandOut = null;
				}
			}

		}

		// Evaluate removals
		for (Integer candOut : currentSol) {

			Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, currentSol);
			Boolean ignoreCand = TL.contains(candOut) || fixed.contains(candOut);

			if (!ignoreCand || currentSol.cost+deltaCost < incumbentSol.cost) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = null;
					bestCandOut = candOut;
				}
			}

		}

		// Evaluate exchanges
		for (Integer candIn : CL) {
			for (Integer candOut : currentSol) {

				Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, currentSol);
				Boolean ignoreCands =
					TL.contains(candIn) ||
					TL.contains(candOut) ||
					fixed.contains(candIn) ||
					fixed.contains(candOut);

				if (!ignoreCands || currentSol.cost+deltaCost < incumbentSol.cost) {
					if (deltaCost < minDeltaCost) {
						minDeltaCost = deltaCost;
						bestCandIn = candIn;
						bestCandOut = candOut;
					}
				}

			}
		}
		
		// Implement the best non-tabu move
		TL.poll();
		if (bestCandOut != null) {
			currentSol.remove(bestCandOut);
			CL.add(bestCandOut);
			TL.add(bestCandOut);
		} else {
			TL.add(fake);
		}
		TL.poll();
		if (bestCandIn != null) {
			currentSol.add(bestCandIn);
			CL.remove(bestCandIn);
			TL.add(bestCandIn);
		} else {
			TL.add(fake);
		}
		ObjFunction.evaluate(currentSol);
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateRecency() {
		
		// Cast obj. to problem to get the variables
		QBF_Inverse qbf = (QBF_Inverse) ObjFunction;
		qbf.setVariables(currentSol);

		for (int e = 0; e < ObjFunction.getDomainSize(); e++) {

			// Increment field if element is in solution and reset it if not
			if (qbf.variables[e] == 1.0) {
				recency[e]++;
			} else {
				recency[e] = 0;
			}

		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startIntensification() {

		NavigableMap<Integer, Integer> recencyMap = new TreeMap<Integer, Integer>();

		// Initialize intensificator attributes
		intensificator.setRunning(true);
		intensificator.setRemainingIt(intensificator.getIterations());

		// Search will restart at incumbentSol
		currentSol = new Solution<Integer>(incumbentSol);

		// Cast obj. to problem to get the variables
		QBF_Inverse qbf = (QBF_Inverse) ObjFunction;
		qbf.setVariables(currentSol);

		// Create recency {recency[e], e} tree map in recency ascending order
		for (int e = 0; e < ObjFunction.getDomainSize(); e++) {

			/* Only add element to map if it was used recently and if it is in
			 * incumbent solution */
			if (recency[e] > 0 && qbf.variables[e] == 1.0) {
				recencyMap.put(recency[e], e);
			}

		}

		// Reverse order
		recencyMap = recencyMap.descendingMap();

		// Iterate over map entries, now stored in descending order
		for (Map.Entry<Integer, Integer> kv : recencyMap.entrySet()) {

			// Fix element
			fixed.add(kv.getValue());

			// Fix at most n / 2 elements
			if (fixed.size() == ObjFunction.getDomainSize() / 2) {
				break;
			}

		}

		// Reset recency array to avoid repetition in future intensifications
		Arrays.fill(recency, 0);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endIntensification() {
		intensificator.setRunning(false);
		fixed.clear();
	}

	/**
	 * A main method used for testing the TS metaheuristic.
	 */
	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();
		TS_QBF tabusearch = new TS_QBF(20, 10000, "instances/qbf020", null);
		Solution<Integer> bestSol = tabusearch.solve();
		System.out.println("maxVal = " + bestSol);
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = "+(double)totalTime/(double)1000+" seg");

	}

}
