package metaheuristics.tabusearch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import problems.Evaluator;
import solutions.Solution;

/**
 * Abstract class for metaheuristic Tabu Search. It consider a minimization problem.
 * 
 * @author ccavellucci, fusberti, einnarelli, jmenezes, vferrari
 * @param <E>
 *		Generic type of the candidate to enter the solution.
 */
public abstract class AbstractTS<E> {

	/**
	 * flag that indicates whether the code should print more information on
	 * screen.
	 */
	public static boolean verbose = true;

	/**
	 * a random number generator.
	 */
	static Random rng = new Random(0);

	/**
	 * the objective function being optimized.
	 */
	protected Evaluator<E> ObjFunction;

	/**
	 * the incumbent solution cost.
	 */
	protected Double incumbentCost;

	/**
	 * the current solution cost.
	 */
	protected Double currentCost;

	/**
	 * the incumbent solution.
	 */
	protected Solution<E> incumbentSol;

	/**
	 * the current solution.
	 */
	protected Solution<E> currentSol;

	/**
	 * the number of iterations the TS main loop executes.
	 */
	protected Integer iterations;

	/**
	 * current iteration.
	 */
	protected Integer iterationsCount;
	
	/**
	 * iteration where the last feasible solution was found.
	 */
	protected Integer lastFeasibleIteration;
	
	/**
	 * the tabu tenure.
	 */
	protected Integer tenure;

	/**
	 * the Candidate List of elements to enter the solution.
	 */
	protected ArrayList<E> CL;

	/**
	 * the Restricted Candidate List of elements to enter the solution.
	 */
	protected ArrayList<E> RCL;
	
	/**
	 * the Tabu List of elements to enter the solution.
	 */
	protected ArrayDeque<E> TL;

	/**
	 * the consecutive number of times the solution did not improve.
	 */
	protected Integer consecFailures;

	/**
	 * array that informs how long a component has been used.
	 */
	protected Integer[] recency;

	/**
	 * intensification parameters.
	 */
	protected Intensificator intensificator;

	/**
	 * Creates the Candidate List, which is an ArrayList of candidate elements
	 * that can enter a solution.
	 * 
	 * @return The Candidate List.
	 */
	public abstract ArrayList<E> makeCL();

	/**
	 * Creates the Restricted Candidate List, which is an ArrayList of the best
	 * candidate elements that can enter a solution. 
	 * 
	 * @return The Restricted Candidate List.
	 */
	public abstract ArrayList<E> makeRCL();
	
	/**
	 * Creates the Tabu List, which is an ArrayDeque of the Tabu
	 * candidate elements. The number of iterations a candidate
	 * is considered tabu is given by the Tabu Tenure {@link #tenure}
	 * 
	 * @return The Tabu List.
	 */
	public abstract ArrayDeque<E> makeTL();

	/**
	 * Updates the Candidate List according to the incumbent solution
	 * {@link #currentSol}. In other words, this method is responsible for
	 * updating the costs of the candidate solution elements.
	 */
	public abstract void updateCL();

	/**
	 * Creates a new solution which is empty, i.e., does not contain any
	 * candidate solution element.
	 * 
	 * @return An empty solution.
	 */
	public abstract Solution<E> createEmptySol();

	/**
	 * The TS local search phase is responsible for repeatedly applying a
	 * neighborhood operation while the solution is getting improved, i.e.,
	 * until a local optimum is attained. When a local optimum is attained
	 * the search continues by exploring moves which can make the current 
	 * solution worse. Cycling is prevented by not allowing forbidden
	 * (tabu) moves that would otherwise backtrack to a previous solution.
	 * 
	 * @return An local optimum solution.
	 */
	public abstract Solution<E> neighborhoodMove();

	/**
	 * Updates the {@link #recency} array.
	 */
	public abstract void updateRecency();

	/**
	 * Initialize the intensification method. In this method, the search 
	 * should restart in the incumbent solution with some components fixed,
	 * such as the ones that most appeared in solutions recently ({@link 
	 * #recency}).
	 */
	public abstract void startIntensification();

	/**
	 * Stop the intensification method, i.e., revert the components fix
	 * applied in the method initialization.
	 */
	public abstract void endIntensification();

	/**
	 * Constructor for the AbstractTS class.
	 * 
	 * @param objFunction
	 *		The objective function being minimized.
	 * @param tenure
	 *		The Tabu tenure parameter. 
	 * @param iterations
	 *		The number of iterations which the TS will be executed.
	 * @param intensificator
	 * 		Intensificator parameters. If {@code null}, intensification is not
	 * 		applied.
	 */
	public AbstractTS(
		Evaluator<E> objFunction, 
		Integer tenure, 
		Integer iterations, 
		Intensificator intensificator
	){
		this.ObjFunction = objFunction;
		this.tenure = tenure;
		this.iterations = iterations;
		this.intensificator = intensificator;
	}

	/**
	 * The TS constructive heuristic, which is responsible for building a
	 * feasible solution by selecting in a greedy fashion, candidate
	 * elements to enter the solution.
	 * 
	 * @return A feasible solution to the problem being minimized.
	 */
	public Solution<E> constructiveHeuristic() {

		CL = makeCL();
		RCL = makeRCL();
		currentSol = createEmptySol();
		currentCost = Double.POSITIVE_INFINITY;

		/* Main loop, which repeats until the stopping criteria is reached. */
		while (!constructiveStopCriteria()) {

			Double maxCost = Double.NEGATIVE_INFINITY, minCost = Double.POSITIVE_INFINITY;
			currentCost = currentSol.cost;
			updateCL();

			/*
			 * Explore all candidate elements to enter the solution, saving the
			 * highest and lowest cost variation achieved by the candidates.
			 */
			for (E c : CL) {
				Double deltaCost = ObjFunction.evaluateInsertionCost(c, currentSol);
				if (deltaCost < minCost)
					minCost = deltaCost;
				if (deltaCost > maxCost)
					maxCost = deltaCost;
			}

			/*
			 * Among all candidates, insert into the RCL those with the highest
			 * performance.
			 */
			for (E c : CL) {
				Double deltaCost = ObjFunction.evaluateInsertionCost(c, currentSol);
				if (deltaCost <= minCost) {
					RCL.add(c);
				}
			}
		
			/* Choose a candidate randomly from the RCL */
			int rndIndex = rng.nextInt(RCL.size());
			E inCand = RCL.get(rndIndex);
			CL.remove(inCand);
			currentSol.add(inCand);
			ObjFunction.evaluate(currentSol);
			RCL.clear();

		}

		return currentSol;
	}

	/**
	 * The TS mainframe. It consists of a constructive heuristic followed by
	 * a loop, in which each iteration a neighborhood move is performed on
	 * the current solution. The best solution is returned as result.
	 * 
	 * @return The best feasible solution obtained throughout all iterations.
	 */
	public Solution<E> solve() {
		boolean isFeasible;

		incumbentSol = createEmptySol();
		TL = makeTL();
		consecFailures = Integer.valueOf(0);

		// Only make recency array if intensification is enabled
		if (intensificator != null) {
			makeRecency();
		}

		// Build a initial solution
		constructiveHeuristic();

		lastFeasibleIteration = 0;
		for (iterationsCount = 0; iterationsCount < iterations; iterationsCount++) {

			/* Start intensification if it's enabled, not running and the 
			 * number of consecutive failures reached the intensificator
			 * tolerance. */
			if (
				intensificator != null && !intensificator.getRunning() && 
				consecFailures >= intensificator.getTolerance()
			) {
				consecFailures = 0; // Reset failures
				startIntensification();
				if (verbose)
					System.out.println("Intensification started at: " + iterationsCount);
			}

			// Perform local search
			neighborhoodMove();

			// Update incumbent if a better solution was found
			isFeasible = isSolutionFeasible(currentSol);
			if(isFeasible)
				this.lastFeasibleIteration = iterationsCount;
			
			if (incumbentSol.cost > currentSol.cost && isFeasible) {
				consecFailures = 0; // Reset failures
				incumbentSol = new Solution<E>(currentSol);
				if (verbose)
					System.out.println("(Iter. " + iterationsCount + ") BestSol = " + incumbentSol);
			}
			
			// Register consecutive failure otherwise
			else {
				consecFailures++;
			}
			
			if (intensificator != null) {

				// Only update recency array if intensification is enabled
				updateRecency();

				if (intensificator.getRunning()) {

					// Stop intensification if there are no iterations left
					if (intensificator.getRemainingIt() == 0) {
						endIntensification();
						if (verbose)
							System.out.println("Intensification ended at: " + iterationsCount);
					}

					// Decrement remaining intensification iterations otherwise
					else {
						intensificator.setRemainingIt(
							intensificator.getRemainingIt() - 1
						);
					} // end if intensificator iteration
				}	  // end if intensificator running
			}		  // end if intensificator not null
		}			  // end for

		return incumbentSol;
	}

	/**
	 * Initialize the recency array with zeros.
	 */
	public void makeRecency() {
		recency = new Integer[ObjFunction.getDomainSize()];
		Arrays.fill(recency, 0);
	}

	/**
	 * A standard stopping criteria for the constructive heuristic is to repeat
	 * until the incumbent solution improves by inserting a new candidate
	 * element.
	 * 
	 * @return true if the criteria is met.
	 */
	public Boolean constructiveStopCriteria() {
		return (currentCost > currentSol.cost) ? false : true;
	}
	
	/**
	 * Check if solution is feasible.
	 * @param sol Current solution to check.
	 * @return true if feasible, false otherwise.
	 */
	public boolean isSolutionFeasible(Solution<E> sol) {
		return true;
	}

}
