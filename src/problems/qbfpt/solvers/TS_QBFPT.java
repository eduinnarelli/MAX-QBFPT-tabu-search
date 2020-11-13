package problems.qbfpt.solvers;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.lang.Math;

import problems.qbf.solvers.TS_QBF;
import problems.qbfpt.QBFPT;
import solutions.Solution;

/**
 * Metaheuristic Tabu Search for obtaining an optimal solution to a {@link 
 * QBFPT} (Quadractive Binary Function with Prohibited Triples). 
 * 
 * @author einnarelli, jmenezes, vferrari
 */
public class TS_QBFPT extends TS_QBF {
	private enum SearchStrategy {
		FI,
		BI
	}
	
	/**
     * Step of iterations on wich the penalty will be dynamically adjusted.
     */
	private final Integer penaltyAdjustmentStep = 25;
	
	private final Integer fake = -1;
	
    /**
     * The set T of prohibited triples.
     */
    private final Set<List<Integer>> T;
    
    /**
	 * Value to represent local search type.
	 * Can be first-improving (FI) or best-improving (BI). 
	 */
	private final SearchStrategy searchType;
	
	/**
	 * 
	 */
	private final boolean oscillation;

    /**
     * Constructor for the TS_QBFPT class.
     * 
     * @param alpha
     *            The Tabu tenure parameter.
     * @param iterations
     *            The number of iterations which the TS will be executed.
     * @param filename
     *            Name of the file for which the objective function parameters
     *            should be read.
     * @param type
     * 			  Local search strategy type, being either first improving or
     * 			  best improving.
     * @throws IOException
     *            Necessary for I/O operations.
     */
    public TS_QBFPT(
        Integer tenure, 
        Integer iterations, 
        String filename,
        SearchStrategy type,
        boolean oscillation
    ) throws IOException {

        super(tenure, iterations, filename);

        // Instantiate QBFPT problem, store T and update objective reference.
        QBFPT qbfpt = new QBFPT(filename);
        T = qbfpt.getT();
        ObjFunction = qbfpt;
        searchType = type;
        this.oscillation = oscillation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tabusearch.abstracts.AbstractTS#updateCL()
     */
    @Override
    public void updateCL() {
    	
    	// Adjust the oscillation penalty based on the number of infeasible solutions found
    	// in the previous penaltyAdjustmentStep iterations
    	if (iterationsCount!=null && (iterationsCount+1) % penaltyAdjustmentStep == 0)
    	{
    		double penalty = ((QBFPT)ObjFunction).getPenalty();
    		if(iterationsCount - lastFeasibleIteration >= penaltyAdjustmentStep-1)
    			((QBFPT)ObjFunction).setPenalty(Math.min(penalty*2, 1000000.0));
    		else if((iterationsCount+1)%200 == 0)
    			((QBFPT)ObjFunction).setPenalty(Math.max(penalty/2, 0.0000001));
    	}

        // Store numbers in solution and _CL as hash sets.
        Set<Integer> sol = new HashSet<Integer>(currentSol);
        Set<Integer> _CL = new HashSet<Integer>();
        HashMap<Integer, Integer> _violator = new HashMap<>();

        // Initialize _CL with all elements not in solution.
        for (Integer e = 0; e < ObjFunction.getDomainSize(); e++) {
        	_violator.put(e, 0);
            if (!sol.contains(e)) {
                _CL.add(e);
            }
        }
        
        // If strategic oscillation is active, no need to remove infeasible elements.
        //if(oscillation) {
        //	CL = new ArrayList<Integer>(_CL);
        //	return;
        //}

        Integer e1, e2, e3;
        Integer infeasible;
        for (List<Integer> t : T) {
        	infeasible = -1;
        	
            /**
             * Detach elements from (e1, e2, e3). They are stored as numbers 
             * from [0, n-1] in sol. and CL, different than in T ([1, n]).
             */
            e1 = t.get(0) - 1;
            e2 = t.get(1) - 1;
            e3 = t.get(2) - 1;

            // e1 and e2 in solution -> e3 infeasible.
            if (sol.contains(e1) && sol.contains(e2)) {
                infeasible = e3;
            }

            // e1 and e3 in solution -> e2 infeasible.
            else if (sol.contains(e1) && sol.contains(e3)) {
                infeasible = e2;
            }

            // e2 and e3 in solution -> e1 infeasible.
            else if (sol.contains(e2) && sol.contains(e3)) {
                infeasible = e1;
            }
            
            if(infeasible > -1) {
            	if(oscillation) 
            		_violator.put(infeasible, _violator.get(infeasible) + 1);
            	else 
            		_CL.remove(infeasible);
            }

        }

        CL = new ArrayList<Integer>(_CL);
        ((QBFPT)ObjFunction).setViolations(_violator);
    }
    
	/**
	 * {@inheritDoc}
	 * 
	 * Can be first-improving or best-improving.
	 */
	@Override
	public Solution<Integer> neighborhoodMove() {
		Solution<Integer> sol;
		
		// Check local search method.
		if (this.searchType == SearchStrategy.BI)
			sol = super.neighborhoodMove();
		else
			sol = firstImprovingNM();
	
		return sol;
	}
	
	/*
	 * First improving neighborhood move.
	 */
	private Solution<Integer> firstImprovingNM(){
		Double minDeltaCost;
		Integer bestCandIn = null, bestCandOut = null;
		boolean done = false;

		minDeltaCost = Double.POSITIVE_INFINITY;
		updateCL();
		
		// Evaluate insertions
		for (Integer candIn : CL) {
			Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, currentSol);
			if (!TL.contains(candIn) || currentSol.cost+deltaCost < incumbentSol.cost) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = candIn;
					bestCandOut = null;
					break;
				}
			}
		}
		
		// Evaluate removals
		for (Integer candOut : currentSol) {
			Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, currentSol);
			if (!TL.contains(candOut) || currentSol.cost+deltaCost < incumbentSol.cost) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = null;
					bestCandOut = candOut;
					break;
				}
			}
		}
		
		// Evaluate exchanges
		for (Integer candIn : CL) {
			for (Integer candOut : currentSol) {
				Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, currentSol);
				if ((!TL.contains(candIn) && !TL.contains(candOut)) || currentSol.cost+deltaCost < incumbentSol.cost) {
					if (deltaCost < minDeltaCost) {
						minDeltaCost = deltaCost;
						bestCandIn = candIn;
						bestCandOut = candOut;
						done = true;
						break;
					}
				}
			}
			if(done) break;
		}
		
		// Implement the best of the first non-tabu moves.
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
	 * 
	 * Check if any triple restriction is violated.
	 */
	@Override
    public boolean isSolutionFeasible(Solution<Integer> sol) {
    	boolean feasible = true;
        Integer e1, e2, e3;
        
        // Check strategic oscillation.
        if(!oscillation) return true;
        
    	for (List<Integer> t : T) {

            /**
             * Detach elements from (e1, e2, e3). They are stored as numbers 
             * from [0, n-1] in sol., different than in T ([1, n]).
             */
            e1 = t.get(0) - 1;
            e2 = t.get(1) - 1;
            e3 = t.get(2) - 1;

            // e1, e2 and e3 in solution -> infeasible.
            if (sol.contains(e1) && sol.contains(e2) && sol.contains(e3)) {
            	feasible = false;
            	break;
            }
        }
    	
    	return feasible;
    }


    /**
     * A main method used for testing the Tabu Search metaheuristic.
     */
    public static void main(String[] args) throws IOException {

        long startTime = System.currentTimeMillis();
        TS_QBF ts = new TS_QBFPT(20, 
        						 10000, 
        						 "instances/qbf200",
        						 SearchStrategy.BI,
        						 true);
        Solution<Integer> bestSol = ts.solve();
        System.out.println("maxVal = " + bestSol);
        long endTime   = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Time = "+(double)totalTime/(double)1000+" seg");

    }

}
