package it.uniroma2.sel.ebpmn.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniroma2.sel.ebpmn.engine.TimeUnit;

import java.io.File;
import java.io.IOException;

/**
 * Simulation configuration loaded from a JSON file.
 *
 * <p>Holds all parameters required to initialise the
 * {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine}, including time settings,
 * HLA federation parameters, workload parameters, and the random seed.
 *
 * <p>Use the static factory method {@link #load(String)} to deserialise a JSON
 * configuration file via Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper}.
 *
 * <p><b>JSON field summary:</b>
 * <ul>
 *   <li>{@code outputFolder}     — output folder where logs are saved
 *   <li>{@code timeUnit}         — simulation time unit ({@link it.uniroma2.sel.ebpmn.engine.TimeUnit})</li>
 *   <li>{@code precision}        — number of decimal places for time output</li>
 *   <li>{@code simulationType}   — {@code LOCAL} or {@code DISTRIBUTED}</li>
 *   <li>{@code federationName}   — HLA federation execution name</li>
 *   <li>{@code federateName}     — name of this federate</li>
 *   <li>{@code controller}       — {@code true} if this federate coordinates the ready-to-run sync point</li>
 *   <li>{@code numbersOfFederates} — total federates (used by the controller)</li>
 *   <li>{@code rtiHostName}      — RTI host address</li>
 *   <li>{@code rtiPortNumber}    — RTI port</li>
 *   <li>{@code simulationLength} — stop time in simulation time units</li>
 *   <li>{@code numberOfTokens}   — initial tokens per participant</li>
 *   <li>{@code lookahead}        — HLA lookahead value</li>
 *   <li>{@code seed}             — RNG seed ({@code -1} = random)</li>
 * </ul>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.engine.ExecutionEngine
 * @see it.uniroma2.sel.ebpmn.engine.TimeUnit
 */
public class SimulationConfig {
    /** Distinguishes between a purely local run and an HLA distributed federation. */
    public enum SimulationType {LOCAL, DISTRIBUTED}

    private String outputFolder;
    private TimeUnit timeUnit;
    private int precision;
    private SimulationType simulationType;
    private String federationName;
    private String federateName;
    private boolean controller;
    private int numbersOfFederates;
    private String rtiHostName;
    private int rtiPortNumber;
    private double simulationLength;
    private int numberOfTokens;
    private double lookahead;
    private long seed=-1;

    public SimulationConfig() {
    }

    public static SimulationConfig load(String path) throws IOException {
        return new ObjectMapper().readValue(new File(path), SimulationConfig.class);
    }

    public String getOutputFolder(){return outputFolder; }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public int getPrecision(){return precision;}

    public SimulationType getSimulationType() {
        return simulationType;
    }

    public String getRtiHostName() {
        return rtiHostName;
    }

    public int getRtiPortNumber() {
        return rtiPortNumber;
    }

    public String getFederationName() {
        return federationName;
    }

    public String getFederateName() {
        return federateName;
    }

    public int getNumbersOfFederates() {
        return numbersOfFederates;
    }

    public double getSimulationLength() {
        return simulationLength;
    }

    public int getNumberOfTokens() { return numberOfTokens;}

    public double getLookahead() {
        return lookahead;
    }

    public boolean isController() {return controller;}

    public long getSeed() {return seed;}

    @Override
    public String toString() {
        return String.format("SimulationConfig " +
                        "OutputFolder=%s, TimeUnit=%s, Precision=%d, Type=%s, RTI=%s:%d, federationName=%s, " +
                        "federateName=%s, numbersOfFederate=%d, simulationLength=%f, numberOfTokens=%d, lookahead=%f, " +
                        "iscontroller=%b, seed=%d, ",
                outputFolder, timeUnit.toString(), precision, simulationType, rtiHostName, rtiPortNumber, federationName,
                federateName, numbersOfFederates, simulationLength, numberOfTokens, lookahead,
                controller, seed );
    }
}
