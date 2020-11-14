package problems.qbfpt.solvers;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

//import metaheuristics.grasp.AbstractGRASP;
import metaheuristics.tabusearch.Intensificator;
import problems.qbf.solvers.TS_QBF;
import problems.qbfpt.QBFPT;
//import problems.qbfpt.solvers.GRASP_QBFPT.BiasFunction;
//import problems.qbfpt.solvers.GRASP_QBFPT.SearchStrategy;
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
	
	private final Integer fake = -1;
	
	/**
     * Step of iterations on which the penalty will be dynamically adjusted.
     */
	private final Integer penaltyAdjustmentStep = 25;
	
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
	 * Variable that indicates if strategic oscillation is active. 
	 */
	private final boolean oscillation;

    /**
     * Constructor for the TS_QBFPT class.
     * 
     * @param alpha
     *      The Tabu tenure parameter.
     * @param iterations
     *      The number of iterations which the TS will be executed.
     * @param filename
     *      Name of the file for which the objective function parameters
     *      should be read.
     * @param type
     *      Local search strategy type, being either first improving or
     *      best improving.
     * @param intensificator
     *      Intensificator parameters. If {@code null}, intensification is not
     *      applied.
     * @param oscillation
     * 		Indicates if strategic oscillation is active or not.
     * @throws IOException
     *      Necessary for I/O operations.
     */
    public TS_QBFPT(
        Integer tenure, 
        Integer iterations, 
        String filename,
        SearchStrategy type,
        Intensificator intensificator,
        boolean oscillation,
        String _resultsFileName
    ) throws IOException {

        super(tenure, iterations, filename, intensificator);

        // Instantiate QBFPT problem, store T and update objective reference.
        QBFPT qbfpt = new QBFPT(filename);
        T = qbfpt.getT();
        ObjFunction = qbfpt;
        searchType = type;
        this.oscillation = oscillation;
        resultsFileName = _resultsFileName;

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
        Integer _violator[] = new Integer[ObjFunction.getDomainSize()];

        // Initialize _CL with all elements not in solution.
        for (Integer e = 0; e < ObjFunction.getDomainSize(); e++) {
        	_violator[e]=0;
            if (!sol.contains(e)) {
                _CL.add(e);
            }
        }

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
            		_violator[infeasible]+=1;
            	else 
            		_CL.remove(infeasible);
            }

        }

        CL = new ArrayList<Integer>(_CL);
        ((QBFPT)ObjFunction).setViolations(_violator);
    }
    
	/**
	 * {@inheritDoc}
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
            Boolean ignoreCand = TL.contains(candIn) || fixed.contains(candIn);

			if (!ignoreCand || currentSol.cost+deltaCost < incumbentSol.cost) {
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
            Boolean ignoreCand = TL.contains(candOut) || fixed.contains(candOut);

			if (!ignoreCand || currentSol.cost+deltaCost < incumbentSol.cost) {
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
						done = true;
						break;
					}
				}
            }
            
            if (done) break;
            
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
	 * Run Tabu Search for QBFPT.
	 */
	public static void run(int tenure, int maxIt, String filename,
						   SearchStrategy searchType,
						   Intensificator intensify,
						   boolean oscillation,
						   double maxTime,
						   String _resultsFileName) 
					   throws IOException {
		
		long startTime = System.currentTimeMillis();
		TS_QBFPT tabu = new TS_QBFPT(tenure,
									 maxIt,
									 filename, 
									 searchType,
									 intensify,
									 oscillation,
									 _resultsFileName);
		
		Solution<Integer> bestSol = tabu.solve(maxTime);
		System.out.println("maxVal = " + bestSol);

		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = "+(double)totalTime/(double)1000+" seg");		
	}
	
	public static void testAll(int tenure, int maxIt,
							   SearchStrategy searchType, 
							   Intensificator intensify,
							   boolean oscillation,
							   double maxTime,
							   String _resultsFileName) 
					   throws IOException {
		
		String inst[] = {"020", "040", "060", "080", "100", "200", "400"};
		
		for(String file : inst) {
			if(file == "200" && intensify != null)
				intensify = new Intensificator(2000, 100);
			
			TS_QBFPT.run(tenure, maxIt, "instances/qbf" + file, 
						 searchType, intensify, oscillation,
						 maxTime, _resultsFileName);
		}
	}
	
	/**
     * A main method used for testing the Tabu Search metaheuristic.
     */
	public static void main(String[] args) throws IOException {
		
		// Fixed parameters
		double maxTime = 1800.0;
		int maxIterations = 10000;
		Intensificator intensificator = new Intensificator(1000, 100);
		
		// Changeable parameters.
		int tenure1 = 20, tenure2 = 30;
		
		// Testing
		TS_QBFPT.run(tenure1, maxIterations, "instances/qbf200", 
				     SearchStrategy.BI, intensificator, true, maxTime, null);
		
		/*
		// 1 - Testing tenure1/best-improving/no div/no intens.
		TS_QBFPT.testAll(tenure1, maxIterations, SearchStrategy.BI, 
						 null, false, maxTime, "results/CONFIG01.csv");

		// 2 - Testing tenure1/best-improving/no div/intens.
		TS_QBFPT.testAll(tenure1, maxIterations, SearchStrategy.BI, 
						 intensificator, false, maxTime, "results/CONFIG02.csv");

		// 3 - Testing tenure1/best-improving/div/no intens.
		TS_QBFPT.testAll(tenure1, maxIterations, SearchStrategy.BI, 
						 null, true, maxTime, "results/CONFIG03.csv");
		
		// 4 - Testing tenure1/best-improving/div/intens.
		TS_QBFPT.testAll(tenure1, maxIterations, SearchStrategy.BI, 
						 intensificator, true, maxTime, "results/CONFIG04.csv");
		
		// 5 - Testing tenure1/first-improving/div/intens.
		TS_QBFPT.testAll(tenure1, maxIterations, SearchStrategy.FI, 
						 intensificator, true, maxTime, "results/CONFIG05.csv");
		
		// 6 - Testing tenure2/best-improving/div/intens.
		TS_QBFPT.testAll(tenure2, maxIterations, SearchStrategy.BI, 
						 intensificator, true, maxTime, "results/CONFIG06.csv");
		
		*/
	}
}
