package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

abstract public class AbstractState implements State {

    /**
     * The client context.
     */
    protected Client context;

    /**
     * @param context
     */
    public AbstractState(Client context) {
        this.context = context;
    }
}