package mapp.agent;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.system.Broadcast;
import edu.ozu.mapp.system.SystemExit;
import edu.ozu.mapp.system.fov.FoV;
import edu.ozu.mapp.utils.*;
import edu.ozu.mapp.utils.path.Path;

import java.util.*;

@MAPPAgent
public class PathAwareAgentMC0 extends Agent
{
    // Path Aware Agent Memory Constraint - FULL
    private List<Bid> bid_space;
    private Iterator<Bid> bid_space_iterator;

    public PathAwareAgentMC0(String agentName, String agentID, Point start, Point dest, int inital_tokens)
    {
        super(agentName, agentID, start, dest, inital_tokens);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public ArrayList<Constraint> prepareConstraints(ArrayList<Constraint> constraints)
    {
        // I don't care about the input
        constraints.clear();

        FoV fov = GetFieldOfView();
        for (Broadcast broadcast: fov.broadcasts) {
            // skip if own ID
            if (broadcast.agent_name.equals(AGENT_ID)) {
                continue;
            }

            ArrayList<Constraint> cs = memory.get(broadcast.agent_name);
            if (cs == null) {
                // skip if no memory of item
                continue;
            }

            // check if broadcast starts with the promised locations
            boolean should_constraint = true;
            for (int i = 0; i < broadcast.locations.size(); i++) {
                if (i >= cs.size()) {
                    // memory is shorter than broadcast
                    continue;   // OOB
                }

                Constraint _c_a = cs.get(i);
                Constraint _c_b = broadcast.locations.get(i);

                if (_c_b.equals(_c_a)) {
                    continue;
                }

                // agent broadcast does not match agent's promise
                //   Ox constraint memoization is invalid
                should_constraint = false;
                // forget the locations as well
                memory.remove(broadcast.agent_name);
            }

            if (!should_constraint) {
                continue;
            }

            // I promised this guy
            constraints.addAll(cs);
        }
        for (Point point : fov.obstacles) {
            constraints.add(new Constraint(point));
        }

        // Basically, remember whom I promised.
        // Then, take what ever that guy broadcasts as a constraint.
        // Because that is the road that they'll take.

        return super.prepareConstraints(constraints);
    }


    // after move is done
    @SuppressWarnings("DuplicatedCode")
    @Override
    public void OnMove(JSONAgent response) {
//        Iterator<Map.Entry<String, ArrayList<Constraint>>> iterator = memory.entrySet().iterator();
//        while (iterator.hasNext()) {
//            ArrayList<Constraint> constraints = iterator.next().getValue();
//
//            constraints.removeIf(constraint -> constraint.at_t < this.time);
//
//            if (constraints.size() == 0) {
//                iterator.remove();
//            }
//        }
        memory.clear();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void PostNegotiation(Contract contract) {
        // get contract and register promise

        if (contract.x.equals(AGENT_ID)) {
            // I won, nothing to do here
            return;
        }

        // remember whom we promised, what we promised
        // register entire Ox as promised as well

        Point[] Ox = contract.GetOx();
        ArrayList<Constraint> constraints = new ArrayList<>();
        for (int i = 0; i < Ox.length; i++) {
            constraints.add(new Constraint(contract.x, Ox[i], this.time + i));
        }

        memory.put(contract.x, constraints);
    }
    private HashMap<String, ArrayList<Constraint>> memory = new HashMap<>();


    @Override
    public void PreNegotiation(State state)
    {
        logger.debug("pre-negotiation :: " + AGENT_ID + "::" + Arrays.toString(state.agents));

        try {
            bid_space = GetCurrentBidSpace(state);
            bid_space_iterator = bid_space.iterator();
        } catch (NoSuchElementException ex) {
            ex.printStackTrace();
            SystemExit.exit(SystemExit.Status.ERROR_SESSION_TERMINATE);
        }
    }

    @Override
    public Action onMakeAction(Contract contract)
    {
        double bid_token_count = contract.GetTokenCountOf(this);

        double token_rate = (this.current_tokens - bid_token_count) / this.initial_tokens;
        double path_rate = (double) GetMyRemainingPathLength() / (double) this.initial_path.size();

        double rate = token_rate / path_rate;

        if (rate < 1)
        {   // CONCEDE
            return run_concede(contract);
        }
        else
        {   // GREED
            return run_greedy(contract);
        }
    }

    public Action run_concede(Contract contract) {
        if (contract == null || contract.Ox.isEmpty()) {   // i am the first bidder
            Action action;
            do {
                if (!bid_space_iterator.hasNext()) {
                    file_logger.LogBid(this.AGENT_ID, this.WORLD_ID, contract == null ? "-" : contract.sess_id, new Path());
                }

                action = new Action(this, ActionType.OFFER, bid_space_iterator.next());
            } while (!action.validate());

            return action;
        }

        // there is a bid

        // can i calculate a path, with bid as constraint
        List<String> possible_path = calculatePath(POS, DEST, contract.Ox);
        if (possible_path == null) {   // i cannot accept this bid!
            Action action;
            do {
                if (!bid_space_iterator.hasNext()) {
                    file_logger.LogBid(this.AGENT_ID, this.WORLD_ID, contract.sess_id, new Path());
                }

                action = new Action(this, ActionType.OFFER, bid_space_iterator.next());
            } while (!action.validate());

            return action;
        }

        int opponent_offered_tokens = contract.GetOpponentTokenProposal(this);
        if (opponent_offered_tokens > 0) {
            return new Action(this, ActionType.ACCEPT);
        }

        return new Action(this, ActionType.ACCEPT);
    }

    public Action run_greedy(Contract contract)
    {
        // insist on current broadcast
        Action insisting_action = new Action(this, ActionType.OFFER, GetOwnBroadcastPath());
        if (insisting_action.validate()) return insisting_action;

        if (bid_space_iterator != null && bid_space_iterator.hasNext())
        {
            Action action = new Action(this, ActionType.OFFER, bid_space_iterator.next());
            if (action.validate()) return action;
        }

        return new Action(this, ActionType.ACCEPT);
    }

}
