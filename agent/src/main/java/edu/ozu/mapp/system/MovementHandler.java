package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.client.AgentHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MovementHandler
{
    private static MovementHandler instance;

    private ConcurrentHashMap<String, AgentHandler> move_queue;

    private WorldManager world;

    private MovementHandler()
    {
        move_queue  = new ConcurrentHashMap<>();
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

    public void SetWorldReference(WorldManager world)
    {
        this.world = world;
    }

    public void put(String agentName, AgentHandler agent)
    {
        move_queue.put(agentName, agent);
    }

    public int size()
    {
        return move_queue.size();
    }

    private static Lock process_queue_lock = new ReentrantLock();
    public synchronized void ProcessQueue()
    {
        if (process_queue_lock.tryLock())
        {
            try {
                CompletableFuture
                        .runAsync(this::process_queue)
                        .thenRun(() -> process_queue_lock.unlock());
            } catch (Exception exception) {
                process_queue_lock.unlock();
            }
        }
    }

    private synchronized void process_queue()
    {
        // TODO process queue
        for (String agent_name : move_queue.keySet())
        {
            AgentHandler agent = move_queue.get(agent_name);
            agent.DoMove(null);
        }
    }
}
