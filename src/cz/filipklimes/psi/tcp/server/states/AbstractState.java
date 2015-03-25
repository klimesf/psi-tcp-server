package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

/**
 * @author klimesf
 */
abstract public class AbstractState implements State {

    /**
     *
     */
    protected Client context;

    /**
     * @param context
     */
    public AbstractState(Client context) {
        this.context = context;
    }

}
