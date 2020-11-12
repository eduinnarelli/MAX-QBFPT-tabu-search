package metaheuristics.tabusearch;

/**
 * Simple class where the intensification parameters are defined.
 */
public class Intensificator {

    /**
     * Number of consecutive failures that the metaheuristic toleres before it 
     * triggers the intensification method.
     */
    private Integer tolerance;

    /**
     * Number of iterations that the intensification lasts.
     */
    private Integer iterations;

    /**
     * How many iterations it remains to stop the method.
     */
    private Integer remainingIt;

    /**
     * Identifies if the intensification method is running or has stopped.
     */
    private Boolean running;

    /**
     * Constructor for the Intensificator class.
     * 
     * @param tolerance
     *      Number of consecutive failures that the tabu search toleres.
     * @param iterations
     *      Intensification number of iterations.
     */
    public Intensificator(Integer tolerance, Integer iterations) {
        this.tolerance = tolerance;
        this.iterations = iterations;
        this.running = false;
    }

    /**
     * 'tolerance' getter.
     * 
     * @return {@link #tolerance}.
     */
    public Integer getTolerance() { return tolerance; }

    /**
     * 'iterations' getter.
     * 
     * @return {@link #iterations}.
     */
    public Integer getIterations() { return iterations; }

    /**
     * 'remainingIt' getter.
     * 
     * @return {@link #remainingIt}.
     */
    public Integer getRemainingIt() { return remainingIt; }

    /**
     * 'remainingIt' setter.
     * 
     * @param newIt
     *      The new number of remaining iterations.
     */
    public void setRemainingIt(Integer newIt) { remainingIt = newIt; }

    /**
     * 'running' getter.
     * 
     * @return {@link #running}.
     */
    public Boolean getRunning() { return running; }

    /**
     * 'running' setter.
     * 
     * @param running
     *      Either the method started running or stopped.
     */
    public void setRunning(Boolean running) { this.running = running; }

}
