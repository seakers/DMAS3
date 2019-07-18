package CCBBA;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import madkit.kernel.Scheduler;
import madkit.simulation.activator.GenericBehaviorActivator;

public class myScheduler extends Scheduler {

    protected GenericBehaviorActivator<AbstractAgent> agents;
    protected GenericBehaviorActivator<AbstractAgent> viewers;

    @Override
    protected void activate() {

        // 1 : request my role
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.SCH_ROLE);

        // 3 : initialize the activators
        // by default, they are activated once each in the order they have been added
        //agents = new GenericBehaviorActivator<>(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK, "doSim");

        agents = new GenericBehaviorActivator<>(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK, "phaseOne");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK, "phaseTwo");
        addActivator(agents);
        setDelay(20);

//      4 : let us start the simulation automatically
        setSimulationState(SimulationState.RUNNING);
    }
}