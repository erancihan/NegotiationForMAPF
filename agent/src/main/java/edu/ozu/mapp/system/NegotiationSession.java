package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.utils.*;
import org.glassfish.grizzly.utils.ArrayUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NegotiationSession
{
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NegotiationSession.class);

    private BiConsumer<String, Integer> bank_update_hook;
    private Consumer<String> world_log_callback;
    private BiConsumer<String, String> log_hook;

    private ConcurrentHashMap<String, AgentHandler> agent_refs;
    private ConcurrentSkipListSet<String> AGENTS_READY;
    private String[] _agent_names;
    private String[] agent_names;
    private ScheduledExecutorService service;
    private ScheduledFuture<?> task_join_await;
    private ScheduledFuture<?> task_run;

    private final PseudoLock session_loop_agent_invoke_lock;

    private int T = 0;
    private int Round = 0;
    private long start_time;
    private ConcurrentLinkedQueue<String> bid_order_queue;

    private String session_hash;
    private NegotiationState state;
    private String TURN;
    private Contract contract;

    public enum NegotiationState {
        JOIN, RUNNING, DONE;
    }

    public NegotiationSession(String session_hash, String[] agent_names, BiConsumer<String, Integer> bank_update_hook, Consumer<String> world_log_callback, BiConsumer<String, String> log_payload_hook)
    {
        this.agent_refs   = new ConcurrentHashMap<>();
        this._agent_names = agent_names.clone();
        this.agent_names  = agent_names.clone();
        this.service      = Executors.newScheduledThreadPool(2);
        this.AGENTS_READY = new ConcurrentSkipListSet<>();

        this.bank_update_hook   = bank_update_hook;
        this.world_log_callback = world_log_callback;
        this.log_hook           = log_payload_hook;

        this.bid_order_queue = new ConcurrentLinkedQueue<>();

        this.session_hash = session_hash;
        this.state        = NegotiationState.JOIN;

        // todo initialize contract
        shuffle_bid_order();
        this.TURN  = bid_order_queue.poll();

        Map<String, String> session_data = new HashMap<>();
        session_data.put("Ox", "");
        session_data.put("x", "");

        // todo make this more... flexible
        session_data.put("A", agent_names[0]);
        session_data.put("B", agent_names[1]);

        session_data.put("_session_id", session_hash);

        this.contract     = Contract.Create(session_data);
        this.contract.apply(this);

        // initialize LOCKs
        session_loop_agent_invoke_lock = new PseudoLock();

        // LOG
        log_hook.accept(session_hash, String.format("%-23s %-7s %s", new java.sql.Timestamp(System.currentTimeMillis()), "INIT", Arrays.toString(agent_names)));
    }

    public NegotiationState GetState()
    {
        return state;
    }

    public synchronized void RegisterAgentREF(AgentHandler agent)
    {
        if (!this.state.equals(NegotiationState.JOIN)) return;

        String agent_name = agent.getAgentName();

        if (!this.agent_refs.containsKey(agent_name))
        {
            Assert.isTrue(Arrays.asList(agent_names).contains(agent_name), "AGENT " + agent_name+ " DOES NOT BELONG HERE");

            this.agent_refs.put(agent.getAgentName(), agent);
            this.log_hook.accept(session_hash, String.format("%-23s %s JOIN {BROADCAST: %s }", new java.sql.Timestamp(System.currentTimeMillis()), agent_name, Arrays.toString(agent.GetBroadcast())));
        }

        if (this.agent_refs.size() == this.agent_names.length)
        {
            conclude_join_process();
        }
    }

    private void conclude_join_process()
    {
        world_log_callback.accept(String.format("Negotiation Session %s STARTING | %s", session_hash.substring(0, 7), Arrays.toString(_agent_names)));
        log_hook.accept(session_hash, String.format("%-23s %-7s", new java.sql.Timestamp(System.currentTimeMillis()), "START"));

        join_task();

        // can be schedule
        task_join_await = service.scheduleAtFixedRate(this::await_init, 0, 250, TimeUnit.MILLISECONDS);
    }

    private void join_task()
    {
        CompletableFuture.runAsync(() -> {
            for (String agent_name : agent_names)
            {
                CompletableFuture.runAsync(() -> {
                    try {
                        State state = new State();
                        state.agents = agent_names.clone();
                        state.contract = contract.clone();

                        agent_refs.get(agent_name).PreNegotiation(session_hash, state);

                        AGENTS_READY.add(agent_name);
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                })
                .exceptionally(ex -> { ex.printStackTrace(); return null; })
                ;
            }
        })
        .exceptionally(ex -> { ex.printStackTrace(); return null; })
        ;
    }

    private void await_init()
    {
        try {
            if (AGENTS_READY.size() == agent_names.length)
            {   // ALL AGENTS ARE READY
                run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void run()
    {
        // state should STILL be JOIN
        if (!state.equals(NegotiationState.JOIN)) return;

        task_join_await.cancel(false);

        state = NegotiationState.RUNNING;
        start_time = System.currentTimeMillis();
        Assert.notNull(TURN, "TURN cannot be null! " + Arrays.toString(agent_names));

        task_run = service.scheduleAtFixedRate(this::session_loop_container, 0, 100, TimeUnit.MILLISECONDS);
    }

    private synchronized void shuffle_bid_order()
    {
        List<String> bid_order;
        do {
            bid_order = Arrays.asList(agent_names);
            Collections.shuffle(bid_order);
        } while (bid_order.get(0).equals(TURN));
        // DON'T ALLOW SAME AGENT TO BID OVER AND OVER AND OVER AND OVER AGAIN

        this.bid_order_queue.addAll(bid_order);
    }

    private void session_loop_container()
    {
        try {
            session_loop();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void session_loop() throws CloneNotSupportedException
    {
        if (!session_loop_agent_invoke_lock.tryLock()) return;

        switch (state)
        {
            case RUNNING:
                session_loop_state_case_running();
                break;
            case DONE:
                // TELL ALL AGENTS THAT NEGOTIATION IS DONE
                session_loop_state_case_done();
                break;
            default:
                System.err.printf("UNHANDLED STATE: %s %s", state, System.lineSeparator());
                System.exit(1);
        }

        T = T + 1;
    }

    private void session_loop_state_case_running()
    {
//        log_hook.accept(session_hash, String.format("%-23s %-7s %s", new java.sql.Timestamp(System.currentTimeMillis()), state, contract.print()));
        try {
            for (String agent_name : agent_names)
            {
                CompletableFuture
                    .runAsync(() -> send_current_state_to_agent(agent_name))
                    .exceptionally(ex -> { ex.printStackTrace(); return null; })
                ;
            }

            CompletableFuture
                .supplyAsync(this::process_turn_make_action)
                .whenCompleteAsync((entity, ex) -> {
                    if (ex != null) ex.printStackTrace();

                    session_loop_agent_invoke_lock.unlock();
                }, service)
            ;
        } catch (Exception exception) {
            exception.printStackTrace();
            session_loop_agent_invoke_lock.unlock();
        }
    }

    private void session_loop_state_case_done() throws CloneNotSupportedException
    {
        Contract contract = this.contract.clone();

        log_hook.accept(session_hash, String.format("%-23s %-7s %s", new java.sql.Timestamp(System.currentTimeMillis()), state, contract.print()));
        for (String agent_name : agent_names)
        {
            CompletableFuture
                .runAsync(() -> {
                    agent_refs.get(agent_name).AcceptLastBids(contract);
                    agent_refs.get(agent_name).PostNegotiation(contract);
                })
                .exceptionally(ex -> { ex.printStackTrace(); return null; })
            ;
        }

        // clean up
        task_run.cancel(false);
    }

    private void send_current_state_to_agent(String agent_name)
    {
        try {
            State state = new State();
            state.agents = agent_names.clone();
            state.contract = contract.clone();

            agent_refs.get(agent_name).OnReceiveState(state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @org.jetbrains.annotations.NotNull
    private String process_turn_make_action() {
        AgentHandler agent = agent_refs.get(TURN);
/*
        if (
                Round >= Globals.NEGOTIATION_DEADLINE_ROUND ||
                (System.currentTimeMillis() - start_time) >= Globals.NEGOTIATION_DEADLINE_MS
        )
        {
            // todo tell agents that negotiation is over
            return "";
        }
 */

        Action action = null;
        try {
            action = agent.OnMakeAction(contract.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Assert.notNull(action, "Action cannot be null!");
        log_hook.accept(session_hash, String.format("%-23s %-7s %s", new java.sql.Timestamp(System.currentTimeMillis()), action.type, action.bid.print()));
        assert action.validate();

        if (action.type.equals(ActionType.OFFER))
        {   // Bidding agent made an offer
            // TODO register bid
            try {
                this.contract = action.bid.clone();
            } catch (Exception e) {
                e.printStackTrace();
            }

            action.bid.apply(this);

            // Update TURN
            if (bid_order_queue.peek() == null) {   // QUEUE is empty!!
                // UPDATE ROUND
                Round = Round + 1;
                shuffle_bid_order();
            }
            TURN = bid_order_queue.poll();

            return "";
        }
        if (action.type.equals(ActionType.ACCEPT))
        {   // Handle agent accept
            Assert.isTrue(agent_refs.size() == 2, "it is not bilateral");
            AgentHandler opponent = null;
            for (String agent_key : agent_refs.keySet()) {
                // opponent is the agent whose turn it is not at the moment
                if (agent_key.equals(TURN)) continue;

                opponent = agent_refs.get(agent_key);
            }
            Assert.notNull(opponent, "opponent cannot be empty");

            if (action.bid.Ox.isEmpty())
            {   // what you mean it is empty!?
                // TODO uh... make it more... flexible

                action.bid.Ox = Utils.toString(opponent.GetBroadcast(), ",");
                action.bid.apply(this);
            }

            logger.debug(
                agent.GetAgentID() + " ACCEPTED " + contract.print()
            );

            // BEGIN: TOKEN EXCHANGE LOGIC
            // if action is accept, `agent` is accepting
            int T_accepting = Integer.parseInt(contract.getTokenCountOf(agent.GetAgent()));
            int T_opponent  = Integer.parseInt(contract.getTokenCountOf(opponent.GetAgent()));

            if (T_accepting < T_opponent)
            {   // opponent has higher tokens in contract
                // accepting (agent) will receive tokens from opponent
                int diff = T_opponent - T_accepting;

                int T_opponent_next = opponent.UpdateTokenCountBy(-1 * diff);
                int T_accepting_next = agent.UpdateTokenCountBy(diff);

                logger.debug(
                    "ACCEPTING " + agent.GetAgentID() + " NEXT : " + T_accepting_next + " | OPPONENT " + opponent.GetAgentID() + " NEXT : " + T_opponent_next
                );

                bank_update_hook.accept(opponent.GetAgentID(), T_opponent_next);
                bank_update_hook.accept(agent.GetAgentID(), T_accepting_next);
            }
            // END

            this.state = NegotiationState.DONE;

            return "";
        }

        return "";
    }

    // TODO update contract functions

    public Contract GetAgentContract(String agent_name)
    {
        // TODO verify agent name
        try {
            return contract.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public void SetContract(Contract contract)
    {
        this.contract = contract;
    }

    public String[] GetAgentNames()
    {
        return _agent_names;
    }

    public String[] GetActiveAgentNames()
    {
        return agent_names == null ? new String[0] : agent_names;
    }

    public synchronized void RegisterAgentLeaving(String agent_name)
    {
        agent_names = ArrayUtils.remove(agent_names, agent_name);
    }

    /**
     * Invoke this when you are done with this instance
     * */
    public void destroy()
    {
        if (task_join_await != null)
        {
            System.out.println("canceling task_join_await");
            task_join_await.cancel(false);
        }
        if (task_run != null)
        {
            task_run.cancel(false);
            System.out.println("canceling task_run");
        }

        service.shutdownNow();

        System.out.println("unlocking SESSION LOOP LOCK");
        session_loop_agent_invoke_lock.unlock();
    }

    public void invalidate() {
        if (task_join_await != null) {
            task_join_await.cancel(false);
        }
        if (task_run != null) {
            task_run.cancel(false);
        }
        if (service != null) {
            service.shutdownNow();
        }
    }

    @Override
    public String toString() {
        return "{" + Arrays.toString(_agent_names) + "}";
    }
}
