package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.utils.JSONAgent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MovementHandler
{
    private static MovementHandler instance;

    private ConcurrentHashMap<String, AgentHandler>                     move_queue;
    private ConcurrentHashMap<String, DATA_REQUEST_PAYLOAD_WORLD_MOVE>  payloads;

    private WorldOverseer world;

    private MovementHandler()
    {
        move_queue  = new ConcurrentHashMap<>();
        payloads    = new ConcurrentHashMap<>();
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

    public void Flush()
    {
        instance = new MovementHandler();
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

    private static Lock process_queue_lock = new ReentrantLock();
    public synchronized void ProcessQueue(Runnable runnable)
    {
        if (process_queue_lock.tryLock())
        {
            try {
                CompletableFuture
                    .runAsync(this::process_queue)
                    .exceptionally(ex -> { ex.printStackTrace(); return null; })
                    .whenCompleteAsync((entity, ex) -> {
                        if (ex != null) ex.printStackTrace();

                        runnable.run();
                        process_queue_lock.unlock();
                    })
                ;
            } catch (Exception exception) {
                exception.printStackTrace();
                process_queue_lock.unlock();
            }
        }
    }

    private synchronized void process_queue()
    {
//        System.out.println("processing queue of size " + move_queue.size());
        Iterator<String> iterator = move_queue.keySet().iterator();
        while (iterator.hasNext())
        {
            String agent_name = iterator.next();
            System.out.println(agent_name);

            DATA_REQUEST_PAYLOAD_WORLD_MOVE payload = payloads.get(agent_name);

            // FREE CURRENT LOCATION FIRST
            world.point_to_agent.remove(payload.CURRENT_LOCATION.key);
            // UPDATE
            world.agent_to_point.put(agent_name, payload.NEXT_LOCATION.key);

            // todo handle this operation after all points are freed
            world.point_to_agent.put(payload.NEXT_LOCATION.key, agent_name);
            System.out.println(agent_name + " " + Arrays.toString(payload.BROADCAST));
            world.broadcasts.put(agent_name, payload.BROADCAST);

            AgentHandler agent_ref = move_queue.get(agent_name);

            CompletableFuture.runAsync(() -> {
                JSONAgent response = new JSONAgent();
                response.agent_id = payload.AGENT_NAME;
                response.agent_x = String.valueOf(payload.NEXT_LOCATION.x);
                response.agent_y = String.valueOf(payload.NEXT_LOCATION.y);

                agent_ref.DoMove(response);
            });

            iterator.remove();
        }
    }
}
