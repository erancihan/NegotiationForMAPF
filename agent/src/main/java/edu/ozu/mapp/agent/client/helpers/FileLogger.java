package edu.ozu.mapp.agent.client.helpers;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.utils.Action;
import edu.ozu.mapp.utils.PseudoLock;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileLogger
{
    private static final String NEGOTIATION = "NEGOTIATION";
    private static final String INFO        = "INFO";

    private static FileLogger instance;
    private final LinkedBlockingDeque<FileData> write_queue;
    private final File mapp_folder;

    private final PseudoLock file_logger_process_queue_lock;

    private FileLogger()
    {
        // define folder path
        mapp_folder = new File(java.nio.file.Paths.get(new JFileChooser().getFileSystemView().getDefaultDirectory().toString(), "MAPP").toString());
        if (!mapp_folder.exists()) //noinspection ResultOfMethodCallIgnored
            mapp_folder.mkdirs();

        write_queue = new LinkedBlockingDeque<>();
        file_logger_process_queue_lock = new PseudoLock();
    }

    public static FileLogger getInstance()
    {
        if (instance == null)
        {
            synchronized (FileLogger.class)
            {
                if (instance == null)
                {
                    instance = new FileLogger();
                }
            }
        }

        return instance;
    }

    private void process_queue()
    {
        if (!file_logger_process_queue_lock.tryLock()) return;

        synchronized (write_queue)
        {
            Iterator<FileData> iterator = write_queue.iterator();
            while (iterator.hasNext())
            {
                try {
                    FileData data = iterator.next();
                    File file = new File(java.nio.file.Paths.get(String.valueOf(mapp_folder), data.file).toString());

                    // create file if not exists
                    //noinspection ResultOfMethodCallIgnored
                    file.getParentFile().mkdirs();
                    if (!file.exists()) //noinspection ResultOfMethodCallIgnored
                        file.createNewFile();

                    new FileWriter(file, true).append(data.payload).close();
                    iterator.remove();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            file_logger_process_queue_lock.unlock();
        }
    }

    /**  ========================================== WORLD FILE LOGS ========================================== */

    public void WorldLogAgentJoin(Agent agent)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/WORLD.log", agent.WORLD_ID);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\",\"agent_id\":\"%s\",\"agent_name\":\"%s\",\"start\":\"%s\",\"dest\":\"%s\",\"path\":\"%s\",\"path_len\":\"%s\",\"token_c\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            "AGENT_JOIN",
            agent.AGENT_ID,                     // agent identifier
            agent.AGENT_NAME,                   // name
            agent.START.key,                    // source
            agent.DEST.key,                     // destination
            agent.path.toString(),              // initial planned path
            agent.path.size(),                  // initial planned path length
            agent.current_tokens,
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void WorldLogAgentLeave(Agent agent)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/WORLD.log", agent.WORLD_ID);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\",\"agent_id\":\"%s\",\"path\":\"%s\",\"path_len\":\"%s\",\"negoC\":\"%s\",\"winC\":\"%s\",\"loseC\":\"%s\",\"token_c\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            "LEAVE",                    // agent's interaction with the world
            agent.AGENT_ID,             // agent identifier
            agent.path.toString(),      // @LEAVE this is the path taken
            agent.path.size(),          // @LEAVE this is the taken path length
            (agent.winC + agent.loseC), // sum w&l
            agent.winC,                 // sum win
            agent.loseC,                // sum lose
            agent.current_tokens,
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void WorldLogCreate(String world_id, int width, int height)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/WORLD.log", world_id);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\", \"world_id\":\"%s\", \"dimensions\":\"%sx%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            "CREATE",
            world_id,
            width, height,
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void WorldLogJoin(String world_id, int active_agent_c, int TIME)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/WORLD.log", world_id);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\", \"player_count\":\"%s\", \"time_tick\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            "JOIN",
            active_agent_c,
            TIME,
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void WorldLogBroadcast(String world_id)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/WORLD.log", world_id);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            "BROADCAST",
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void WorldLogNegotiate(String world_id, String state)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/WORLD.log", world_id);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\", \"state\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            "NEGOTIATE",
            state,
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void WorldLogMove(String world_id)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/WORLD.log", world_id);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            "MOVE",
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void WorldLogDone(String world_id, long time, double duration)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/WORLD.log", world_id);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\", \"duration\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(time)),
            "DONE",
            duration,
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

/**  ========================================== AGENT FILE LOGS ========================================== */

    public void AgentLogPreNegotiation(AgentHandler handler, String session_id)
    {
        Agent agent_ref = handler.GetAgent();

        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/AGENT-%s-%s.log", agent_ref.WORLD_ID, NEGOTIATION, agent_ref.AGENT_ID);
        data.payload  = String.format(
                "%s;{\"name\":\"%s\", \"session_id\":\"%s\", \"path\":\"%s\", \"token\":\"%s\", \"conflict_location\":\"%s\", \"world_time\":\"%s\"}%s",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                "PRE",
                session_id,                         // negotiation session id
                agent_ref.path.toString(),          // agents path before negotiation session
                agent_ref.GetCurrentTokenC(),       // print token count
                agent_ref.GetConflictLocation(session_id),    // conflict location
                agent_ref.time,
                System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void AgentLogNegotiationAction(AgentHandler handler, Action action)
    {
        Agent agent_ref = handler.GetAgent();

        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/AGENT-%s-%s.log", agent_ref.WORLD_ID, NEGOTIATION, agent_ref.AGENT_ID);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\", \"turn\":\"%s\", \"contract\":%s}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            action.type,
            agent_ref.AGENT_ID,
            action.bid.getJSON(),
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void AgentLogPostNegotiation(AgentHandler handler, String session_id, boolean is_win)
    {
        Agent agent_ref = handler.GetAgent();

        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/AGENT-%s-%s.log", agent_ref.WORLD_ID, NEGOTIATION, agent_ref.AGENT_ID);
        data.payload  = String.format(
            "%s;{\"name\":\"%s\", \"session_id\":\"%s\", \"path\":\"%s\", \"token\":\"%s\", \"is_win\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            "POST",
            session_id,
            agent_ref.path.toString(),
            agent_ref.GetCurrentTokenC(),
            is_win,
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }

    public void AgentLogInfo(Agent agent, String step)
    {
        FileData data = new FileData();
        data.file     = String.format("logs/WORLD-%s/AGENT-%s-%s.log", agent.WORLD_ID, INFO, agent.AGENT_ID);
        data.payload  = String.format(
            "%s;{\"token_count\":\"%s\",\"time\":\"%s\",\"agent_pos\":\"%s\",\"step\":\"%s\"}%s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
            agent.GetCurrentTokenC(),
            agent.time,
            agent.POS.key,
            step,
            System.lineSeparator()
        );

        CompletableFuture.runAsync(() -> {
            write_queue.add(data);
            process_queue();
        });
    }
}

class FileData
{
    public String file;
    public String payload;
}
