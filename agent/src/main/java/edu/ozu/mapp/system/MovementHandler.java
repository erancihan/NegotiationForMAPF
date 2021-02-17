package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.utils.JSONAgent;
import edu.ozu.mapp.utils.PseudoLock;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class MovementHandler
{
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MovementHandler.class);

    private static MovementHandler instance;

    private ConcurrentHashMap<String, AgentHandler>                     move_queue;
    private ConcurrentHashMap<String, DATA_REQUEST_PAYLOAD_WORLD_MOVE>  payloads;

    private WorldOverseer world;

    private final PseudoLock process_queue_lock;
    private CountDownLatch process_queue_unlock_latch;

    private MovementHandler()
    {
        move_queue  = new ConcurrentHashMap<>();
        payloads    = new ConcurrentHashMap<>();

        process_queue_lock = new PseudoLock();
    }

    public static MovementHandler getInstance()
    {
        if (instance == null)
        {
            //synchronized block to remove overhead
            synchronized (MovementHandler.class)
            {
                if (instance == null)
                {
                    instance = new MovementHandler();
                }
            }
        }

        return instance;
    }

    public MovementHandler Flush()
    {
        instance = new MovementHandler();

        return instance;
    }

    public void SetWorldReference(WorldOverseer world)
    {
        this.world = world;
    }

    public void put(String agentName, AgentHandler agent, DATA_REQUEST_PAYLOAD_WORLD_MOVE payload)
    {
        move_queue.put(agentName, agent);
        payloads.put(agentName, payload);
    }

    public int size()
    {
        return move_queue.size();
    }

    public synchronized void ProcessQueue(Runnable runnable)
    {
        if (!process_queue_lock.tryLock()) return;

        process_queue_unlock_latch = new CountDownLatch(move_queue.size());

        try
        {
            process_queue();

            process_queue_unlock_latch.await();

            runnable.run();
            process_queue_lock.unlock();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private synchronized void process_queue()
    {
        System.out.println("MovementHandler | processing queue of size : " + move_queue.size());

        for (String agent_id : move_queue.keySet())
        {
            DATA_REQUEST_PAYLOAD_WORLD_MOVE payload = payloads.get(agent_id);

            if (world.point_to_agent.get(payload.CURRENT_LOCATION.key).equals(agent_id)) {
                // location has not been cleared
                world.point_to_agent.remove(payload.CURRENT_LOCATION.key);
            } else {
                logger.error(
                        "THIS SHOULD HAVE NEVER HAPPENED. " +
                        world.point_to_agent.get(payload.CURRENT_LOCATION.key) +
                        " AND " +
                        agent_id +
                        " HAVE THE SAME LOCATION KEYS"
                );
                System.exit(500);
            }
        }

        Iterator<String> iterator = move_queue.keySet().iterator();
        while (iterator.hasNext())
        {
            String agent_name = iterator.next();
            DATA_REQUEST_PAYLOAD_WORLD_MOVE payload = payloads.get(agent_name);

            if (world.point_to_agent.get(payload.NEXT_LOCATION.key) != null)
            {   // ensure location is OPEN
                logger.error(
                    payload.NEXT_LOCATION.key + " WAS NOT OPEN FOR " + agent_name + ". OCCUPIED BY " +
                    world.point_to_agent.get(payload.NEXT_LOCATION.key)
                );
                System.exit(500);
            }
            world.point_to_agent.put(payload.NEXT_LOCATION.key, agent_name);    // CLOSE
            world.agent_to_point.put(agent_name, payload.NEXT_LOCATION.key);    // UPDATE

            world.broadcasts.put(agent_name, payload.BROADCAST);

            System.out.println(
                agent_name + " | " +
                payload.CURRENT_LOCATION.key + " -> " + payload.NEXT_LOCATION.key
            );

            AgentHandler agent_ref = move_queue.get(agent_name);

            CompletableFuture.runAsync(() -> {
                JSONAgent response = new JSONAgent();
                response.agent_id = payload.AGENT_NAME;
                response.agent_x = String.valueOf(payload.NEXT_LOCATION.x);
                response.agent_y = String.valueOf(payload.NEXT_LOCATION.y);

                agent_ref.DoMove(response);
                process_queue_unlock_latch.countDown();
            });

            iterator.remove();
        }
    }
}
